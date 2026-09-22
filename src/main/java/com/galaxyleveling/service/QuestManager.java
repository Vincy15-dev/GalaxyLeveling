package com.galaxyleveling.service;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.Quest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Gestisce il caricamento, salvataggio e accesso alle missioni dal file quests.yml.
 */
public class QuestManager {

    private final GalaxyLeveling plugin;
    private final File questFile;
    private FileConfiguration questConfig;
    private final Map<String, Quest> quests = new HashMap<>();
    private static QuestManager instance;

    public QuestManager(GalaxyLeveling plugin) {
        this.plugin = plugin;
        this.questFile = new File(plugin.getDataFolder(), "quests.yml");
        if (!questFile.exists()) {
            plugin.saveResource("quests.yml", false);
        }
        reloadQuests();
        instance = this;
    }

    /**
     * Ricarica tutte le missioni dal file YAML.
     */
    public void reloadQuests() {
        this.questConfig = YamlConfiguration.loadConfiguration(questFile);
        quests.clear();

        // Carica missioni DAILY
        loadQuestsFromCategory("daily", Quest.Category.DAILY);
        
        // Carica missioni WEEKLY
        loadQuestsFromCategory("weekly", Quest.Category.WEEKLY);
        
        // Carica missioni STORY
        loadQuestsFromCategory("story", Quest.Category.STORY);
        
        // Carica missioni TRIAL
        loadQuestsFromCategory("trial", Quest.Category.TRIAL);
        
        // Carica missioni CLASS_SPECIFIC
        loadQuestsFromCategory("class-specific", Quest.Category.CLASS_SPECIFIC);

        plugin.getLogger().info("Caricate " + quests.size() + " missioni.");
    }

    private void loadQuestsFromCategory(String categoryKey, Quest.Category category) {
        ConfigurationSection categorySection = questConfig.getConfigurationSection(categoryKey);
        if (categorySection == null) return;

        for (String questId : categorySection.getKeys(false)) {
            ConfigurationSection questSection = categorySection.getConfigurationSection(questId);
            if (questSection == null) continue;

            Quest quest = Quest.fromConfig(questId, questSection);
            if (quest != null) {
                quests.put(questId.toLowerCase(), quest);
            } else {
                plugin.getLogger().warning("Saltata missione invalida: " + questId);
            }
        }
    }

    /**
     * Ottiene una missione per ID.
     */
    public Quest getQuest(String id) {
        return quests.get(id.toLowerCase());
    }

    /**
     * Ottiene tutte le missioni caricate.
     */
    public Collection<Quest> getAllQuests() {
        return new ArrayList<>(quests.values());
    }

    /**
     * Ottiene missioni per categoria.
     */
    public List<Quest> getQuestsByCategory(Quest.Category category) {
        List<Quest> result = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (quest.getCategory() == category) {
                result.add(quest);
            }
        }
        return result;
    }

    /**
     * Ottiene missioni disponibili per un giocatore.
     */
    public List<Quest> getAvailableQuests(int playerLevel, String playerRank, String playerClass,
                                          Set<String> completedQuestIds, Set<String> acceptedQuestIds) {
        List<Quest> available = new ArrayList<>();
        for (Quest quest : quests.values()) {
            // Controlla livello
            if (playerLevel < quest.getMinLevel()) continue;
            
            // Già completata (one-time o story/trial)
            if (quest.isOneTime() && completedQuestIds.contains(quest.getId())) continue;
            
            // Già accettata
            if (acceptedQuestIds.contains(quest.getId())) continue;
            
            // Requisito grado
            if (quest.getRequirements().getMinRank() != null) {
                if (!playerRank.equalsIgnoreCase(quest.getRequirements().getMinRank())) {
                    // Controlla se il grado del giocatore è >= richiesto (logica da espandere con RankManager)
                    // Per ora controllo semplice di uguaglianza
                }
            }
            
            // Requisito classe
            if (quest.getRequiresClass() != null) {
                if (playerClass == null || !playerClass.equalsIgnoreCase(quest.getRequiresClass())) {
                    continue;
                }
            }
            
            // Prerequisiti
            if (quest.getPrerequisiteQuestId() != null) {
                if (!completedQuestIds.contains(quest.getPrerequisiteQuestId())) {
                    continue;
                }
            }
            
            available.add(quest);
        }
        return available;
    }

    public static QuestManager getInstance() {
        return instance;
    }
}
