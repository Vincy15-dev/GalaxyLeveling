package com.galaxyleveling.service;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerClass;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ClassManager {
    private final GalaxyLeveling plugin;
    private final Map<String, PlayerClass> classes = new HashMap<>();
    private String unlockRankId = "c";

    public ClassManager(GalaxyLeveling plugin) {
        this.plugin = plugin;
        loadClasses();
    }

    private void loadClasses() {
        File classFile = new File(plugin.getDataFolder(), "classes.yml");
        if (!classFile.exists()) {
            plugin.saveResource("classes.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(classFile);
        
        unlockRankId = config.getString("unlock-rank", "c");

        ConfigurationSection classesSection = config.getConfigurationSection("classes");
        if (classesSection == null) {
            plugin.getLogger().warning("Nessuna classe trovata in classes.yml");
            return;
        }

        for (String key : classesSection.getKeys(false)) {
            try {
                ConfigurationSection classSec = classesSection.getConfigurationSection(key);
                if (classSec == null) continue;

                String displayName = classSec.getString("display-name", key);
                
                // Carica stat bonuses
                Map<com.galaxyleveling.model.StatDefinition, Double> statBonuses = new HashMap<>();
                ConfigurationSection bonusSec = classSec.getConfigurationSection("stat-bonus");
                if (bonusSec != null) {
                    for (String statKey : bonusSec.getKeys(false)) {
                        com.galaxyleveling.model.StatDefinition stat = com.galaxyleveling.model.StatDefinition.fromId(statKey);
                        if (stat != null) {
                            statBonuses.put(stat, bonusSec.getDouble(statKey, 1.0));
                        }
                    }
                }

                double focusRegenBonus = classSec.getDouble("focus-regen-bonus", 0.0);

                PlayerClass pClass = new PlayerClass(key, displayName, statBonuses, focusRegenBonus);
                classes.put(key.toLowerCase(), pClass);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Errore nel caricamento della classe: " + key, e);
            }
        }

        plugin.getLogger().info("Caricate " + classes.size() + " classi.");
    }

    public PlayerClass getClassById(String id) {
        return classes.get(id.toLowerCase());
    }

    public String getUnlockRankId() {
        return unlockRankId;
    }

    public boolean isClassUnlocked(String rankId) {
        // Logica semplificata: controlla se il rank è >= unlock-rank
        // Implementazione completa richiederebbe confronto ordinato dei rank
        return true; // Placeholder
    }
    
    public Map<String, PlayerClass> getAllClasses() {
        return new HashMap<>(classes);
    }
}
