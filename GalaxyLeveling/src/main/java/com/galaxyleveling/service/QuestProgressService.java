package com.galaxyleveling.service;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.*;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestProgressService {
    private final GalaxyLeveling plugin;
    private final Map<UUID, Map<String, QuestProgress>> playerQuests = new ConcurrentHashMap<>();

    public QuestProgressService(GalaxyLeveling plugin) {
        this.plugin = plugin;
    }

    public void loadPlayerQuests(Player player) {
        // Caricamento da storage (implementazione futura)
        playerQuests.putIfAbsent(player.getUniqueId(), new ConcurrentHashMap<>());
    }

    public List<QuestProgress> getActiveQuests(Player player) {
        Map<String, QuestProgress> quests = playerQuests.get(player.getUniqueId());
        if (quests == null) return new ArrayList<>();
        
        List<QuestProgress> active = new ArrayList<>();
        for (QuestProgress progress : quests.values()) {
            if (!progress.isCompleted()) {
                active.add(progress);
            }
        }
        return active;
    }

    public void advance(Player player, String questId, int amount) {
        Map<String, QuestProgress> quests = playerQuests.get(player.getUniqueId());
        if (quests == null) return;

        QuestProgress progress = quests.get(questId);
        if (progress == null || progress.isCompleted()) return;

        int newProgress = progress.getProgress() + amount;
        int target = progress.getQuest().getAmount();
        
        if (newProgress >= target) {
            progress.setProgress(target);
            progress.setCompleted(true);
            completeQuest(player, progress);
        } else {
            progress.setProgress(newProgress);
        }
    }

    private void completeQuest(Player player, QuestProgress progress) {
        Quest quest = progress.getQuest();
        
        // Dai reward
        if (quest.getXpReward() > 0) {
            plugin.getXpService().grantXp(player, quest.getXpReward(), "quest");
        }
        
        // Rimuovi dalla lista attiva dopo un delay o tienila per claim manuale
        // Implementazione semplificata: rimuovi subito
        playerQuests.get(player.getUniqueId()).remove(quest.getId());
        
        player.sendMessage(com.galaxyleveling.util.ColorUtils.colorize(
            "&aMissione completata: &e" + quest.getDisplayName()
        ));
    }

    public void acceptQuest(Player player, String questId) {
        Quest quest = plugin.getQuestManager().getQuestById(questId);
        if (quest == null) return;

        QuestProgress progress = new QuestProgress(quest);
        progress.setAcceptedEpoch(System.currentTimeMillis());
        playerQuests.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                    .put(questId.toLowerCase(), progress);
    }
}
