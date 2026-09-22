package com.example.plugin.sistema.services;

import com.example.plugin.sistema.SistemaPlugin;
import com.example.plugin.sistema.models.PlayerProfile;
import com.example.plugin.sistema.models.Rank;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Gestisce la logica dei level-up, suoni, effetti e assegnazione punti statistica.
 */
public class LevelingManager {

    private final SistemaPlugin plugin;

    public LevelingManager(SistemaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Controlla se il giocatore ha abbastanza XP per uno o più level-up e li processa.
     */
    public void checkLevelUps(Player player, PlayerProfile profile) {
        int maxLevel = plugin.getConfig().getInt("system.xp.max-level", 100);
        if (profile.getLevel() >= maxLevel) return;

        XpService xpService = plugin.getXpService();
        boolean leveledUp = false;

        while (profile.getLevel() < maxLevel) {
            long xpNeeded = xpService.getXpNeededForNextLevel(profile.getLevel());
            long totalXpForNextLevel = xpService.calculateTotalXpForLevel(profile.getLevel() + 1);

            if (profile.getTotalXp() >= totalXpForNextLevel) {
                // Level up!
                profile.setLevel(profile.getLevel() + 1);
                profile.addUnspentStatPoints(3); // +3 punti per livello
                leveledUp = true;

                // Effetti level-up
                playLevelUpEffects(player, profile.getLevel());

                // Controlla se è un milestone (es. ogni 10 livelli)
                if (profile.getLevel() % 10 == 0) {
                    broadcastLevelMilestone(player, profile.getLevel());
                }

                // Controlla rank-up
                checkRankUp(player, profile);

            } else {
                break; // Non abbastanza XP per il prossimo livello
            }
        }

        if (leveledUp) {
            // Aggiorna BossBar dopo tutti i level-up
            xpService.updateBossBar(player);
            
            // Salva profilo
            plugin.getPlayerProfileService().saveProfile(profile, false);
            
            // Ricalcola statistiche (nuovi punti disponibili)
            plugin.getStatEngine().recomputeAllModifiers(player);
        }
    }

    /**
     * Processa i level-up pendenti accumulati durante il soft-cap di un rank.
     * Da chiamare quando il giocatore completa la trial quest.
     */
    public void processPendingLevelUps(Player player) {
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null || profile.getPendingLevelUps() <= 0) return;

        int pending = profile.getPendingLevelUps();
        int maxLevel = plugin.getConfig().getInt("system.xp.max-level", 100);
        
        for (int i = 0; i < pending && profile.getLevel() < maxLevel; i++) {
            profile.setLevel(profile.getLevel() + 1);
            profile.addUnspentStatPoints(3);
            playLevelUpEffects(player, profile.getLevel());
            checkRankUp(player, profile);
        }

        profile.setPendingLevelUps(0);
        plugin.getXpService().updateBossBar(player);
        plugin.getPlayerProfileService().saveProfile(profile, false);
        plugin.getStatEngine().recomputeAllModifiers(player);
        
        player.sendMessage(ChatColor.GREEN + "Hai recuperato " + pending + " level-up accumulati!");
    }

    /**
     * Esegue effetti visivi e sonori per un level-up.
     */
    private void playLevelUpEffects(Player player, int newLevel) {
        // Suono
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // Title
        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "LIVELLO " + newLevel;
        String subtitle = ChatColor.WHITE + "+3 Punti Statistica Disponibili";
        player.sendTitle(title, subtitle, 10, 60, 20);

        // Messaggio personale
        String msg = plugin.getMessage("success.level-up")
                .replace("%level%", String.valueOf(newLevel));
        player.sendMessage(com.example.plugin.sistema.utils.ColorUtils.translate(msg));
    }

    /**
     * Controlla se il giocatore ha raggiunto un nuovo rank.
     */
    private void checkRankUp(Player player, PlayerProfile profile) {
        RankManager rankManager = plugin.getRankManager();
        Rank newRank = rankManager.getRankForLevel(profile.getLevel());

        if (newRank != null && (profile.getCurrentRank() == null || !profile.getCurrentRank().getId().equals(newRank.getId()))) {
            // Rank up!
            Rank oldRank = profile.getCurrentRank();
            profile.setCurrentRank(newRank);

            // Broadcast solo se non è il primo rank
            if (oldRank != null) {
                broadcastRankUp(player, newRank);
            }

            // Controlla se la nuova classe è ora sbloccabile
            plugin.getClassManager().checkClassUnlock(player, profile);
        }
    }

    /**
     * Broadcast del rank-up a tutto il server.
     */
    private void broadcastRankUp(Player player, Rank newRank) {
        String message = plugin.getMessage("broadcast.rank-up")
                .replace("%player%", player.getName())
                .replace("%s%", player.getName())
                .replace("%rank%", newRank.getId())
                .replace("%display_name%", newRank.getDisplayName());
        
        message = com.example.plugin.sistema.utils.ColorUtils.translate(message);
        Bukkit.broadcastMessage(message);

        // Title a tutti i giocatori
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.5f);
        }
    }

    /**
     * Broadcast per milestone di livello (ogni 10 livelli).
     */
    private void broadcastLevelMilestone(Player player, int level) {
        String message = plugin.getMessage("broadcast.level-up-milestone")
                .replace("%player%", player.getName())
                .replace("%s%", player.getName())
                .replace("%d%", String.valueOf(level));
        
        message = com.example.plugin.sistema.utils.ColorUtils.translate(message);
        Bukkit.broadcastMessage(message);
    }
}
