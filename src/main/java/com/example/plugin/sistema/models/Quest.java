package com.example.plugin.sistema.models;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Tipi di obiettivo per le missioni.
 */
public enum QuestType {
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

/**
 * Categoria di una missione.
 */
public enum QuestCategory {
    DAILY,
    WEEKLY,
    STORY,
    TRIAL,
    CLASS_SPECIFIC
}

/**
 * Rappresenta una Missione/Quest definita in quests.yml.
 */
public class Quest {

    private final String id;
    private final String displayName;
    private final QuestType type;
    private final String target; // ID del target (es. "SPIDER", "COAL_ORE", "ANY")
    private final int amount;
    private final Material iconMaterial;
    private final int minLevel;
    private final boolean oneTime;
    private final Map<String, Object> rewards; // XP, punti stat, items, commands
    private final String minRank; // Grado minimo richiesto (null se nessuno)
    private final String prerequisiteQuestId; // ID missione prerequisito (null se nessuno)
    private final String requiresClass; // ID classe richiesta (null se nessuna)
    private final boolean allowPlacedBlocks; // Per obiettivi BREAK_BLOCK, permette blocchi piazzati?
    private final QuestCategory category;

    public Quest(String id, String displayName, QuestCategory category, QuestType type, String target, int amount,
                 String iconMaterial, int minLevel, boolean oneTime, boolean allowPlacedBlocks,
                 String requiresClass, String prerequisiteQuestId, String minRank, Map<String, Object> rewards) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.type = type;
        this.target = target != null ? target : "ANY";
        this.amount = amount;
        
        // Parse material from string
        Material mat = Material.BOOK;
        try {
            mat = Material.valueOf(iconMaterial.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Usa materiale default
        }
        this.iconMaterial = mat;
        
        this.minLevel = minLevel;
        this.oneTime = oneTime;
        this.rewards = rewards != null ? rewards : new HashMap<>();
        this.minRank = minRank;
        this.prerequisiteQuestId = prerequisiteQuestId;
        this.requiresClass = requiresClass;
        this.allowPlacedBlocks = allowPlacedBlocks;
    }

    /**
     * Controlla se la quest è ripetibile (DAILY o WEEKLY).
     */
    public boolean isRepeatable() {
        return category == QuestCategory.DAILY || category == QuestCategory.WEEKLY;
    }
    
    /**
     * Ottiene la lista di items come reward (formato configurazionale).
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getItemRewards() {
        Object items = rewards.get("items");
        if (items instanceof List) {
            return (List<Map<String, Object>>) items;
        }
        return new ArrayList<>();
    }
    
    /**
     * Ottiene la lista di comandi come reward.
     */
    @SuppressWarnings("unchecked")
    public List<String> getCommandRewards() {
        Object commands = rewards.get("commands");
        if (commands instanceof List) {
            return (List<String>) commands;
        }
        return new ArrayList<>();
    }
    
    // Getter pubblici
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public QuestType getType() {
        return type;
    }
    
    public String getTarget() {
        return target;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public Material getIconMaterial() {
        return iconMaterial;
    }
    
    public int getMinLevel() {
        return minLevel;
    }
    
    public boolean isOneTime() {
        return oneTime;
    }
    
    public Map<String, Object> getRewards() {
        return Collections.unmodifiableMap(rewards);
    }
    
    public int getXpReward() {
        return (int) rewards.getOrDefault("xp", 0);
    }
    
    public int getStatPointsReward() {
        return (int) rewards.getOrDefault("stat-points", 0);
    }
    
    public String getMinRank() {
        return minRank;
    }
    
    public String getPrerequisiteQuestId() {
        return prerequisiteQuestId;
    }
    
    public String getRequiresClass() {
        return requiresClass;
    }
    
    public boolean isAllowPlacedBlocks() {
        return allowPlacedBlocks;
    }
    
    public QuestCategory getCategory() {
        return category;
    }
}
