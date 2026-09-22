package com.galaxyleveling.service;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * Servizio per la classifica (Leaderboard).
 * Mantiene una snapshot immutabile in memoria, aggiornata periodicamente in async.
 * Evita letture su disco/DB sincrone durante l'uso di placeholder o GUI.
 */
public class LeaderboardService {

    private final GalaxyLeveling plugin;
    private final int refreshIntervalSeconds;
    private final int maxSize;
    
    // Snapshot immutabile della classifica
    private volatile List<LeaderboardEntry> leaderboardSnapshot = new CopyOnWriteArrayList<>();
    
    private BukkitRunnable refreshTask;

    public LeaderboardService(GalaxyLeveling plugin) {
        this.plugin = plugin;
        this.refreshIntervalSeconds = plugin.getConfig().getInt("leaderboard.refresh-interval-seconds", 300);
        this.maxSize = plugin.getConfig().getInt("leaderboard.max-size", 10);
        
        startRefreshTask();
    }

    private void startRefreshTask() {
        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                refreshLeaderboard();
            }
        };
        refreshTask.runTaskTimerAsynchronously(plugin, refreshIntervalSeconds * 20L, refreshIntervalSeconds * 20L);
        
        // Primo refresh immediato dopo un breve delay
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::refreshLeaderboard, 20L);
    }

    private void refreshLeaderboard() {
        try {
            PlayerProfileService profileService = plugin.getPlayerProfileService();
            List<LeaderboardEntry> entries = new ArrayList<>();

            // Legge tutti i profili caricati in cache (non fa I/O su disco per quelli non in memoria)
            // Per una classifica completa servirebbe leggere tutti i file/db, ma qui usiamo solo cache per performance
            // In produzione, si dovrebbe iterare su tutti i file nella cartella playerdata o fare query SQL
            for (PlayerProfile profile : profileService.getAllLoadedProfiles()) {
                entries.add(new LeaderboardEntry(
                    profile.getPlayerName(), // Deve essere cached o letto dal file
                    profile.getLevel(),
                    profile.getTotalXp(),
                    profile.getCurrentRank().getDisplayName(),
                    profile.getCurrentRank().getColorHex()
                ));
            }

            // Ordina per livello (desc), poi XP (desc) come tiebreaker
            entries.sort(Comparator.comparingInt(LeaderboardEntry::getLevel).reversed()
                    .thenComparingLong(LeaderboardEntry::getTotalXp).reversed());

            // Taglia alla dimensione massima
            if (entries.size() > maxSize) {
                entries = entries.subList(0, maxSize);
            }

            // Sostituzione atomica della snapshot
            leaderboardSnapshot = new CopyOnWriteArrayList<>(entries);
            
            plugin.getLogger().log(Level.FINE, "Leaderboard aggiornata con " + entries.size() + " giocatori");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante l'aggiornamento della leaderboard", e);
        }
    }

    /**
     * Ottiene la voce alla posizione specificata (0-based).
     */
    public LeaderboardEntry getEntryAt(int index) {
        if (index < 0 || index >= leaderboardSnapshot.size()) {
            return null;
        }
        return leaderboardSnapshot.get(index);
    }

    /**
     * Ottiene una copia della classifica completa.
     */
    public List<LeaderboardEntry> getLeaderboard() {
        return Collections.unmodifiableList(leaderboardSnapshot);
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
    }

    /**
     * Classe entry immutabile per la leaderboard.
     */
    public static class LeaderboardEntry {
        private final String playerName;
        private final int level;
        private final long totalXp;
        private final String rankDisplayName;
        private final String rankColorHex;

        public LeaderboardEntry(String playerName, int level, long totalXp, String rankDisplayName, String rankColorHex) {
            this.playerName = playerName;
            this.level = level;
            this.totalXp = totalXp;
            this.rankDisplayName = rankDisplayName;
            this.rankColorHex = rankColorHex;
        }

        public String getPlayerName() { return playerName; }
        public int getLevel() { return level; }
        public long getTotalXp() { return totalXp; }
        public String getRankDisplayName() { return rankDisplayName; }
        public String getRankColorHex() { return rankColorHex; }
    }
}