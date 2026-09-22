package com.galaxyleveling.model;

import org.bukkit.Material;
import java.util.List;

public class Quest {
    private final String id;
    private final String displayName;
    private final QuestCategory category;
    private final QuestType type;
    private final String target;
    private final int amount;
    private final Material iconMaterial;
    private final int minLevel;
    private final boolean oneTime;
    private final String requiresClass;
    private final List<String> prerequisites;
    
    private final int xpReward;
    private final int statPointsReward;
    private final List<String> commandsReward;

    public Quest(String id, String displayName, QuestCategory category, QuestType type, 
                 String target, int amount, Material iconMaterial, int minLevel, 
                 boolean oneTime, String requiresClass, List<String> prerequisites,
                 int xpReward, int statPointsReward, List<String> commandsReward) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.iconMaterial = iconMaterial;
        this.minLevel = minLevel;
        this.oneTime = oneTime;
        this.requiresClass = requiresClass;
        this.prerequisites = prerequisites;
        this.xpReward = xpReward;
        this.statPointsReward = statPointsReward;
        this.commandsReward = commandsReward;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public QuestCategory getCategory() { return category; }
    public QuestType getType() { return type; }
    public String getTarget() { return target; }
    public int getAmount() { return amount; }
    public Material getIconMaterial() { return iconMaterial; }
    public int getMinLevel() { return minLevel; }
    public boolean isOneTime() { return oneTime; }
    public String getRequiresClass() { return requiresClass; }
    public List<String> getPrerequisites() { return prerequisites; }
    public int getXpReward() { return xpReward; }
    public int getStatPointsReward() { return statPointsReward; }
    public List<String> getCommandsReward() { return commandsReward; }
}
