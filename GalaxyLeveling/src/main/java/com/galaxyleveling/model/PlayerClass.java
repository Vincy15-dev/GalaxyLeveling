package com.galaxyleveling.model;

import java.util.Map;

public class PlayerClass {
    private final String id;
    private final String displayName;
    private final Map<StatDefinition, Double> statBonuses;
    private final double focusRegenBonus;

    public PlayerClass(String id, String displayName, Map<StatDefinition, Double> statBonuses, double focusRegenBonus) {
        this.id = id;
        this.displayName = displayName;
        this.statBonuses = statBonuses;
        this.focusRegenBonus = focusRegenBonus;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Map<StatDefinition, Double> getStatBonuses() { return statBonuses; }
    
    public double getStatMultiplier(StatDefinition stat) {
        return statBonuses.getOrDefault(stat, 1.0);
    }
    
    public double getFocusRegenBonus() { return focusRegenBonus; }
}
