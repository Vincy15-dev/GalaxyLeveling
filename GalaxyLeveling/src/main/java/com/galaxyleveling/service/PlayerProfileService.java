package com.galaxyleveling.service;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.storage.ProfileStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerProfileService implements Listener {
    private final GalaxyLeveling plugin;
    private final ProfileStorage storage;
    private final Map<UUID, PlayerProfile> loadedProfiles = new ConcurrentHashMap<>();
    private final Set<UUID> loadingProfiles = ConcurrentHashMap.newKeySet();

    public PlayerProfileService(GalaxyLeveling plugin, ProfileStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public PlayerProfile getProfile(UUID uuid) {
        return loadedProfiles.get(uuid);
    }

    public void saveProfile(PlayerProfile profile, boolean removeFromCache) {
        if (profile == null) return;
        storage.saveProfile(profile).thenRun(() -> {
            if (removeFromCache) {
                loadedProfiles.remove(profile.getPlayerUuid());
            }
        });
    }

    public void saveAllOnline() {
        for (PlayerProfile profile : loadedProfiles.values()) {
            saveProfile(profile, false);
        }
    }

    public boolean isProfileReady(UUID uuid) {
        return loadedProfiles.containsKey(uuid) && !loadingProfiles.contains(uuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (loadedProfiles.containsKey(uuid)) {
            return;
        }

        loadingProfiles.add(uuid);
        storage.loadProfile(uuid).thenAccept(profile -> {
            loadedProfiles.put(uuid, profile);
            loadingProfiles.remove(uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getStatEngine().recomputeAllModifiers(player);
                plugin.getXpService().updateBossBar(player);
            });
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Errore caricamento profilo per " + uuid, ex);
            loadingProfiles.remove(uuid);
            loadedProfiles.put(uuid, PlayerProfile.createNew(uuid));
            return null;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        PlayerProfile profile = loadedProfiles.get(uuid);
        if (profile != null) {
            saveProfile(profile, true);
            plugin.getXpService().removeBossBar(player);
            plugin.getStatEngine().clearCache(uuid);
        }
    }

    public Collection<PlayerProfile> getAllLoadedProfiles() {
        return Collections.unmodifiableCollection(loadedProfiles.values());
    }
}
