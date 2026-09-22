package com.example.plugin.sistema.services;

import com.example.plugin.sistema.SistemaPlugin;
import com.example.plugin.sistema.models.PlayerClass;
import com.example.plugin.sistema.models.PlayerProfile;
import com.example.plugin.sistema.models.StatDefinition;
import com.example.plugin.sistema.utils.AttributeRegistryHelper;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.Operation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motore delle statistiche: calcola e applica gli AttributeModifier ai giocatori.
 * 
 * CRITICO per Spigot 1.21.5:
 * - Usa NamespacedKey per identificare univocamente i modifier
 * - Usa il costruttore moderno di AttributeModifier con EquipmentSlotGroup
 * - Rimuove sempre il modifier esistente prima di aggiungerne uno nuovo
 */
public class StatEngine {

    private final SistemaPlugin plugin;
    
    // Cache dei profili statistici calcolati per giocatore (solo dati, no riferimenti a Player)
    private final Map<UUID, StatProfile> statProfileCache;

    public StatEngine(SistemaPlugin plugin) {
        this.plugin = plugin;
        this.statProfileCache = new ConcurrentHashMap<>();
    }

    /**
     * Ricalcola tutti i modifier per un giocatore.
     * Da chiamare su: join, cambio stat, level-up, cambio classe.
     */
    public void recomputeAllModifiers(Player player) {
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) return;

        // Rimuovi tutti i modifier gestiti dal sistema
        removeAllSystemModifiers(player);

        // Calcola nuovi valori
        StatProfile statProfile = calculateStatProfile(profile);
        statProfileCache.put(player.getUniqueId(), statProfile);

        // Applica nuovi modifier
        applyModifiers(player, statProfile);
        
