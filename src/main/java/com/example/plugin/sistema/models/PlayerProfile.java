package com.example.plugin.sistema.models;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Profilo del giocatore che contiene tutti i dati di progressione.
 * Thread-safe per accessi concorrenti.
 */
public class PlayerProfile {

    private final UUID playerUuid;
    
    // Dati base
    private long totalXp;
    private int level;
    private int unspentStatPoints;
    
    // Statistiche (valore allocato per ciascuna stat)
    private final Map<StatDefinition, Integer> statValues;
    
    // Classe scelta (può essere null se non ancora scelta)
    private PlayerClass chosenClass;
    
    // Grado attuale
    private Rank currentRank;
    
    // Focus (mana-like resource per Intelligenza)
    private double focusAmount;
    
    // Progressi missioni: key = questId
    private final Map<String, QuestProgress> questProgressMap;
    
    // Level-up pendenti dovuti al soft-cap del rank
    private int pendingLevelUps;
    
    // Flag dirty per il salvataggio
    private boolean dirty;

    private PlayerProfile(UUID uuid) {
        this.playerUuid = uuid;
        this.totalXp = 0;
        this.level = 1;
        this.unspentStatPoints = 0;
        this.statValues = new EnumMap<>(StatDefinition.class);
        for (StatDefinition stat : StatDefinition.values()) {
            statValues.put(stat, 0);
        }
        this.chosenClass = null;
        this.currentRank = null; // Da impostare dal manager dopo il caricamento
        this.focusAmount = 0.0;
        this.questProgressMap = new ConcurrentHashMap<>();
        this.pendingLevelUps = 0;
        this.dirty = false;
    }

    /**
     * Crea un nuovo profilo vuoto per un giocatore.
     */
    public static PlayerProfile createNew(UUID uuid) {
        return new PlayerProfile(uuid);
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public long getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(long totalXp) {
        this.totalXp = totalXp;
        this.dirty = true;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        this.dirty = true;
    }

    public int getUnspentStatPoints() {
        return unspentStatPoints;
    }

    public void setUnspentStatPoints(int unspentStatPoints) {
        this.unspentStatPoints = unspentStatPoints;
        this.dirty = true;
    }

    public void addUnspentStatPoints(int amount) {
        this.unspentStatPoints += amount;
        this.dirty = true;
    }

    public Map<StatDefinition, Integer> getStatValues() {
        return Collections.unmodifiableMap(statValues);
    }

    public int getStatValue(StatDefinition stat) {
        return statValues.getOrDefault(stat, 0);
    }

    public void setStatValue(StatDefinition stat, int value) {
        if (value < 0) value = 0;
        this.statValues.put(stat, value);
        this.dirty = true;
    }

    public PlayerClass getChosenClass() {
        return chosenClass;
    }

    public void setChosenClass(PlayerClass chosenClass) {
        this.chosenClass = chosenClass;
        this.dirty = true;
    }

    public Rank getCurrentRank() {
        return currentRank;
    }

    public void setCurrentRank(Rank currentRank) {
        this.currentRank = currentRank;
        this.dirty = true;
    }

    public double getFocusAmount() {
        return focusAmount;
    }

    public void setFocusAmount(double focusAmount) {
        this.focusAmount = focusAmount;
        this.dirty = true;
    }

    public void addFocusAmount(double amount) {
        this.focusAmount += amount;
        if (this.focusAmount < 0) this.focusAmount = 0;
        this.dirty = true;
    }

    public Map<String, QuestProgress> getQuestProgressMap() {
        return Collections.unmodifiableMap(questProgressMap);
    }

    public QuestProgress getQuestProgress(String questId) {
        return questProgressMap.get(questId);
    }

    public void addQuestProgress(QuestProgress progress) {
        this.questProgressMap.put(progress.getQuest().getId(), progress);
        this.dirty = true;
    }

    public void removeQuestProgress(String questId) {
        this.questProgressMap.remove(questId);
        this.dirty = true;
    }

    public int getPendingLevelUps() {
        return pendingLevelUps;
    }

    public void setPendingLevelUps(int pendingLevelUps) {
        this.pendingLevelUps = pendingLevelUps;
        this.dirty = true;
    }

    public void addPendingLevelUps(int amount) {
        this.pendingLevelUps += amount;
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    /**
     * Controlla se una missione è già stata completata (per one-time quest).
     */
    public boolean hasCompletedQuest(String questId) {
        QuestProgress progress = questProgressMap.get(questId);
        return progress != null && progress.isCompleted();
    }

    /**
     * Ottiene o crea il progresso per una missione.
     */
    public QuestProgress getOrCreateQuestProgress(Quest quest) {
        return questProgressMap.computeIfAbsent(quest.getId(), k -> new QuestProgress(quest));
    }
}
