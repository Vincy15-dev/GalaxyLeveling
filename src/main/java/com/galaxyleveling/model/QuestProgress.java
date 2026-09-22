package com.galaxyleveling.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Tiene traccia del progresso di una missione per un giocatore.
 */
public class QuestProgress {

    private final String questId;
    private int currentProgress; // Progresso attuale (semplificato per obiettivi singoli)
    private boolean completed;
    private long lastCompletedEpoch; // Per cooldown/one-time check
    private long acceptedEpoch; // Quando è stata accettata
    private final Map<String, Integer> objectiveProgress; // Mappa obiettivo -> progresso

    public QuestProgress(String questId) {
        this.questId = questId;
        this.currentProgress = 0;
        this.completed = false;
        this.lastCompletedEpoch = 0;
        this.acceptedEpoch = System.currentTimeMillis();
        this.objectiveProgress = new HashMap<>();
    }

    public String getQuestId() { return questId; }
    public int getCurrentProgress() { return currentProgress; }
    public boolean isCompleted() { return completed; }
    public long getLastCompletedEpoch() { return lastCompletedEpoch; }
    public long getAcceptedEpoch() { return acceptedEpoch; }
    public Map<String, Integer> getObjectiveProgress() { return objectiveProgress; }

    public void setProgress(int progress) {
        this.currentProgress = progress;
    }

    public void addProgress(int amount) {
        this.currentProgress += amount;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && lastCompletedEpoch == 0) {
            this.lastCompletedEpoch = System.currentTimeMillis();
        }
    }

    public void setLastCompletedEpoch(long epoch) {
        this.lastCompletedEpoch = epoch;
    }

    public void setAcceptedEpoch(long epoch) {
        this.acceptedEpoch = epoch;
    }

    /**
     * Aggiorna il progresso di un obiettivo specifico.
     */
    public void updateObjectiveProgress(String objectiveKey, int progress) {
        this.objectiveProgress.put(objectiveKey, progress);
    }

    /**
     * Ottiene il progresso di un obiettivo specifico.
     */
    public int getObjectiveProgress(String objectiveKey) {
        return this.objectiveProgress.getOrDefault(objectiveKey, 0);
    }

    /**
     * Resetta il progresso per un nuovo ciclo (daily/weekly).
     */
    public void reset() {
        this.currentProgress = 0;
        this.completed = false;
        this.objectiveProgress.clear();
        this.acceptedEpoch = System.currentTimeMillis();
    }
}
