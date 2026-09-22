package com.example.plugin.sistema.services;

import com.example.plugin.sistema.SistemaPlugin;
import com.example.plugin.sistema.models.Rank;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.logging.Level;

/**
 * Gestisce il caricamento dei rank da ranks.yml e la logica di progressione.
 */
public class RankManager {

    private final SistemaPlugin plugin;
    private final Map<String, Rank> ranksById = new LinkedHashMap<>();
    private final List<Rank> ranksByLevel = new ArrayList<>();
    private Rank defaultRank;

    public RankManager(SistemaPlugin plugin) {
        this.plugin = plugin;
        loadRanks();
    }

    /**
     * Carica i rank da ranks.yml.
     */
    public void loadRanks() {
        ranksById.clear();
        ranksByLevel.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ranks");
        if (section == null) {
            plugin.getLogger().warning("Nessun rank trovato in ranks.yml! Uso rank default.");
            defaultRank = new Rank("e", "Novizio", "#808080", 1, 100, null);
            ranksById.put("e", defaultRank);
            ranksByLevel.add(defaultRank);
            return;
        }

        List<Rank> loadedRanks = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection rankSection = section.getConfigurationSection(key);
            if (rankSection == null) continue;

            Rank rank = Rank.fromConfig(rankSection);
            if (rank != null) {
                loadedRanks.add(rank);
                ranksById.put(rank.getId(), rank);
            } else {
                plugin.getLogger().warning("Rank invalido saltato: " + key);
            }
        }

        // Ordina per minLevel
        loadedRanks.sort(Comparator.comparingInt(Rank::getMinLevel));
        ranksByLevel.addAll(loadedRanks);

        if (!ranksByLevel.isEmpty()) {
            defaultRank = ranksByLevel.get(0);
        } else {
            plugin.getLogger().severe("Nessun rank valido caricato! Creo rank default.");
            defaultRank = new Rank("e", "Novizio", "#808080", 1, 100, null);
            ranksById.put("e", defaultRank);
            ranksByLevel.add(defaultRank);
        }

        plugin.getLogger().info("Caricati " + ranksById.size() + " rank.");
    }

    /**
     * Ottiene un rank per ID.
     */
    public Rank getRankById(String id) {
        return ranksById.get(id);
    }

    /**
     * Ottiene il rank default (il primo).
     */
    public Rank getDefaultRank() {
        return defaultRank;
    }

    /**
     * Ottiene il rank appropriato per un dato livello.
     */
    public Rank getRankForLevel(int level) {
        Rank current = defaultRank;
        for (Rank rank : ranksByLevel) {
            if (level >= rank.getMinLevel()) {
                current = rank;
            } else {
                break;
            }
        }
        return current;
    }

    /**
     * Ottiene tutti i rank ordinati per livello.
     */
    public List<Rank> getAllRanks() {
        return Collections.unmodifiableList(ranksByLevel);
    }

    /**
     * Controlla se un rank richiede una trial quest.
     */
    public boolean hasTrial(Rank rank) {
        return rank != null && rank.hasTrial();
    }

    /**
     * Ottiene il prossimo rank dopo quello dato.
     */
    public Rank getNextRank(Rank currentRank) {
        if (currentRank == null) return defaultRank;
        
        int index = ranksByLevel.indexOf(currentRank);
        if (index >= 0 && index < ranksByLevel.size() - 1) {
            return ranksByLevel.get(index + 1);
        }
        return null; // Ultimo rank
    }
}
