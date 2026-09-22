package com.galaxyleveling.service;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.model.Rank;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RankManager {
    private final GalaxyLeveling plugin;
    private final List<Rank> ranks = new ArrayList<>();

    public RankManager(GalaxyLeveling plugin) {
        this.plugin = plugin;
        loadRanks();
    }

    private void loadRanks() {
        // Carica da ranks.yml
        if (!plugin.getConfig().contains("ranks")) {
            // Ranks di default se non configurati
            ranks.add(new Rank("e", "Novizio", "#808080", 1, 10, null));
            ranks.add(new Rank("d", "Apprendista", "#00FF00", 11, 25, "trial_d_to_c"));
            ranks.add(new Rank("c", "Adepto", "#0080FF", 26, 40, "trial_c_to_b"));
            ranks.add(new Rank("b", "Esperto", "#8000FF", 41, 60, "trial_b_to_a"));
            ranks.add(new Rank("a", "Maestro", "#FF8000", 61, 80, "trial_a_to_s"));
            ranks.add(new Rank("s", "Leggenda", "#FFD700", 81, 100, null));
        } else {
            // Caricamento da config (implementazione futura)
        }
        Collections.sort(ranks, (a, b) -> a.getMinLevel() - b.getMinLevel());
    }

    public Rank getRankById(String id) {
        for (Rank rank : ranks) {
            if (rank.getId().equals(id)) return rank;
        }
        return null;
    }

    public Rank getDefaultRank() {
        return ranks.isEmpty() ? null : ranks.get(0);
    }

    public Rank getRankForLevel(int level) {
        Rank result = getDefaultRank();
        for (Rank rank : ranks) {
            if (level >= rank.getMinLevel() && level <= rank.getMaxLevel()) {
                result = rank;
            }
        }
        return result;
    }

    public void checkRankUp(Player player, PlayerProfile profile) {
        Rank currentRank = getRankById(profile.getCurrentRankId());
        Rank newRank = getRankForLevel(profile.getLevel());

        if (newRank != null && currentRank != null && !newRank.getId().equals(currentRank.getId())) {
            // Controlla trial quest
            if (newRank.getRequiresTrial() != null) {
                // Soft-cap: blocca il rank up finché la trial non è completata
                // Implementazione dettagliata in QuestProgressService
                return;
            }

            profile.setCurrentRankId(newRank.getId());
            
            // Broadcast server-wide
            String message = "&6" + player.getName() + " è stato promosso al grado " + 
                           newRank.getDisplayName() + "&r!";
            for (Player p : player.getServer().getOnlinePlayers()) {
                p.sendMessage(com.galaxyleveling.util.ColorUtils.colorize(message));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                p.sendTitle("", com.galaxyleveling.util.ColorUtils.colorize("&6&l" + newRank.getDisplayName()), 20, 60, 20);
            }
        }
    }
}
