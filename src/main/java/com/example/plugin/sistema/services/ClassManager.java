package com.example.plugin.sistema.services;

import com.example.plugin.sistema.SistemaPlugin;
import com.example.plugin.sistema.models.PlayerClass;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

/**
 * Gestisce il caricamento delle classi dalle configurazioni e fornisce metodi
 * per accedere alle definizioni delle classi disponibili.
 */
public class ClassManager {

    private final SistemaPlugin plugin;
    private final Map<String, PlayerClass> classesById = new HashMap<>();
    private String unlockRankId = "c"; // Default rank per sblocco classi

    public ClassManager(SistemaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Carica tutte le classi dal file classes.yml
     */
    public void loadClasses() {
        classesById.clear();
        
        FileConfiguration config = plugin.getClassesConfig();
        if (config == null) {
            plugin.getLogger().warning("classes.yml non trovato o nullo!");
            return;
        }

        // Carica l'unlock-rank
        unlockRankId = config.getString("unlock-rank", "c");

        ConfigurationSection classesSection = config.getConfigurationSection("classes");
        if (classesSection == null) {
            plugin.getLogger().warning("Nessuna classe trovata in classes.yml!");
            return;
        }

        for (String key : classesSection.getKeys(false)) {
            try {
                ConfigurationSection classSec = classesSection.getConfigurationSection(key);
                if (classSec == null) continue;

                String id = key;
                String displayName = classSec.getString("display-name", id);
                String iconMaterial = classSec.getString("icon", "BOOK");
                
                List<String> colorGradient = classSec.getStringList("color-gradient");
                if (colorGradient.isEmpty()) {
                    colorGradient = Arrays.asList("#FFFFFF", "#CCCCCC");
                }

                List<String> description = classSec.getStringList("description");
                String passiveText = classSec.getString("passive", "");
                double focusRegenBonus = classSec.getDouble("focus-regen-bonus", 0.0);

                // Carica i bonus delle statistiche
                Map<String, Double> statBonusMap = new HashMap<>();
                ConfigurationSection bonusSec = classSec.getConfigurationSection("stat-bonus");
                if (bonusSec != null) {
                    for (String statKey : bonusSec.getKeys(false)) {
                        statBonusMap.put(statKey, bonusSec.getDouble(statKey, 1.0));
                    }
                } else {
                    // Default: tutti i bonus a 1.0 se non specificati
                    statBonusMap.put("forza", 1.0);
                    statBonusMap.put("agilita", 1.0);
                    statBonusMap.put("vitalita", 1.0);
                    statBonusMap.put("intelligenza", 1.0);
                    statBonusMap.put("percezione", 1.0);
                }

                PlayerClass playerClass = new PlayerClass(
                    id,
                    displayName,
                    iconMaterial,
                    colorGradient,
                    description,
                    statBonusMap,
                    passiveText,
                    focusRegenBonus
                );

                classesById.put(id, playerClass);
                plugin.getLogger().info("Classe caricata: " + displayName);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Errore nel caricamento della classe '" + key + "'", e);
            }
        }

        plugin.getLogger().info("Caricate " + classesById.size() + " classi.");
    }

    /**
     * Ottiene una classe per ID
     */
    public PlayerClass getClassById(String id) {
        return classesById.get(id);
    }

    /**
     * Ottiene tutte le classi caricate
     */
    public Collection<PlayerClass> getAllClasses() {
        return Collections.unmodifiableCollection(classesById.values());
    }

    /**
     * Controlla se una classe esiste
     */
    public boolean hasClass(String id) {
        return classesById.containsKey(id);
    }

    /**
     * Ottiene l'ID del rango richiesto per sbloccare la selezione delle classi
     */
    public String getUnlockRankId() {
        return unlockRankId;
    }

    /**
     * Verifica se un giocatore ha il rango necessario per sbloccare le classi
     * (La logica effettiva di controllo del rango è in RankManager)
     */
    public boolean isUnlockRankReached(com.example.plugin.sistema.models.Rank currentRank) {
        if (currentRank == null) return false;
        
        // Confronta il livello minimo del rank corrente con quello richiesto
        // Oppure confronta gli ID in ordine (semplificato: assumiamo che i rank siano ordinati)
        int currentMinLevel = currentRank.getMinLevel();
        
        PlayerClass dummyClass = getClassById("guerriero"); // Usa una classe qualsiasi per ottenere il rank di sblocco
        if (dummyClass == null) return false; // Fallback
        
        // In realtà dobbiamo confrontare i rank, non le classi
        // Usiamo RankManager per questo
        RankManager rankManager = plugin.getRankManager();
        com.example.plugin.sistema.models.Rank unlockRank = rankManager.getRankById(unlockRankId);
        
        if (unlockRank == null) {
            // Se il rank di sblocco non esiste, usiamo un default di livello 25
            return currentMinLevel >= 25;
        }
        
        return currentMinLevel >= unlockRank.getMinLevel();
    }
}
