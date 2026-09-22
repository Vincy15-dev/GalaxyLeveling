package com.galaxyleveling.integration;

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

public class SistemaExpansion extends PlaceholderExpansion {

    private final GalaxyLeveling plugin;

    public SistemaExpansion(GalaxyLeveling plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "sistema";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GalaxyLeveling";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        PlayerProfileService profileService = plugin.getPlayerProfileService();
        if (!profileService.isProfileReady(player.getUniqueId())) {
            return "N/A";
        }

        PlayerProfile profile = profileService.getProfile(player.getUniqueId());

        switch (params.toLowerCase()) {
            case "level":
                return String.valueOf(profile.getLevel());
            case "rank":
                return profile.getCurrentRank().getDisplayName();
            case "rank_color":
                return profile.getCurrentRank().getColorHex();
            case "rank_prefix":
                return profile.getCurrentRank().getDisplayName(); // O un campo separato se presente
            case "class":
                String classId = profile.getChosenClassId();
                if (classId == null) return "Nessuna";
                var pClass = plugin.getClassManager().getClassById(classId);
                return pClass != null ? pClass.getDisplayName() : "Sconosciuta";
            case "xp":
                return String.valueOf(profile.getTotalXp());
            case "xp_next":
                return String.valueOf(plugin.getStatEngine().getXpForLevel(profile.getLevel() + 1));
            case "points":
                return String.valueOf(profile.getUnspentStatPoints());
            case "stat_forza":
                return String.valueOf(profile.getStatValue(StatDefinition.FORZA));
            case "stat_agilita":
                return String.valueOf(profile.getStatValue(StatDefinition.AGILITA));
            case "stat_vitalita":
                return String.valueOf(profile.getStatValue(StatDefinition.VITALITA));
            case "stat_intelligenza":
                return String.valueOf(profile.getStatValue(StatDefinition.INTELLIGENZA));
            case "stat_percezione":
                return String.valueOf(profile.getStatValue(StatDefinition.PERCEZIONE));
            default:
                // Gestione placeholder leaderboard: sistema_top_<N>_name|level|rank
                if (params.startsWith("top_")) {
                    String[] parts = params.substring(4).split("_");
                    if (parts.length >= 2) {
                        int position;
                        try {
                            position = Integer.parseInt(parts[0]);
                        } catch (NumberFormatException e) {
                            return "";
                        }
                        String type = parts[1].toLowerCase();
                        
                        LeaderboardService lbService = plugin.getLeaderboardService();
                        var entry = lbService.getEntryAt(position - 1); // 0-based index
                        
                        if (entry == null) return "";
                        
                        switch (type) {
                            case "name":
                                return entry.getPlayerName();
                            case "level":
                                return String.valueOf(entry.getLevel());
                            case "rank":
                                return entry.getRankDisplayName();
                            default:
                                return "";
                        }
                    }
                }
                return "";
        }
    }
}
