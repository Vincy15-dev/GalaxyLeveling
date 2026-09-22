package com.galaxyleveling.listener;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.QuestProgress;
import com.galaxyleveling.service.QuestManager;
import com.galaxyleveling.service.QuestProgressService;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class QuestObjectiveListener implements Listener {
    private final GalaxyLeveling plugin;

    public QuestObjectiveListener(GalaxyLeveling plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Controlla se mob è da spawner (anti-farm)
        if (!plugin.getConfig().getBoolean("system.quests.allow-spawner-mobs", false)) {
            if (event.getEntity().hasMetadata("spawner-spawned")) {
                return;
            }
        }

        EntityType entityType = event.getEntityType();
        QuestProgressService qps = plugin.getQuestProgressService();
        QuestManager qm = plugin.getQuestManager();

        // Avanza missioni KILL_MOB
        for (QuestProgress progress : qps.getActiveQuests(killer)) {
            if (progress.getQuest().getType() == com.galaxyleveling.model.QuestType.KILL_MOB) {
                String target = progress.getQuest().getTarget();
                if (target.equals("ANY") || target.equals(entityType.name())) {
                    qps.advance(killer, progress.getQuest().getId(), 1);
                }
            }
        }
    }
}
