package com.galaxyleveling.service;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.Quest;
import com.galaxyleveling.model.QuestProgress;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servizio centrale per il tracking e avanzamento delle missioni.
 * Tutti gli obiettivi delle missioni passano attraverso advance().
 */
public class QuestProgressService implements Listener {

    private final GalaxyLeveling plugin;
    // <PlayerUUID, <QuestID, QuestProgress>>
    private final Map<UUID, Map<String, QuestProgress>> playerQuestProgress = new ConcurrentHashMap<>();
    // Cache missioni completate (per one-time check)
    private final Map<UUID, Set<String>> playerCompletedQuests = new ConcurrentHashMap<>();
    // Cache missioni accettate/in corso
    private final Map<UUID, Set<String>> playerAcceptedQuests = new ConcurrentHashMap<>();
    
    // Set per ottimizzare TRAVEL_DISTANCE: solo giocatori con obiettivo travel attivo
    private final Set<UUID> playersWithTravelObjective = ConcurrentHashMap.newKeySet();

    private static QuestProgressService instance;

    public QuestProgressService(GalaxyLeveling plugin) {
        this.plugin = plugin;
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Carica i dati missione per un giocatore (chiamato al join dal ProfileService).
     */
    public void loadPlayerData(Player player, Map<String, QuestProgress> progressMap, 
                               Set<String> completed, Set<String> accepted) {
        UUID uuid = player.getUniqueId();
        playerQuestProgress.put(uuid, progressMap);
        playerCompletedQuests.put(uuid, completed);
        playerAcceptedQuests.put(uuid, accepted);
        
        // Popola travel set se necessario
        updateTravelObjectiveSet(player);
    }

    /**
     * Salva i dati missione per un giocatore.
     */
    public Map<String, QuestProgress> getPlayerProgress(Player player) {
        return playerQuestProgress.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
    }

    public Set<String> getCompletedQuests(Player player) {
        return playerCompletedQuests.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
    }

    public Set<String> getAcceptedQuests(Player player) {
        return playerAcceptedQuests.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
    }

    /**
     * Accetta una missione.
     */
    public boolean acceptQuest(Player player, Quest quest) {
        Set<String> accepted = getAcceptedQuests(player);
        Set<String> completed = getCompletedQuests(player);
        
        // Verifiche
        if (accepted.contains(quest.getId())) return false; // Già accettata
        if (completed.contains(quest.getId()) && quest.isOneTime()) return false; // Già completata one-time
        
        QuestProgress progress = new QuestProgress(quest.getId());
        getPlayerProgress(player).put(quest.getId(), progress);
        accepted.add(quest.getId());
        
        player.sendMessage("§aMissione accettata: §e" + quest.getDisplayName());
        updateTravelObjectiveSet(player);
        return true;
    }

    /**
     * Completa una missione e distribuisce ricompense.
     */
    public void completeQuest(Player player, String questId) {
        QuestProgress progress = getPlayerProgress(player).get(questId);
        if (progress == null || !progress.isCompleted()) return;
        
        Quest quest = QuestManager.getInstance().getQuest(questId);
        if (quest == null) return;
        
        // Rimuovi da accepted
        getAcceptedQuests(player).remove(questId);
        
        // Aggiungi a completed se one-time/story/trial
        if (quest.isOneTime() || quest.getCategory() == Quest.Category.STORY || 
            quest.getCategory() == Quest.Category.TRIAL || quest.getCategory() == Quest.Category.CLASS_SPECIFIC) {
            getCompletedQuests(player).add(questId);
        }
        
        // Dai ricompense
        giveRewards(player, quest);
        
        // Rimuovi progresso
        getPlayerProgress(player).remove(questId);
        
        player.sendMessage("§6§lMissione Completata: §e" + quest.getDisplayName());
        updateTravelObjectiveSet(player);
    }

    /**
     * Distribuisce le ricompense di una missione.
     */
    private void giveRewards(Player player, Quest quest) {
        Quest.QuestRewards rewards = quest.getRewards();
        
        // XP
        if (rewards.getXp() > 0) {
            XpService xpService = XpService.getInstance();
            if (xpService != null) {
                xpService.grantXp(player, rewards.getXp(), "QUEST");
            }
        }
        
        // Stat Points
        if (rewards.getStatPoints() > 0) {
            PlayerProfile profile = PlayerProfileService.getInstance().getProfile(player.getUniqueId());
            if (profile != null) {
                profile.addUnspentStatPoints(rewards.getStatPoints());
                player.sendMessage("§a+§e" + rewards.getStatPoints() + " §apunti statistica!");
            }
        }
        
        // Items
        for (Quest.RewardItem rewardItem : rewards.getItems()) {
            player.getInventory().addItem(org.bukkit.inventory.ItemStack.of(rewardItem.getMaterial(), rewardItem.getAmount()));
        }
        
        // Commands
        for (String cmd : rewards.getCommands()) {
            String command = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    /**
     * Punto centrale per avanzare qualsiasi obiettivo missione.
     * @param type Tipo di obiettivo
     * @param key Chiave (es. EntityType.name(), Material.name())
     * @param amount Quantità progredita
     */
    public void advance(Player player, Quest.ObjectiveType type, String key, int amount) {
        UUID uuid = player.getUniqueId();
        Map<String, QuestProgress> progressMap = playerQuestProgress.get(uuid);
        if (progressMap == null) return;
        
        Set<String> accepted = getAcceptedQuests(player);
        
        for (String questId : accepted) {
            QuestProgress progress = progressMap.get(questId);
            if (progress == null || progress.isCompleted()) continue;
            
            Quest quest = QuestManager.getInstance().getQuest(questId);
            if (quest == null || quest.getType() != type) continue;
            
            // Controlla target
            if (!quest.getTarget().equalsIgnoreCase(key) && !quest.getTarget().equalsIgnoreCase("ANY")) {
                continue;
            }
            
            // Anti-exploit per BREAK_BLOCK (placed blocks)
            if (type == Quest.ObjectiveType.BREAK_BLOCK && !quest.isAllowPlacedBlocks()) {
                // Qui andrebbe controllato il PDC tag sui blocchi
                // Per ora skip se è un blocco piazzato dal giocatore (logica da implementare nel listener)
            }
            
            // Avanza progresso
            int current = progress.getCurrentProgress();
            if (current < quest.getAmount()) {
                progress.addProgress(amount);
                player.sendMessage("§7Progresso: §e" + progress.getCurrentProgress() + "§7/§e" + quest.getAmount());
                
                // Controlla completamento
                if (progress.getCurrentProgress() >= quest.getAmount()) {
                    progress.setCompleted(true);
                    player.sendMessage("§6§lObiettivo Raggiunto! §7Completa la missione per la ricompensa.");
                }
            }
        }
    }

    /**
     * Aggiorna il set dei giocatori con obiettivo travel attivo.
     */
    private void updateTravelObjectiveSet(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, QuestProgress> progressMap = playerQuestProgress.get(uuid);
        if (progressMap == null) {
            playersWithTravelObjective.remove(uuid);
            return;
        }
        
        boolean hasTravel = false;
        Set<String> accepted = getAcceptedQuests(player);
        for (String questId : accepted) {
            Quest quest = QuestManager.getInstance().getQuest(questId);
            if (quest != null && quest.getType() == Quest.ObjectiveType.TRAVEL_DISTANCE) {
                hasTravel = true;
                break;
            }
        }
        
        if (hasTravel) {
            playersWithTravelObjective.add(uuid);
        } else {
            playersWithTravelObjective.remove(uuid);
        }
    }

    /**
     * Controlla se un giocatore ha un obiettivo travel attivo (per ottimizzazione listener).
     */
    public boolean hasActiveTravelObjective(Player player) {
        return playersWithTravelObjective.contains(player.getUniqueId());
    }

    /**
     * Avanza obiettivo travel distance.
     */
    public void advanceTravel(Player player, double distance) {
        advance(player, Quest.ObjectiveType.TRAVEL_DISTANCE, "ANY", (int) distance);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateTravelObjectiveSet(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerQuestProgress.remove(uuid);
        playerCompletedQuests.remove(uuid);
        playerAcceptedQuests.remove(uuid);
        playersWithTravelObjective.remove(uuid);
    }

    public static QuestProgressService getInstance() {
        return instance;
    }
}
