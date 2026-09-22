package com.example.plugin.sistema.services;

import com.example.plugin.sistema.SistemaPlugin;
import com.example.plugin.sistema.models.PlayerProfile;
import com.example.plugin.sistema.models.StatDefinition;
import com.example.plugin.sistema.utils.ProgressBarUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.Operation;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce l'XP, le BossBar di progresso e i multiplier da Intelligenza.
 */
public class XpService implements Listener {

    private final SistemaPlugin plugin;
    // Mappa delle BossBar per giocatore
    private final Map<UUID, BossBar> bossBars;

    public XpService(SistemaPlugin plugin) {
        this.plugin = plugin;
        this.bossBars = new ConcurrentHashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Calcola l'XP necessario per il prossimo livello.
     * Formula: base + linear*level + quadratic*level^2
     */
    public long getXpNeededForNextLevel(int currentLevel) {
        int base = plugin.getConfig().getInt("system.xp.base-xp-for-level", 50);
        int linear = plugin.getConfig().getInt("system.xp.linear-coefficient", 25);
        int quadratic = plugin.getConfig().getInt("system.xp.quadratic-coefficient", 5);
        
        return (long) (base + linear * currentLevel + quadratic * Math.pow(currentLevel, 2));
    }

    /**
     * Concede XP a un giocatore, gestendo level-up e multiplier.
     * @param player Il giocatore.
     * @param amount Amount base di XP (prima dei multiplier).
     * @param source Fonte dell'XP ("kill_mob", "break_block", "fishing", "quest").
     */
    public void grantXp(Player player, long amount, String source) {
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) return;

        // Applica moltiplicatori
        double multiplier = 1.0;
        
        // Moltiplicatore fonte (config)
        multiplier *= plugin.getConfig().getDouble("system.xp.multipliers." + source, 1.0);
        
        // Moltiplicatore Intelligenza
        int intelligenzaPoints = profile.getStatValue(StatDefinition.INTELLIGENZA);
        double intelligenzaMultiplierPerPoint = plugin.getConfig().getDouble("system.stats.intelligence.xp-multiplier-per-point", 0.02);
        multiplier += (intelligenzaPoints * intelligenzaMultiplierPerPoint);

        // Moltiplicatore classe (se esiste)
        if (profile.getChosenClass() != null) {
            // La classe potrebbe avere un bonus globale XP, se definito in futuro
        }

        long finalAmount = (long) (amount * multiplier);
        if (finalAmount < 1) finalAmount = 1;

        // Controlla soft-cap del rank
        Rank currentRank = profile.getCurrentRank();
        if (currentRank != null && currentRank.hasTrial()) {
            int maxLevelForRank = currentRank.getMaxLevel();
            long xpAtMaxLevel = calculateTotalXpForLevel(maxLevelForRank);
            
            if (profile.getTotalXp() >= xpAtMaxLevel) {
                // Giocatore già al cap del rank, accumula ma non processa level-up
                profile.setTotalXp(profile.getTotalXp() + finalAmount);
                plugin.getPlayerProfileService().saveProfile(profile, false);
                updateBossBar(player);
                sendActionBar(player, finalAmount);
                return;
            } else if (profile.getTotalXp() + finalAmount > xpAtMaxLevel) {
                // Questo grant supera il cap: accumula tutto ma processa solo fino al cap
                long overflow = (profile.getTotalXp() + finalAmount) - xpAtMaxLevel;
                profile.setTotalXp(profile.getTotalXp() + finalAmount); // Accumula tutto
                // I level-up oltre il cap saranno processati al completamento della trial
                plugin.getPlayerProfileService().saveProfile(profile, false);
                updateBossBar(player);
                sendActionBar(player, finalAmount);
                return;
            }
        }

        // Nessun soft-cap o sotto il cap: processa normalmente
        profile.setTotalXp(profile.getTotalXp() + finalAmount);
        
        // Processa level-up
        plugin.getLevelingManager().checkLevelUps(player, profile);
        
        // Salva e aggiorna UI
        plugin.getPlayerProfileService().saveProfile(profile, false);
        updateBossBar(player);
        sendActionBar(player, finalAmount);
    }

    /**
     * Calcola l'XP totale necessario per raggiungere un certo livello (cumulativo).
     */
    public long calculateTotalXpForLevel(int targetLevel) {
        long total = 0;
        for (int i = 1; i < targetLevel; i++) {
            total += getXpNeededForNextLevel(i);
        }
        return total;
    }

    /**
     * Crea o aggiorna la BossBar dell'XP per un giocatore.
     */
    public void updateBossBar(Player player) {
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) return;

        int level = profile.getLevel();
        long totalXp = profile.getTotalXp();
        long xpNeeded = getXpNeededForNextLevel(level);
        long xpInCurrentLevel = totalXp - calculateTotalXpForLevel(level);

        String title = plugin.getMessage("boss-bar.format")
                .replace("%level%", String.valueOf(level))
                .replace("%xp%", String.valueOf(xpInCurrentLevel))
                .replace("%xp_needed%", String.valueOf(xpNeeded));
        
        title = com.example.plugin.sistema.utils.ColorUtils.translate(title);

        double progress = Math.min(1.0, Math.max(0.0, (double) xpInCurrentLevel / xpNeeded));

        BossBar bossBar = bossBars.computeIfAbsent(player.getUniqueId(), uuid -> {
            BossBar newBar = Bukkit.createBossBar(title, BarColor.BLUE, BarStyle.SOLID);
            newBar.addPlayer(player);
            return newBar;
        });

        bossBar.setTitle(title);
        bossBar.setProgress(progress);
    }

    /**
     * Rimuove la BossBar di un giocatore.
     */
    public void removeBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    /**
     * Invia un ActionBar con l'XP guadagnato.
     */
    private void sendActionBar(Player player, long amount) {
        String message = plugin.getMessage("action-bar.xp-gain").replace("%d", String.valueOf(amount));
        message = com.example.plugin.sistema.utils.ColorUtils.translate(message);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        // Ritarda leggermente la creazione della BossBar
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            if (plugin.getPlayerProfileService().isProfileReady(player.getUniqueId())) {
                updateBossBar(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeBossBar(event.getPlayer());
    }

    /**
     * Toggle della BossBar per un giocatore.
     */
    public void toggleBossBar(Player player) {
        if (bossBars.containsKey(player.getUniqueId())) {
            removeBossBar(player);
            player.sendMessage(ChatColor.GRAY + "BossBar XP disattivata.");
        } else {
            updateBossBar(player);
            player.sendMessage(ChatColor.GRAY + "BossBar XP attivata.");
        }
    }
}
