package com.galaxyleveling.service;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class QuestManager {
    private final GalaxyLeveling plugin;
    private final Map<String, Quest> quests = new HashMap<>();

    public QuestManager(GalaxyLeveling plugin) {
        this.plugin = plugin;
        loadQuests();
    }

    private void loadQuests() {
        File questFile = new File(plugin.getDataFolder(), "quests.yml");
        if (!questFile.exists()) {
            plugin.saveResource("quests.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(questFile);

        // Carica Daily
        loadQuestCategory(config, "daily", QuestCategory.DAILY);
        // Carica Weekly
        loadQuestCategory(config, "weekly", QuestCategory.WEEKLY);
        // Carica Story
        loadQuestCategory(config, "story", QuestCategory.STORY);
        // Carica Trial
        loadQuestCategory(config, "trial", QuestCategory.TRIAL);
        // Carica Class-specific
        loadQuestCategory(config, "class-specific", QuestCategory.CLASS_SPECIFIC);

        plugin.getLogger().info("Caricate " + quests.size() + " missioni.");
    }

    private void loadQuestCategory(FileConfiguration config, String categoryKey, QuestCategory category) {
        ConfigurationSection catSection = config.getConfigurationSection(categoryKey);
        if (catSection == null) return;

        for (String key : catSection.getKeys(false)) {
            try {
                ConfigurationSection questSec = catSection.getConfigurationSection(key);
                if (questSec == null) continue;

                String displayName = questSec.getString("display-name", key);
                QuestType type = QuestType.valueOf(questSec.getString("type", "KILL_MOB"));
                String target = questSec.getString("target", "ANY");
                int amount = questSec.getInt("amount", 1);
                Material icon = Material.valueOf(questSec.getString("icon", "PAPER"));
                int minLevel = questSec.getInt("min-level", 1);
                boolean oneTime = questSec.getBoolean("one-time", false);
                String requiresClass = questSec.getString("requires-class");
                
                List<String> prerequisites = questSec.getStringList("prerequisites");
                int xpReward = questSec.getInt("rewards.xp", 0);
                int statPointsReward = questSec.getInt("rewards.stat-points", 0);
                List<String> commandsReward = questSec.getStringList("rewards.commands");

                Quest quest = new Quest(key, displayName, category, type, target, amount, icon, minLevel, oneTime, requiresClass, prerequisites, xpReward, statPointsReward, commandsReward);
                quests.put(key.toLowerCase(), quest);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Errore nel caricamento missione: " + key, e);
            }
        }
    }

    public Quest getQuestById(String id) {
        return quests.get(id.toLowerCase());
    }

    public Collection<Quest> getAllQuests() {
        return quests.values();
    }

    public List<Quest> getQuestsByCategory(QuestCategory category) {
        List<Quest> result = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (quest.getCategory() == category) {
                result.add(quest);
            }
        }
        return result;
    }
}
