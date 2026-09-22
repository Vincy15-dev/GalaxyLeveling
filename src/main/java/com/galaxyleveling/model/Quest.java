package com.galaxyleveling.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Rappresenta una missione con i suoi obiettivi, ricompense e requisiti.
 */
public class Quest {

    private final String id;
    private final String displayName;
    private final Category category;
    private final ObjectiveType type;
    private final String target; // Materiale, EntityType, o altro target
    private final int amount;
    private final Material icon;
    private final int minLevel;
    private final boolean oneTime;
    private final String requiresClass; // Nullable
    private final QuestRewards rewards;
    private final QuestRequirements requirements;
    private final String prerequisiteQuestId; // Nullable
    private final boolean allowPlacedBlocks; // Per BREAK_BLOCK quests
    private final String description;

    public enum Category {
        DAILY,
        WEEKLY,
        STORY,
        TRIAL,
        CLASS_SPECIFIC
    }

    public enum ObjectiveType {
        KILL_MOB,
        BREAK_BLOCK,
        PLACE_BLOCK,
        CRAFT_ITEM,
        FISH_ITEM,
        TAME_ANIMAL,
        BREED_ANIMAL,
        ENCHANT_ITEM,
        BREW_POTION,
        DEAL_DAMAGE,
        TRAVEL_DISTANCE,
        REACH_LEVEL
    }

    public Quest(String id, String displayName, Category category, ObjectiveType type, String target,
                 int amount, Material icon, int minLevel, boolean oneTime, String requiresClass,
                 QuestRewards rewards, QuestRequirements requirements, String prerequisiteQuestId,
                 boolean allowPlacedBlocks, String description) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.icon = icon;
        this.minLevel = minLevel;
        this.oneTime = oneTime;
        this.requiresClass = requiresClass;
        this.rewards = rewards;
        this.requirements = requirements;
        this.prerequisiteQuestId = prerequisiteQuestId;
        this.allowPlacedBlocks = allowPlacedBlocks;
        this.description = description;
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Category getCategory() { return category; }
    public ObjectiveType getType() { return type; }
    public String getTarget() { return target; }
    public int getAmount() { return amount; }
    public Material getIcon() { return icon; }
    public int getMinLevel() { return minLevel; }
    public boolean isOneTime() { return oneTime; }
    public String getRequiresClass() { return requiresClass; }
    public QuestRewards getRewards() { return rewards; }
    public QuestRequirements getRequirements() { return requirements; }
    public String getPrerequisiteQuestId() { return prerequisiteQuestId; }
    public boolean isAllowPlacedBlocks() { return allowPlacedBlocks; }
    public String getDescription() { return description; }

    /**
     * Contiene le ricompense di una missione.
     */
    public static class QuestRewards {
        private final int xp;
        private final int statPoints;
        private final List<RewardItem> items;
        private final List<String> commands;

        public QuestRewards(int xp, int statPoints, List<RewardItem> items, List<String> commands) {
            this.xp = xp;
            this.statPoints = statPoints;
            this.items = items != null ? items : new ArrayList<>();
            this.commands = commands != null ? commands : new ArrayList<>();
        }

        public int getXp() { return xp; }
        public int getStatPoints() { return statPoints; }
        public List<RewardItem> getItems() { return items; }
        public List<String> getCommands() { return commands; }
    }

    /**
     * Item di ricompensa.
     */
    public static class RewardItem {
        private final Material material;
        private final int amount;
        private final boolean enchanted;

        public RewardItem(Material material, int amount, boolean enchanted) {
            this.material = material;
            this.amount = amount;
            this.enchanted = enchanted;
        }

        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
        public boolean isEnchanted() { return enchanted; }
    }

    /**
     * Requisiti per accettare/completare la missione.
     */
    public static class QuestRequirements {
        private final String minRank; // Nullable
        private final String requiresClass; // Già nel costruttore principale

        public QuestRequirements(String minRank) {
            this.minRank = minRank;
        }

        public String getMinRank() { return minRank; }
    }

    /**
     * Carica una missione da una ConfigurationSection.
     * @param id ID della missione.
     * @param section Sezione YAML.
     * @return Quest caricata o null se errori.
     */
    public static Quest fromConfig(String id, ConfigurationSection section) {
        try {
            String displayName = section.getString("display-name", id);
            String categoryStr = section.getString("category", "DAILY");
            Category category = Category.valueOf(categoryStr.toUpperCase());
            
            String typeStr = section.getString("type", "KILL_MOB");
            ObjectiveType type = ObjectiveType.valueOf(typeStr.toUpperCase());
            
            String target = section.getString("target", "ANY");
            int amount = section.getInt("amount", 1);
            
            String iconStr = section.getString("icon", "PAPER");
            Material icon = Material.matchMaterial(iconStr);
            if (icon == null) icon = Material.PAPER;
            
            int minLevel = section.getInt("min-level", 1);
            boolean oneTime = section.getBoolean("one-time", false);
            String requiresClass = section.getString("requires-class"); // Nullable
            
            // Ricompense
            ConfigurationSection rewardsSec = section.getConfigurationSection("rewards");
            int xp = 0, statPoints = 0;
            List<RewardItem> items = new ArrayList<>();
            List<String> commands = new ArrayList<>();
            if (rewardsSec != null) {
                xp = rewardsSec.getInt("xp", 0);
                statPoints = rewardsSec.getInt("stat-points", 0);
                if (rewardsSec.contains("items")) {
                    for (Object itemObj : rewardsSec.getList("items")) {
                        if (itemObj instanceof ConfigurationSection itemSec) {
                            String matStr = itemSec.getString("material", "STONE");
                            Material mat = Material.matchMaterial(matStr);
                            if (mat != null) {
                                int itemAmount = itemSec.getInt("amount", 1);
                                boolean enchanted = itemSec.getBoolean("enchanted", false);
                                items.add(new RewardItem(mat, itemAmount, enchanted));
                            }
                        }
                    }
                }
                commands = rewardsSec.getStringList("commands");
            }
            QuestRewards rewards = new QuestRewards(xp, statPoints, items, commands);
            
            // Requisiti
            String minRank = section.getString("requirements.min-rank");
            QuestRequirements requirements = new QuestRequirements(minRank);
            
            String prerequisiteQuestId = section.getString("requirements.prerequisite-quest-id");
            boolean allowPlacedBlocks = section.getBoolean("allow-placed-blocks", false);
            String description = section.getString("description", "");
            
            return new Quest(id, displayName, category, type, target, amount, icon, minLevel,
                    oneTime, requiresClass, rewards, requirements, prerequisiteQuestId,
                    allowPlacedBlocks, description);
                    
        } catch (Exception e) {
            // Logga errore ma non crashare
            System.err.println("[GalaxyLeveling] Errore nel caricamento missione: " + id + " - " + e.getMessage());
            return null;
        }
    }
}
