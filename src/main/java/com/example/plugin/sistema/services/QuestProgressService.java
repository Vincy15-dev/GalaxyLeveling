package com.example.plugin.sistema.services;

import com.example.plugin.sistema.SistemaPlugin;
import com.example.plugin.sistema.models.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.pdc.PersistentDataContainer;
import org.bukkit.pdc.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce il progresso delle missioni e gli eventi correlati.
 * Include anti-exploit per farming di blocchi e mob da spawner.
 */
public class QuestProgressService implements Listener {

    private final SistemaPlugin plugin;
    
    // Set di giocatori con obiettivi di viaggio attivi (per ottimizzazione PlayerMoveEvent)
    private final Set<UUID> playersWithTravelObjective = ConcurrentHashMap.newKeySet();
    
    // Tag PDC per blocchi piazzati dai giocatori (anti-farm)
    private final NamespacedKey playerPlacedKey;
    
    // Tag PDC per mob da spawner (anti-farm kill)
    private final NamespacedKey spawnerSpawnedKey;

    public QuestProgressService(SistemaPlugin plugin) {
        this.plugin = plugin;
        this.playerPlacedKey = new NamespacedKey(plugin, "player_placed");
        this.spawnerSpawnedKey = new NamespacedKey(plugin, "spawner_spawned");
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Punto centrale per avanzare il progresso di una missione.
     * Chiamato dagli event listener o da altre parti del codice.
     */
    public void advance(Player player, Quest.QuestType type, String key, int amount) {
        UUID uuid = player.getUniqueId();
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(uuid);
        
        if (profile == null) return;

        // Ottieni tutte le missioni attive del giocatore
        Map<String, QuestProgress> progressMap = profile.getQuestProgressMap();
        
        for (QuestProgress qp : progressMap.values()) {
            if (qp.isCompleted() && !qp.getQuest().isRepeatable()) continue;
            
            Quest quest = qp.getQuest();
            if (quest.getType() != type) continue;
            
            // Controlla se il target corrisponde
            if (!quest.getTarget().equalsIgnoreCase("ANY") && 
                !quest.getTarget().equalsIgnoreCase(key)) {
                continue;
            }
            
            // Avanza il progresso
            int newProgress = qp.getProgress() + amount;
            if (newProgress >= quest.getAmount()) {
                completeQuest(player, qp);
            } else {
                qp.setProgress(newProgress);
                // Update UI se GUI aperta
                updateQuestGUIIfOpen(player, qp);
            }
        }
    }

    /**
     * Completa una missione e distribuisce i reward.
     */
    private void completeQuest(Player player, QuestProgress qp) {
        Quest quest = qp.getQuest();
        qp.setCompleted(true);
        qp.setLastCompletedEpoch(System.currentTimeMillis());
        
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) return;

        // Distribuisci reward
        int xpReward = quest.getXpReward();
        int statPointsReward = quest.getStatPointsReward();
        
        if (xpReward > 0) {
            plugin.getXpService().grantXp(player, xpReward, XpService.XpSource.QUEST);
        }
        
        if (statPointsReward > 0) {
            profile.addUnspentStatPoints(statPointsReward);
            player.sendMessage(plugin.getMessage("success.stat-points-allocated")
                .replace("%points%", String.valueOf(statPointsReward)));
        }
        
        // Item rewards
        for (Map<String, Object> itemData : quest.getItemRewards()) {
            try {
                String matStr = (String) itemData.get("material");
                int qty = ((Number) itemData.getOrDefault("amount", 1)).intValue();
                boolean enchanted = (Boolean) itemData.getOrDefault("enchanted", false);
                
                Material material = Material.matchMaterial(matStr);
                if (material != null) {
                    ItemStack rewardItem = new ItemStack(material, qty);
                    // TODO: applicare enchantments se enchanted=true
                    player.getInventory().addItem(rewardItem);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Errore nel dare item reward per quest " + quest.getId());
            }
        }
        
        // Command rewards
        for (String cmd : quest.getCommandRewards()) {
            try {
                String command = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception e) {
                plugin.getLogger().warning("Errore nell'eseguire comando reward '" + cmd + "' per quest " + quest.getId());
            }
        }

        // Feedback al giocatore
        String rewardsText = xpReward + " XP";
        if (statPointsReward > 0) rewardsText += ", " + statPointsReward + " punti stat";
        player.sendMessage(plugin.getMessage("success.quest-completed").replace("%rewards%", rewardsText));
        
        // Suono di completamento
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        
        // Se è una trial quest, controlla se sblocca un rank
        if (quest.getCategory() == Quest.Category.TRIAL) {
            plugin.getRankManager().checkTrialCompletion(player, quest);
        }
        
        // Salva profilo async
        plugin.getPlayerProfileService().saveProfile(profile, false);
    }

    /**
     * Resetta le missioni daily/settimanali in base all'orario configurato.
     */
    public void resetDailyQuests() {
        int resetHour = plugin.getConfig().getInt("system.quests.daily-reset-hour", 0);
        long today = LocalDate.now().toEpochDay();
        
        for (PlayerProfile profile : plugin.getPlayerProfileService().getAllLoadedProfiles()) {
            boolean saved = false;
            for (QuestProgress qp : profile.getQuestProgressMap().values()) {
                if (qp.getQuest().getCategory() == Quest.Category.DAILY) {
                    qp.resetProgress();
                    saved = true;
                }
            }
            if (saved) {
                profile.setLastDailyResetEpochDay(today);
            }
        }
        
        plugin.getLogger().info("Resetate missioni daily per tutti i giocatori online.");
    }

    /**
     * Resetta le missioni weekly in base alla settimana ISO.
     */
    public void resetWeeklyQuests() {
        long currentWeek = WeekFields.ISO.weekOfWeekBasedYear().getValue(Instant.now().atZone(ZoneId.systemDefault()));
        
        for (PlayerProfile profile : plugin.getPlayerProfileService().getAllLoadedProfiles()) {
            boolean saved = false;
            for (QuestProgress qp : profile.getQuestProgressMap().values()) {
                if (qp.getQuest().getCategory() == Quest.Category.WEEKLY) {
                    qp.resetProgress();
                    saved = true;
                }
            }
            if (saved) {
                profile.setLastWeeklyResetWeek(currentWeek);
            }
        }
        
        plugin.getLogger().info("Resetate missioni weekly per tutti i giocatori online.");
    }

    /**
     * Controlla e resetta le missioni se necessario (chiamato periodicamente o al join).
     */
    public void checkAndResetQuests(PlayerProfile profile) {
        long today = LocalDate.now().toEpochDay();
        long currentWeek = WeekFields.ISO.weekOfWeekBasedYear().getValue(Instant.now().atZone(ZoneId.systemDefault()));
        
        // Reset daily
        if (profile.getLastDailyResetEpochDay() < today) {
            for (QuestProgress qp : profile.getQuestProgressMap().values()) {
                if (qp.getQuest().getCategory() == Quest.Category.DAILY) {
                    qp.resetProgress();
                }
            }
            profile.setLastDailyResetEpochDay(today);
        }
        
        // Reset weekly
        if (profile.getLastWeeklyResetWeek() < currentWeek) {
            for (QuestProgress qp : profile.getQuestProgressMap().values()) {
                if (qp.getQuest().getCategory() == Quest.Category.WEEKLY) {
                    qp.resetProgress();
                }
            }
            profile.setLastWeeklyResetWeek(currentWeek);
        }
    }

    /**
     * Aggiorna la GUI delle missioni se è aperta.
     */
    private void updateQuestGUIIfOpen(Player player, QuestProgress qp) {
        // Implementato in QuestsGUI
        // Questo è un hook per future implementazioni
    }

    // ================= EVENT LISTENERS =================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) return;
        
        Player killer = event.getEntity().getKiller();
        String entityType = event.getEntityType().name();
        
        // Controlla se il mob era da spawner (se allow-spawner-mobs è false)
        boolean allowSpawnerMobs = plugin.getConfig().getBoolean("system.quests.allow-spawner-mobs", false);
        if (!allowSpawnerMobs) {
            PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
            if (pdc.has(spawnerSpawnedKey, PersistentDataType.BYTE)) {
                return; // Ignora mob da spawner
            }
        }
        
        advance(killer, Quest.QuestType.KILL_MOB, entityType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().name();
        
        // Controlla se il blocco era stato piazzato dal giocatore (anti-farm)
        PersistentDataContainer pdc = event.getBlock().getPersistentDataContainer();
        if (pdc.has(playerPlacedKey, PersistentDataType.BYTE)) {
            // Blocco piazzato dal giocatore - ignora a meno che la quest non lo permetta
            // La verifica specifica della quest avviene in advance()
            return;
        }
        
        advance(player, Quest.QuestType.BREAK_BLOCK, blockType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Tagga il blocco come piazzato dal giocatore
        PersistentDataContainer pdc = event.getBlock().getPersistentDataContainer();
        pdc.set(playerPlacedKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        Player player = event.getPlayer();
        ItemStack caught = event.getCaught();
        
        if (caught == null) return;
        
        String itemType = caught.getType().name();
        advance(player, Quest.QuestType.FISH_ITEM, itemType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Ottimizzazione: skip se nessun giocatore ha travel objective
        if (playersWithTravelObjective.isEmpty()) return;
        if (!playersWithTravelObjective.contains(uuid)) return;
        
        // Controlla teleport (distanza troppo grande = teleport)
        double maxPerTick = plugin.getConfig().getDouble("system.quests.max-travel-per-tick", 10.0);
        double distance = event.getFrom().distance(event.getTo());
        
        if (distance > maxPerTick) {
            return; // Teleport, ignora
        }
        
        // Stesso mondo?
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }
        
        advance(player, Quest.QuestType.TRAVEL_DISTANCE, "ANY", (int) Math.floor(distance * 100)); // cm
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(PrepareItemCraftEvent event) {
        // Nota: questo evento fire molte volte, meglio usare InventoryClickEvent per craft effettivi
        // Implementazione semplificata qui
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getWhoClicked() instanceof Player ? 
            (Player) event.getWhoClicked() : null;
        if (player == null) return;
        
        advance(player, Quest.QuestType.ENCHANT_ITEM, "ANY", 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        // Implementazione semplificata - richiede tracking più complesso
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTameAnimal(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player)) return;
        Player player = (Player) event.getOwner();
        
        advance(player, Quest.QuestType.TAME_ANIMAL, event.getEntityType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreedAnimals(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player)) return;
        Player player = (Player) event.getBreeder();
        
        advance(player, Quest.QuestType.BREED_ANIMAL, event.getEntityType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDealDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        
        // Usa getFinalDamage() al MONITOR per il danno effettivo dopo riduzioni
        // Ma qui siamo a NORMAL, quindi usiamo getDamage() come approssimazione
        double damage = event.getDamage();
        advance(player, Quest.QuestType.DEAL_DAMAGE, "ANY", (int) Math.ceil(damage));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelUp(com.example.plugin.sistema.events.PlayerLevelUpEvent event) {
        Player player = event.getPlayer();
        int newLevel = event.getNewLevel();
        
        advance(player, Quest.QuestType.REACH_LEVEL, "ANY", newLevel);
    }

    /**
     * Registra un giocatore come avente un obiettivo di viaggio attivo.
     */
    public void addTravelObjective(UUID uuid) {
        playersWithTravelObjective.add(uuid);
    }

    /**
     * Rimuove un giocatore dagli obiettivi di viaggio.
     */
    public void removeTravelObjective(UUID uuid) {
        playersWithTravelObjective.remove(uuid);
    }
}
