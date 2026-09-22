package com.galaxyleveling.service;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.*;
import com.galaxyleveling.util.AttributeRegistryHelper;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatEngine implements Listener {
    private final GalaxyLeveling plugin;
    private final Map<UUID, PlayerProfile> playerProfiles = new ConcurrentHashMap<>();
    private static StatEngine instance;

    public StatEngine(GalaxyLeveling plugin) {
        this.plugin = plugin;
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void recomputeAllModifiers(Player player) {
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) return;

        // Rimuovi tutti i modifier esistenti e applica nuovi
        for (StatDefinition stat : StatDefinition.values()) {
            applyStatModifier(player, profile, stat);
        }
    }

    private void applyStatModifier(Player player, PlayerProfile profile, StatDefinition stat) {
        Attribute attr = AttributeRegistryHelper.getAttributeForStat(stat);
        if (attr == null) return;

        AttributeInstance instance = player.getAttribute(attr);
        if (instance == null) return;

        // Rimuovi modifier esistente
        NamespacedKey key = AttributeRegistryHelper.getModifierKey(plugin, stat, "base");
        instance.removeModifier(key);

        // Calcola valore con moltiplicatore classe
        int statValue = profile.getStatValue(stat);
        double baseValue = stat.getBaseValue() + (statValue * stat.getIncrementPerLevel());
        
        double classMultiplier = 1.0;
        String classId = profile.getChosenClassId();
        if (classId != null) {
            PlayerClass pClass = plugin.getClassManager().getClassById(classId);
            if (pClass != null) {
                classMultiplier = pClass.getStatMultiplier(stat);
            }
        }

        double finalValue = baseValue * classMultiplier;
        
        // Aggiungi nuovo modifier
        AttributeModifier modifier = new AttributeModifier(
            key,
            finalValue,
            AttributeModifier.Operation.ADD_NUMBER,
            org.bukkit.inventory.EquipmentSlotGroup.ANY
        );
        instance.addModifier(modifier);
    }

    public void clearCache(UUID uuid) {
        playerProfiles.remove(uuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        recomputeAllModifiers(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        clearCache(event.getPlayer().getUniqueId());
    }

    public static StatEngine getInstance() {
        return instance;
    }
}
