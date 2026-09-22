package com.example.plugin.sistema.services;

import com.example.plugin.sistema.SistemaPlugin;
import com.example.plugin.sistema.models.Quest;
import com.example.plugin.sistema.models.QuestProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

/**
 * Gestisce il caricamento e l'accesso alle definizioni delle missioni da quests.yml.
 */
public class QuestManager {

    private final SistemaPlugin plugin;
    private final Map<String, Quest> questsById = new HashMap<>();
    
    // Pool separati per categoria
    private final List<Quest> dailyQuests = new ArrayList<>();
    private final List<Quest> weeklyQuests = new ArrayList<>();
    private final List<Quest> storyQuests = new ArrayList<>();
    private final List<Quest> trialQuests = new ArrayList<>();
    private final List<Quest> classSpecificQuests = new ArrayList<>();

    public QuestManager(SistemaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Carica tutte le missioni da quests.yml
     */
    public void loadQuests() {
        questsById.clear();
        dailyQuests.clear();
        weeklyQuests.clear();
        storyQuests.clear();
        trialQuests.clear();
        classSpecificQuests.clear();

        FileConfiguration config = plugin.getQuestsConfig();
        if (config == null) {
            plugin.getLogger().warning("quests.yml non trovato o nullo!");
            return;
        }

        // Carica per categoria
        loadQuestCategory(config, "daily", dailyQuests);
        loadQuestCategory(config, "weekly", weeklyQuests);
        loadQuestCategory(config, "story", storyQuests);
        loadQuestCategory(config, "trial", trialQuests);
        loadQuestCategory(config, "class-specific", classSpecificQuests);

        plugin.getLogger().info("Caricate " + questsById.size() + " missioni totali.");
        plugin.getLogger().info("  Daily: " + dailyQuests.size());
        plugin.getLogger().info("  Weekly: " + weeklyQuests.size());
        plugin.getLogger().info("  Story: " + storyQuests.size());
        plugin.getLogger().info("  Trial: " + trialQuests.size());
        plugin.getLogger().info("  Class-Specific: " + classSpecificQuests.size());
    }

    @SuppressWarnings("unchecked")
    private void loadQuestCategory(FileConfiguration config, String category, List<Quest> questList) {
        ConfigurationSection categorySection = config.getConfigurationSection(category);
        if (categorySection == null) {
            plugin.getLogger().fine("Nessuna missione trovata per la categoria: " + category);
            return;
        }

        for (String key : categorySection.getKeys(false)) {
            try {
                ConfigurationSection questSec = categorySection.getConfigurationSection(key);
                if (questSec == null) continue;

                String id = key;
                String displayName = questSec.getString("display-name", id);
                String typeStr = questSec.getString("type", "KILL_MOB");
                String target = questSec.getString("target", "ANY");
                int amount = questSec.getInt("amount", 1);
                String iconMaterial = questSec.getString("icon", "BOOK");
                int minLevel = questSec.getInt("min-level", 1);
                boolean oneTime = questSec.getBoolean("one-time", false);
                boolean allowPlacedBlocks = questSec.getBoolean("allow-placed-blocks", false);
                
                String requiresClass = questSec.getString("requires-class"); // Nullable
                String prerequisiteQuestId = questSec.getString("prerequisite-quest-id"); // Nullable
                String minRank = questSec.getString("min-rank"); // Nullable

                // Carica rewards
                ConfigurationSection rewardsSec = questSec.getConfigurationSection("rewards");
                Map<String, Object> rewards = new HashMap<>();
                if (rewardsSec != null) {
                    rewards.put("xp", rewardsSec.getInt("xp", 0));
                    rewards.put("stat-points", rewardsSec.getInt("stat-points", 0));
                    rewards.put("items", rewardsSec.getList("items", new ArrayList<>()));
                    rewards.put("commands", rewardsSec.getStringList("commands"));
                }

                Quest.QuestType type;
                try {
                    type = Quest.QuestType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Tipo di missione non valido '" + typeStr + "' per " + id + ", usando KILL_MOB");
                    type = Quest.QuestType.KILL_MOB;
                }

                Quest.Category questCategory;
                switch (category.toLowerCase()) {
                    case "daily": questCategory = Quest.Category.DAILY; break;
                    case "weekly": questCategory = Quest.Category.WEEKLY; break;
                    case "story": questCategory = Quest.Category.STORY; break;
                    case "trial": questCategory = Quest.Category.TRIAL; break;
                    case "class-specific": questCategory = Quest.Category.CLASS_SPECIFIC; break;
                    default: questCategory = Quest.Category.DAILY;
                }

                Quest quest = new Quest(
                    id,
                    displayName,
                    questCategory,
                    type,
                    target,
                    amount,
                    iconMaterial,
                    minLevel,
                    oneTime,
                    allowsPlacedBlocks,
                    requiresClass,
                    prerequisiteQuestId,
                    minRank,
                    rewards
                );

                questsById.put(id, quest);
                questList.add(quest);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Errore nel caricamento della missione '" + key + "'", e);
            }
        }
    }

    /**
     * Ottiene una missione per ID
     */
    public Quest getQuestById(String id) {
        return questsById.get(id);
    }

    /**
     * Ottiene tutte le missioni caricate
     */
    public Collection<Quest> getAllQuests() {
        return Collections.unmodifiableCollection(questsById.values());
    }

    /**
     * Ottiene le missioni daily disponibili
     */
    public List<Quest> getDailyQuests() {
        return Collections.unmodifiableList(dailyQuests);
    }

    /**
     * Ottiene le missioni weekly disponibili
     */
    public List<Quest> getWeeklyQuests() {
        return Collections.unmodifiableList(weeklyQuests);
    }

    /**
     * Ottiene le missioni story disponibili
     */
    public List<Quest> getStoryQuests() {
        return Collections.unmodifiableList(storyQuests);
    }

    /**
     * Ottiene le missioni trial disponibili
     */
    public List<Quest> getTrialQuests() {
        return Collections.unmodifiableList(trialQuests);
    }

    /**
     * Ottiene le missioni class-specific disponibili
     */
    public List<Quest> getClassSpecificQuests() {
        return Collections.unmodifiableList(classSpecificQuests);
    }

    /**
     * Seleziona un pool deterministico di missioni daily/settimanali basato sul seed
     */
    public List<Quest> selectDeterministicPool(List<Quest> pool, int seed, int count) {
        Random rng = new Random(seed);
        List<Quest> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, rng);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    /**
     * Controlla se una missione esiste
     */
    public boolean hasQuest(String id) {
        return questsById.containsKey(id);
    }
}