        // Aggiorna salute corrente se MaxHealth è cambiato
        updateCurrentHealth(player, statProfile);
    }

    /**
     * Calcola il profilo statistico moltiplicando i valori base per i bonus di classe.
     */
    private StatProfile calculateStatProfile(PlayerProfile profile) {
        StatProfile statProfile = new StatProfile(plugin);
        PlayerClass pClass = profile.getChosenClass();

        for (StatDefinition stat : StatDefinition.values()) {
            int baseValue = profile.getStatValue(stat);
            
            // Ottieni moltiplicatore di classe (default 1.0 se nessuna classe o nessun bonus)
            double classMultiplier = 1.0;
            if (pClass != null) {
                classMultiplier = pClass.getStatMultiplier(stat);
            }

            double effectiveValue = baseValue * classMultiplier;
            statProfile.setEffectiveValue(stat, effectiveValue);
        }

        return statProfile;
    }

    /**
     * Applica tutti i modifier al giocatore.
     */
    private void applyModifiers(Player player, StatProfile statProfile) {
        // FORZA -> ATTACK_DAMAGE + ATTACK_KNOCKBACK
        applyModifier(player, AttributeRegistryHelper.ATTACK_DAMAGE, 
                "stat_forza_attack_damage", 
                statProfile.getEffectiveValue(StatDefinition.FORZA) * 
                    plugin.getConfig().getDouble("system.stats.strength.attack-damage-per-point", 0.5),
                Operation.ADD_NUMBER);

        applyModifier(player, AttributeRegistryHelper.ATTACK_KNOCKBACK,
                "stat_forza_knockback",
                statProfile.getEffectiveValue(StatDefinition.FORZA) *
                    plugin.getConfig().getDouble("system.stats.strength.knockback-per-point", 0.1),
                Operation.ADD_NUMBER);

        // AGILITÀ -> MOVEMENT_SPEED + SAFE_FALL_DISTANCE + ATTACK_SPEED
        double movementSpeedBonus = Math.min(
                statProfile.getEffectiveValue(StatDefinition.AGILITA) * 
                    plugin.getConfig().getDouble("system.stats.agility.movement-speed-per-point", 0.001),
                plugin.getConfig().getDouble("system.stats.agility.max-movement-speed", 0.25)
        );
        applyModifier(player, AttributeRegistryHelper.MOVEMENT_SPEED,
                "stat_agilita_movement_speed",
                movementSpeedBonus,
                Operation.ADD_NUMBER);

        applyModifier(player, AttributeRegistryHelper.SAFE_FALL_DISTANCE,
                "stat_agilita_safe_fall",
                statProfile.getEffectiveValue(StatDefinition.AGILITA) *
                    plugin.getConfig().getDouble("system.stats.agility.safe-fall-distance-per-point", 0.5),
                Operation.ADD_NUMBER);

        applyModifier(player, AttributeRegistryHelper.ATTACK_SPEED,
                "stat_agilita_attack_speed",
                statProfile.getEffectiveValue(StatDefinition.AGILITA) *
                    plugin.getConfig().getDouble("system.stats.agility.attack-speed-per-point", 0.05),
                Operation.ADD_NUMBER);

        // VITALITÀ -> MAX_HEALTH
        applyModifier(player, AttributeRegistryHelper.MAX_HEALTH,
                "stat_vitalita_max_health",
                statProfile.getEffectiveValue(StatDefinition.VITALITA) *
                    plugin.getConfig().getDouble("system.stats.vitality.max-health-per-point", 2.0),
                Operation.ADD_NUMBER);

        // INTELLIGENZA -> LUCK
        applyModifier(player, AttributeRegistryHelper.LUCK,
                "stat_intelligenza_luck",
                statProfile.getEffectiveValue(StatDefinition.INTELLIGENZA) *
                    plugin.getConfig().getDouble("system.stats.intelligence.luck-per-point", 0.1),
                Operation.ADD_NUMBER);

        // PERCEZIONE -> KNOCKBACK_RESISTANCE
        applyModifier(player, AttributeRegistryHelper.KNOCKBACK_RESISTANCE,
                "stat_percezione_knockback_resist",
                statProfile.getEffectiveValue(StatDefinition.PERCEZIONE) *
                    plugin.getConfig().getDouble("system.stats.perception.knockback-resistance-per-point", 0.01),
                Operation.ADD_NUMBER);
    }

    /**
     * Applica un singolo AttributeModifier, rimuovendo prima quello esistente con la stessa key.
     */
    private void applyModifier(Player player, Attribute attribute, String modifierKeySuffix, 
                               double amount, Operation operation) {
        if (attribute == null) return;
        
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        // Crea la NamespacedKey unica per questo modifier
        String fullKey = plugin.getName().toLowerCase() + ":" + modifierKeySuffix;
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, modifierKeySuffix.toLowerCase().replace("-", "_"));

        // Rimuovi modifier esistente con questa key
        removeModifierByKey(instance, key);

        // Aggiungi nuovo modifier se amount != 0
        if (amount != 0.0) {
            try {
                AttributeModifier modifier = new AttributeModifier(
                        key,
                        amount,
                        operation,
                        EquipmentSlotGroup.ANY
                );
                instance.addModifier(modifier);
            } catch (Exception e) {
                plugin.getLogger().warning("Errore nell'applicare modifier " + fullKey + ": " + e.getMessage());
            }
        }
    }

    /**
     * Rimuove un modifier specifico da un'AttributeInstance per chiave.
     */
    private void removeModifierByKey(AttributeInstance instance, org.bukkit.NamespacedKey key) {
        List<AttributeModifier> toRemove = new ArrayList<>();
        for (AttributeModifier mod : instance.getModifiers()) {
            if (mod.getKey().equals(key)) {
                toRemove.add(mod);
            }
        }
        for (AttributeModifier mod : toRemove) {
            instance.removeModifier(mod);
        }
    }

    /**
     * Rimuove TUTTI i modifier del sistema da un giocatore.
     */
    private void removeAllSystemModifiers(Player player) {
        String pluginPrefix = plugin.getName().toLowerCase() + ":";
        
        Attribute[] attributes = {
                AttributeRegistryHelper.ATTACK_DAMAGE,
                AttributeRegistryHelper.ATTACK_KNOCKBACK,
                AttributeRegistryHelper.MOVEMENT_SPEED,
                AttributeRegistryHelper.SAFE_FALL_DISTANCE,
                AttributeRegistryHelper.ATTACK_SPEED,
                AttributeRegistryHelper.MAX_HEALTH,
                AttributeRegistryHelper.LUCK,
                AttributeRegistryHelper.KNOCKBACK_RESISTANCE
        };

        for (Attribute attr : attributes) {
            if (attr == null) continue;
            AttributeInstance instance = player.getAttribute(attr);
            if (instance == null) continue;

            List<AttributeModifier> toRemove = new ArrayList<>();
            for (AttributeModifier mod : instance.getModifiers()) {
                if (mod.getKey().getKey().startsWith(plugin.getName().toLowerCase().replace(" ", "_"))) {
                    toRemove.add(mod);
                }
            }
            for (AttributeModifier mod : toRemove) {
                instance.removeModifier(mod);
            }
        }
    }

    /**
     * Aggiusta la salute corrente dopo un cambio di MaxHealth.
     */
    private void updateCurrentHealth(Player player, StatProfile statProfile) {
        AttributeInstance maxHealthInst = player.getAttribute(AttributeRegistryHelper.MAX_HEALTH);
        if (maxHealthInst == null) return;

        double maxHealth = maxHealthInst.getValue();
        double currentHealth = player.getHealth();

        if (currentHealth > maxHealth) {
            player.setHealth(maxHealth);
        } else if (currentHealth <= 0) {
            player.setHealth(0.1); // Evita morte accidentale
        }
    }

    /**
     * Rimuove un giocatore dalla cache.
     */
    public void clearCache(UUID uuid) {
        statProfileCache.remove(uuid);
    }

    /**
     * Ottiene il profilo statistico calcolato per un giocatore.
     */
    public StatProfile getStatProfile(UUID uuid) {
        return statProfileCache.get(uuid);
    }

    /**
     * Classe interna che memorizza i valori effettivi delle statistiche (dopo i multiplier di classe).
     */
    public static class StatProfile {
        private final Map<StatDefinition, Double> effectiveValues;
        private final SistemaPlugin plugin;

        public StatProfile(SistemaPlugin plugin) {
            this.plugin = plugin;
            this.effectiveValues = new EnumMap<>(StatDefinition.class);
            for (StatDefinition stat : StatDefinition.values()) {
                effectiveValues.put(stat, 0.0);
            }
        }

        public void setEffectiveValue(StatDefinition stat, double value) {
            effectiveValues.put(stat, value);
        }

        public double getEffectiveValue(StatDefinition stat) {
            return effectiveValues.getOrDefault(stat, 0.0);
        }

        /**
         * Ottieni la chance di critico basata su Percezione.
         */
        public double getCritChance(double baseChance, double perPointChance) {
            return baseChance + (getEffectiveValue(StatDefinition.PERCEZIONE) * perPointChance);
        }

        /**
         * Ottieni il moltiplicatore di danno critico.
         */
        public double getCritDamageMultiplier() {
            return plugin.getConfig().getDouble("system.stats.perception.crit-damage-multiplier", 1.5);
        }
        
        /**
         * Ottieni il bonus di rigenerazione Focus dalla classe.
         */
        public double getFocusRegenBonus() {
            if (plugin.getClassManager() == null) return 0.0;
            // Questo dovrebbe essere preso dal PlayerProfile, ma lo teniamo come helper
            return 0.0; // Implementato in FocusService
        }
    }
}
