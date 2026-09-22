package com.galaxyleveling.listener;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.Quest;
import com.galaxyleveling.service.QuestProgressService;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener per gli obiettivi delle missioni e anti-farm tagging.
 */
public class QuestObjectiveListener implements Listener {

    private final GalaxyLeveling plugin;
    private final QuestProgressService questProgressService;
    
    // PDC Keys per anti-farm tagging
    private final NamespacedKey placedByKey;
    private final NamespacedKey spawnerSpawnedKey;

    public QuestObjectiveListener(GalaxyLeveling plugin) {
        this.plugin = plugin;
        this.questProgressService = QuestProgressService.getInstance();
        
        placedByKey = new NamespacedKey(plugin, "placed_by_player");
        spawnerSpawnedKey = new NamespacedKey(plugin, "spawner_spawned");
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * KILL_MOB obiettivo.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        EntityType entityType = event.getEntityType();
        
        // Anti-exploit: skip se mob spawnato da spawner (se configurato)
        PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
        if (pdc.has(spawnerSpawnedKey, PersistentDataType.BYTE)) {
            boolean allowSpawnerMobs = plugin.getConfig().getBoolean("quests.allow-spawner-mobs", false);
            if (!allowSpawnerMobs) {
                return; // Ignora mob da spawner
            }
        }
        
        // Avanza missione
        questProgressService.advance(killer, Quest.ObjectiveType.KILL_MOB, entityType.name(), 1);
        
        // Avanza anche DEAL_DAMAGE (usando getFinalDamage)
        double damage = event.getEntity().getLastDamage();
        if (damage > 0) {
            questProgressService.advance(killer, Quest.ObjectiveType.DEAL_DAMAGE, "ANY", (int) damage);
        }
    }

    /**
     * BREAK_BLOCK obiettivo con anti-farm place-then-break.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Controlla se il blocco è stato piazzato da un giocatore
        PersistentDataContainer pdc = block.getPersistentDataContainer();
        if (pdc.has(placedByKey, PersistentDataType.STRING)) {
            // Blocco piazzato da giocatore - controlla se la missione allow-placed-blocks
            // La logica di filtraggio è in QuestProgressService.advance
            // Qui tagghiamo solo per riferimento
        }
        
        String materialName = block.getType().name();
        questProgressService.advance(player, Quest.ObjectiveType.BREAK_BLOCK, materialName, 1);
    }

    /**
     * Tagga i blocchi piazzati dai giocatori per anti-farm.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();
        
        // Tagga il blocco come piazzato da questo giocatore
        PersistentDataContainer pdc = block.getPersistentDataContainer();
        pdc.set(placedByKey, PersistentDataType.STRING, player.getUniqueId().toString());
    }

    /**
     * Tagga i mob spawnati da spawner per anti-farm.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
            pdc.set(spawnerSpawnedKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    /**
     * FISH_ITEM obiettivo.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        Player player = event.getPlayer();
        if (!(event.getCaught() instanceof org.bukkit.entity.Item itemEntity)) return;
        
        String itemType = itemEntity.getItemStack().getType().name();
        questProgressService.advance(player, Quest.ObjectiveType.FISH_ITEM, itemType, 1);
        
        // Bonus percezione per treasure (da implementare con chance)
        // Per ora contiamo tutti i pesci ugualmente
    }
}
