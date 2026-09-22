package com.galaxyleveling.model;

public class Rank {
    private final String id;
    private final String displayName;
    private final String colorHex;
    private final int minLevel;
    private final int maxLevel;
    private final String requiresTrial;

    public Rank(String id, String displayName, String colorHex, int minLevel, int maxLevel, String requiresTrial) {
        this.id = id;
        this.displayName = displayName;
        this.colorHex = colorHex;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.requiresTrial = requiresTrial;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getColorHex() { return colorHex; }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public String getRequiresTrial() { return requiresTrial; }
}
