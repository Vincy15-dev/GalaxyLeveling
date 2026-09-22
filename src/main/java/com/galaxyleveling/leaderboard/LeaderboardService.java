package com.galaxyleveling.leaderboard;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.model.Rank;
import com.galaxyleveling.service.PlayerProfileService;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servizio leaderboard: snapshot ordinata per livello (poi XP) refreshata periodicamente.
 * Lettura IMMUTABILE e thread-safe: GUI e placeholder usano solo questa cache.
 * Config: leaderboard.refresh-interval-seconds (default 300s = 5min).
 */
public class LeaderboardService {

    private final GalaxyLeveling plugin;
    private final PlayerProfileService profileService;
    private final int maxEntries;
    private final int refreshIntervalSeconds;

    // Snapshot immutabile: lista ordinata di entry
    private volatile List<LeaderboardEntry> cachedSnapshot = new ArrayList<>();
    private BukkitTask refreshTask;

    public LeaderboardService(GalaxyLeveling plugin, PlayerProfileService profileService) {
        this.plugin = plugin;
        this.profileService = profileService;
        this.maxEntries = plugin.getConfig().getInt("system.quests.max-leaderboard-size", 10);
        this.refreshIntervalSeconds = plugin.getConfig().getInt("system.ranks.update-interval", 300);

        startRefreshTask();
    }

    /**
     * Avvia task asincrono periodico per refreshare la snapshot.
     */
    private void startRefreshTask() {
        refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                refreshSnapshot();
            } catch (Exception e) {
                plugin.getLogger().severe("Errore nel refresh leaderboard: " + e.getMessage());
            }
        }, 20L * refreshIntervalSeconds, 20L * refreshIntervalSeconds);
    }

    /**
     * Refresha la snapshot leggendo tutti i profili caricati.
     * ORDINAMENTO: prima per livello (decrescente), poi per XP totale (decrescente).
     */
    private void refreshSnapshot() {
        Collection<PlayerProfile> allProfiles = profileService.getAllLoadedProfiles();
        
        List<LeaderboardEntry> entries = allProfiles.stream()
            .filter(Objects::nonNull)
            .map(p -> {
                Rank rank = p.getCurrentRank();
                return new LeaderboardEntry(
                    p.getPlayerUuid(),
                    getOfflinePlayerName(p.getPlayerUuid()),
                    p.getLevel(),
                    p.getTotalXp(),
                    rank != null ? rank.getDisplayName() : "Nessuno",
                    rank != null ? rank.getColorHex() : "#FFFFFF"
                );
            })
            .sorted((a, b) -> {
                // Prima per livello (decrescente)
                int levelCompare = Integer.compare(b.getLevel(), a.getLevel());
                if (levelCompare != 0) return levelCompare;
                // Poi per XP (decrescente)
                return Long.compare(b.getTotalXp(), a.getTotalXp());
            })
            .limit(maxEntries)
            .collect(Collectors.toList());

        // Swap atomic della snapshot
        this.cachedSnapshot = Collections.unmodifiableList(entries);
        plugin.getLogger().info("Leaderboard aggiornata: " + entries.size() + " giocatori.");
    }

    /**
     * Ottiene il nome del giocatore (cache o fallback offline).
     */
    private String getOfflinePlayerName(UUID uuid) {
        var player = Bukkit.getPlayer(uuid);
        if (player != null) return player.getName();
        // Fallback: usa l'UUID se non online (in produzione si potrebbe usare un lookup offline)
        return uuid.toString().substring(0, 8);
    }

    /**
     * Restituisce la entry alla posizione specificata (0-based).
     * Null se fuori range.
     */
    public LeaderboardEntry getEntryAt(int position) {
        if (position < 0 || position >= cachedSnapshot.size()) return null;
        return cachedSnapshot.get(position);
    }

    /**
     * Restituisce una copia della snapshot corrente.
     */
    public List<LeaderboardEntry> getSnapshot() {
        return new ArrayList<>(cachedSnapshot);
    }

    /**
     * Ferma il task di refresh (onDisable).
     */
    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
    }

    /**
     * Entry immutabile per la leaderboard.
     */
    public static class LeaderboardEntry {
        private final UUID playerUuid;
        private final String playerName;
        private final int level;
        private final long totalXp;
        private final String rankDisplay;
        private final String rankColor;

        public LeaderboardEntry(UUID playerUuid, String playerName, int level, long totalXp, String rankDisplay, String rankColor) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.level = level;
            this.totalXp = totalXp;
            this.rankDisplay = rankDisplay;
            this.rankColor = rankColor;
        }

        public UUID getPlayerUuid() { return playerUuid; }
        public String getPlayerName() { return playerName; }
        public int getLevel() { return level; }
        public long getTotalXp() { return totalXp; }
        public String getRankDisplay() { return rankDisplay; }
        public String getRankColor() { return rankColor; }
    }
}
