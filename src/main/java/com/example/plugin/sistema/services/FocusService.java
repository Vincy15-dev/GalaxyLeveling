package com.example.plugin.sistema.services;

import com.example.plugin.sistema.SistemaPlugin;
import com.example.plugin.sistema.models.PlayerClass;
import com.example.plugin.sistema.models.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce il Focus (risorsa tipo mana) per la statistica Intelligenza.
 * Il Focus si rigenera nel tempo e può essere usato per abilità future.
 */
public class FocusService {

    private final SistemaPlugin plugin;
    
    // Cache del focus corrente per giocatore (UUID -> currentFocus)
    private final Map<UUID, Double> focusCache = new ConcurrentHashMap<>();
    
    // Task di rigenerazione
    private int regenTaskId = -1;

    public FocusService(SistemaPlugin plugin) {
        this.plugin = plugin;
        startRegenTask();
    }

    /**
     * Avvia il task di rigenerazione del focus (ogni secondo).
     */
    private void startRegenTask() {
        regenTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            this::regenerateAllFocus,
            20L, // 1 secondo
            20L
        ).getTaskId();
    }

    /**
     * Rigenera il focus per tutti i giocatori online.
     */
    private void regenerateAllFocus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            PlayerProfile profile = plugin.getPlayerProfileService().getProfile(uuid);
            
            if (profile == null) continue;

            double currentFocus = focusCache.getOrDefault(uuid, 0.0);
            
            // Calcola rigenerazione base + bonus da Intelligenza + bonus da classe
            double intelligenzaValue = profile.getStatValue(com.example.plugin.sistema.models.StatDefinition.INTELLIGENZA);
            double baseRegen = plugin.getConfig().getDouble("system.stats.intelligence.focus-regen", 5.0);
            
            // Bonus da classe (Arcanista ha +2.0)
            double classBonus = 0.0;
            PlayerClass pClass = profile.getChosenClass();
            if (pClass != null) {
                classBonus = pClass.getFocusRegenBonus();
            }

            double regenPerTick = (baseRegen + classBonus + (intelligenzaValue * 0.1)) / 20.0; // Diviso per 20 tick
            
            double maxFocus = plugin.getConfig().getDouble("system.stats.intelligence.focus-max", 100.0);
            double newFocus = Math.min(maxFocus, currentFocus + regenPerTick);
            
            focusCache.put(uuid, newFocus);
            profile.setFocusedAmount(newFocus);
        }
    }

    /**
     * Ottiene il focus corrente di un giocatore.
     */
    public double getFocus(UUID uuid) {
        return focusCache.getOrDefault(uuid, 0.0);
    }

    /**
     * Imposta il focus corrente di un giocatore.
     */
    public void setFocus(UUID uuid, double amount) {
        double maxFocus = plugin.getConfig().getDouble("system.stats.intelligence.focus-max", 100.0);
        focusCache.put(uuid, Math.max(0.0, Math.min(amount, maxFocus)));
    }

    /**
     * Spendee il focus di un giocatore. Ritorna true se sufficiente.
     */
    public boolean spendFocus(UUID uuid, double amount) {
        double current = getFocus(uuid);
        if (current >= amount) {
            setFocus(uuid, current - amount);
            return true;
        }
        return false;
    }

    /**
     * Aggiunge focus a un giocatore (es. da abilità o item).
     */
    public void addFocus(UUID uuid, double amount) {
        double current = getFocus(uuid);
        double maxFocus = plugin.getConfig().getDouble("system.stats.intelligence.focus-max", 100.0);
        setFocus(uuid, Math.min(maxFocus, current + amount));
    }

    /**
     * Inizializza il focus per un giocatore al join.
     */
    public void initPlayerFocus(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(uuid);
        
        if (profile != null) {
            double maxFocus = plugin.getConfig().getDouble("system.stats.intelligence.focus-max", 100.0);
            double savedFocus = profile.getFocusAmount();
            
            // Clamp tra 0 e max
            focusCache.put(uuid, Math.max(0.0, Math.min(savedFocus, maxFocus)));
        } else {
            focusCache.put(uuid, 0.0);
        }
    }

    /**
     * Rimuove un giocatore dalla cache.
     */
    public void removePlayer(UUID uuid) {
        focusCache.remove(uuid);
    }

    /**
     * Ferma il task di rigenerazione.
     */
    public void shutdown() {
        if (regenTaskId != -1) {
            Bukkit.getScheduler().cancelTask(regenTaskId);
        }
    }
}
