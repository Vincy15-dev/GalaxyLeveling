package com.galaxyleveling.model;

public enum StatDefinition {
    FORZA("Forza", 0.5, 0.1),
    AGILITA("Agilità", 0.001, 0.05),
    VITALITA("Vitalità", 2.0, 0.5),
    INTELLIGENZA("Intelligenza", 0.02, 0.1),
    PERCEZIONE("Percezione", 0.01, 0.01);

    private final String displayName;
    private final double baseValue;
    private final double incrementPerLevel;

    StatDefinition(String displayName, double baseValue, double incrementPerLevel) {
        this.displayName = displayName;
        this.baseValue = baseValue;
        this.incrementPerLevel = incrementPerLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseValue() {
        return baseValue;
    }

    public double getIncrementPerLevel() {
        return incrementPerLevel;
    }

    public int getCostForNextLevel(int currentLevel) {
        return 1 + (currentLevel * 1);
    }

    public static StatDefinition fromId(String id) {
        try {
            return valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
