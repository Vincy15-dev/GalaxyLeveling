package com.galaxyleveling.listener;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.Quest;
import com.galaxyleveling.service.QuestProgressService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Listener per TRAVEL_DISTANCE obiettivo con anti-teleport exploit.
 */
public class TravelDistanceListener implements Listener {

    private final GalaxyLeveling plugin;
    private final QuestProgressService questProgressService;
    
    // Ultima posizione nota per giocatore (per calcolo delta)
    private final Map<Player, Location> lastKnownLocation = new WeakHashMap<>();

    public TravelDistanceListener(GalaxyLeveling plugin) {
        this.plugin = plugin;
        this.questProgressService = QuestProgressService.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Traccia i teleport per invalidare il conteggio travel.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        // Rimuovi dalla cache l'ultima posizione nota dopo un teleport
        lastKnownLocation.remove(player);
    }

    /**
     * Calcola e avanza il travel distance solo se il giocatore ha un obiettivo attivo.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Ottimizzazione: skip se il giocatore non ha obiettivi travel attivi
        if (!questProgressService.hasActiveTravelObjective(player)) {
            return;
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Skip se stessa posizione o mondo diverso
        if (from == null || to == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }
        
        // Calcola distanza orizzontale (ignora Y per evitare exploit scale/ladder)
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        // Sanity check: max distanza per tick (anti-speedhack/teleport exploit)
        double maxPerTick = plugin.getConfig().getDouble("quests.max-travel-per-tick", 10.0);
        if (distance > maxPerTick) {
            // Teleport sospetto o speedhack, resetta la posizione nota
            lastKnownLocation.remove(player);
            return;
        }
        
        // Accumula distanza
        Location lastLoc = lastKnownLocation.get(player);
        if (lastLoc != null && lastLoc.getWorld().equals(from.getWorld())) {
            // Distanza già calcolata nel movimento, usa quella
            questProgressService.advanceTravel(player, distance);
        }
        
        // Aggiorna ultima posizione nota
        lastKnownLocation.put(player, to.clone());
    }
}
