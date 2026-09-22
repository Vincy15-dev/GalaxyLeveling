package com.example.plugin.sistema.models;

/**
 * Traccia i progressi di un giocatore in una specifica missione.
 */
public class QuestProgress {

    private final Quest quest;
    private int progress;
    private boolean completed;
    private long lastCompletedEpoch; // Timestamp ultimo completamento (per repeatable)
    private long acceptedEpoch; // Timestamp accettazione (per tracking)

    public QuestProgress(Quest quest) {
        this.quest = quest;
        this.progress = 0;
        this.completed = false;
        this.lastCompletedEpoch = 0;
        this.acceptedEpoch = System.currentTimeMillis();
    }

    public Quest getQuest() {
        return quest;
    }

    public int getProgress() {
        return progress;
    }

    /**
     * Imposta il progresso corrente.
     */
    public void setProgress(int progress) {
        if (progress < 0) progress = 0;
        if (progress > quest.getAmount()) progress = quest.getAmount();
        this.progress = progress;
        
        // Auto-completa se raggiunto l'obiettivo
        if (progress >= quest.getAmount() && !completed) {
            this.completed = true;
            this.lastCompletedEpoch = System.currentTimeMillis();
        }
    }

    /**
     * Avanza il progresso di una quantità data.
     * @param amount Quantità da aggiungere.
     * @return true se la missione è stata completata con questo avanzamento.
     */
    public boolean advance(int amount) {
        if (completed && !quest.isRepeatable()) {
            return false; // Già completata e non ripetibile
        }
        
        int newProgress = progress + amount;
        if (newProgress > quest.getAmount()) {
            newProgress = quest.getAmount();
        }
        
        this.progress = newProgress;
        
        if (newProgress >= quest.getAmount() && !completed) {
            this.completed = true;
            this.lastCompletedEpoch = System.currentTimeMillis();
            return true;
        }
        
        return false;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            this.lastCompletedEpoch = System.currentTimeMillis();
        }
    }

    public long getLastCompletedEpoch() {
        return lastCompletedEpoch;
    }

    public void setLastCompletedEpoch(long epoch) {
        this.lastCompletedEpoch = epoch;
    }

    public long getAcceptedEpoch() {
        return acceptedEpoch;
    }

    public void setAcceptedEpoch(long acceptedEpoch) {
        this.acceptedEpoch = acceptedEpoch;
    }

    /**
     * Resetta i progressi per una missione ripetibile (daily/weekly).
     */
    public void resetForRepeatable() {
        if (quest.isRepeatable()) {
            this.progress = 0;
            this.completed = false;
            // Non resettiamo lastCompletedEpoch per evitare doppi claim
        }
    }

    /**
     * Controlla se la missione può essere claimata di nuovo (per repeatable).
     */
    public boolean canBeClaimedAgain() {
        if (!quest.isRepeatable()) return false;
        return completed && lastCompletedEpoch > 0;
    }

    /**
     * Percentuale di completamento (0.0 - 1.0).
     */
    public double getCompletionRatio() {
        if (quest.getAmount() <= 0) return 0.0;
        return (double) progress / quest.getAmount();
    }
}
