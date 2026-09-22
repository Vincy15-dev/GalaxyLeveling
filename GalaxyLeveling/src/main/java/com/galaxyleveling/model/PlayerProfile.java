package com.galaxyleveling.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerProfile {
    private final UUID uuid;
    private long totalXp;
    private int level;
    private int unspentStatPoints;
    private String chosenClassId;
    private String currentRankId;
    private double focusAmount;
    private final Map<StatDefinition, Integer> stats = new ConcurrentHashMap<>();
    private int pendingLevelUps;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.totalXp = 0;
        this.level = 1;
        this.unspentStatPoints = 0;
        this.chosenClassId = null;
        this.currentRankId = "e";
        this.focusAmount = 0.0;
        this.pendingLevelUps = 0;
        
        for (StatDefinition stat : StatDefinition.values()) {
            stats.put(stat, 0);
        }
    }

    public static PlayerProfile createNew(UUID uuid) {
        return new PlayerProfile(uuid);
    }

    public UUID getPlayerUuid() { return uuid; }
    public long getTotalXp() { return totalXp; }
    public void setTotalXp(long totalXp) { this.totalXp = totalXp; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getUnspentStatPoints() { return unspentStatPoints; }
    public void setUnspentStatPoints(int unspentStatPoints) { this.unspentStatPoints = unspentStatPoints; }
    public String getChosenClassId() { return chosenClassId; }
    public void setChosenClassId(String chosenClassId) { this.chosenClassId = chosenClassId; }
    public String getCurrentRankId() { return currentRankId; }
    public void setCurrentRankId(String currentRankId) { this.currentRankId = currentRankId; }
    public double getFocusAmount() { return focusAmount; }
    public void setFocusedAmount(double focusAmount) { this.focusAmount = focusAmount; }
    public int getPendingLevelUps() { return pendingLevelUps; }
    public void setPendingLevelUps(int pendingLevelUps) { this.pendingLevelUps = pendingLevelUps; }
    
    public Map<StatDefinition, Integer> getStatValues() { return stats; }
    public int getStatValue(StatDefinition stat) { return stats.getOrDefault(stat, 0); }
    public void setStatValue(StatDefinition stat, int value) { stats.put(stat, value); }
    
    public void resetStats() {
        for (StatDefinition stat : StatDefinition.values()) {
            stats.put(stat, 0);
        }
    }
}
