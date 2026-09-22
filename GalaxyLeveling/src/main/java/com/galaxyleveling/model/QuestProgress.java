package com.galaxyleveling.model;

public class QuestProgress {
    private final Quest quest;
    private int progress;
    private boolean completed;
    private long lastCompletedEpoch;
    private long acceptedEpoch;

    public QuestProgress(Quest quest) {
        this.quest = quest;
        this.progress = 0;
        this.completed = false;
        this.lastCompletedEpoch = 0;
        this.acceptedEpoch = 0;
    }

    public Quest getQuest() { return quest; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public long getLastCompletedEpoch() { return lastCompletedEpoch; }
    public void setLastCompletedEpoch(long lastCompletedEpoch) { this.lastCompletedEpoch = lastCompletedEpoch; }
    public long getAcceptedEpoch() { return acceptedEpoch; }
    public void setAcceptedEpoch(long acceptedEpoch) { this.acceptedEpoch = acceptedEpoch; }
}
