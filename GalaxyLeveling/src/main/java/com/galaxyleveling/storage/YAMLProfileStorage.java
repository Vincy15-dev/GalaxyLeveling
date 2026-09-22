package com.galaxyleveling.storage;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.model.StatDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class YAMLProfileStorage implements ProfileStorage {
    private final GalaxyLeveling plugin;
    private final File dataFolder;

    public YAMLProfileStorage(GalaxyLeveling plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    @Override
    public CompletableFuture<PlayerProfile> loadProfile(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(dataFolder, uuid.toString() + ".yml");
            if (!file.exists()) {
                return PlayerProfile.createNew(uuid);
            }

            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                PlayerProfile profile = new PlayerProfile(uuid);
                
                profile.setTotalXp(config.getLong("xp.total", 0));
                profile.setLevel(config.getInt("level", 1));
                profile.setUnspentStatPoints(config.getInt("stats.unspent", 0));
                profile.setChosenClassId(config.getString("class.id"));
                profile.setCurrentRankId(config.getString("rank.id", "e"));
                profile.setFocusedAmount(config.getDouble("focus.current", 0.0));
                profile.setPendingLevelUps(config.getInt("rank.pending_levelups", 0));

                ConfigurationSection statsSec = config.getConfigurationSection("stats.values");
                if (statsSec != null) {
                    for (String key : statsSec.getKeys(false)) {
                        StatDefinition stat = StatDefinition.fromId(key);
                        if (stat != null) {
                            profile.setStatValue(stat, statsSec.getInt(key, 0));
                        }
                    }
                }

                return profile;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Errore caricamento profilo per " + uuid, e);
                return PlayerProfile.createNew(uuid);
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveProfile(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> {
            File file = new File(dataFolder, profile.getPlayerUuid().toString() + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            try {
                config.set("xp.total", profile.getTotalXp());
                config.set("level", profile.getLevel());
                config.set("stats.unspent", profile.getUnspentStatPoints());
                config.set("class.id", profile.getChosenClassId());
                config.set("rank.id", profile.getCurrentRankId());
                config.set("rank.pending_levelups", profile.getPendingLevelUps());
                config.set("focus.current", profile.getFocusAmount());

                config.set("stats.values", null);
                for (Map.Entry<StatDefinition, Integer> entry : profile.getStatValues().entrySet()) {
                    config.set("stats.values." + entry.getKey().name().toLowerCase(), entry.getValue());
                }

                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Errore salvataggio profilo per " + profile.getPlayerUuid(), e);
            }
        });
    }

    @Override
    public void close() {
        // Nessun resource locking specifico per file YAML
    }
}
