package com.galaxyleveling.expansion;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.model.Rank;
import com.galaxyleveling.model.StatDefinition;
import com.galaxyleveling.service.LeaderboardService;
import com.galaxyleveling.service.PlayerProfileService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion per IL SISTEMA.
 * Fornisce placeholder per livello, grado, classe, statistiche e leaderboard.
 * Registrazione condizionata: solo se PlaceholderAPI è presente.
 */
public class SistemaExpansion extends PlaceholderExpansion {

    private final GalaxyLeveling plugin;
    private final PlayerProfileService profileService;
    private final LeaderboardService leaderboardService;

    public SistemaExpansion(GalaxyLeveling plugin, PlayerProfileService profileService, LeaderboardService leaderboardService) {
        this.plugin = plugin;
        this.profileService = profileService;
        this.leaderboardService = leaderboardService;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "sistema";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "GalaxyLeveling";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // Rimane registrato anche dopo /reload
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            // Placeholder globali (es. leaderboard) funzionano anche senza player
            if (params.startsWith("top_")) {
                return handleLeaderboardPlaceholder(params);
            }
            return null;
        }

        PlayerProfile profile = profileService.getProfile(player.getUniqueId());
        if (profile == null) {
            return "Caricamento...";
        }

        switch (params.toLowerCase()) {
            case "level":
                return String.valueOf(profile.getLevel());
            case "rank":
                Rank rank = profile.getCurrentRank();
                return rank != null ? rank.getDisplayName() : "Nessuno";
            case "rank_color":
                Rank rColor = profile.getCurrentRank();
                return rColor != null ? rColor.getColorHex() : "#FFFFFF";
            case "rank_prefix":
                Rank rPrefix = profile.getCurrentRank();
                return rPrefix != null ? rPrefix.getDisplayName() : "";
            case "class":
                return profile.getChosenClassId() != null ? profile.getChosenClassId() : "Nessuna";
            case "xp":
                return String.valueOf(profile.getTotalXp());
            case "xp_next":
                int nextLevel = profile.getLevel() + 1;
                long needed = GalaxyLeveling.getInstance().getXpService().xpForLevel(nextLevel);
                return String.valueOf(needed);
            case "points":
                return String.valueOf(profile.getUnspentStatPoints());
            case "stat_forza":
                return String.format("%.2f", profile.getCachedStatValue(StatDefinition.FORZA));
            case "stat_agilita":
                return String.format("%.2f", profile.getCachedStatValue(StatDefinition.AGILITA));
            case "stat_vitalita":
                return String.format("%.2f", profile.getCachedStatValue(StatDefinition.VITALITA));
            case "stat_intelligenza":
                return String.format("%.2f", profile.getCachedStatValue(StatDefinition.INTELLIGENZA));
            case "stat_percezione":
                return String.format("%.2f", profile.getCachedStatValue(StatDefinition.PERCEZIONE));
            default:
                if (params.startsWith("top_")) {
                    return handleLeaderboardPlaceholder(params);
                }
                return null;
        }
    }

    private String handleLeaderboardPlaceholder(String params) {
        // Formato: top_<N>_name, top_<N>_level, top_<N>_rank
        String[] parts = params.split("_");
        if (parts.length < 3) return null;

        try {
            int position = Integer.parseInt(parts[1]);
            if (position < 1 || position > 10) return null;

            String type = parts[2];
            var entry = leaderboardService.getEntryAt(position - 1);
            if (entry == null) return "-";

            switch (type) {
                case "name":
                    return entry.getPlayerName();
                case "level":
                    return String.valueOf(entry.getLevel());
                case "rank":
                    return entry.getRankDisplay();
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
