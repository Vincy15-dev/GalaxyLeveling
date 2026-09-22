package com.galaxyleveling;

import com.galaxyleveling.command.*;
import com.galaxyleveling.gui.ClassSelectionGUI;
import com.galaxyleveling.gui.LeaderboardGUI;
import com.galaxyleveling.gui.QuestsGUI;
import com.galaxyleveling.gui.StatusGUI;
import com.galaxyleveling.integration.SistemaExpansion;
import com.galaxyleveling.listener.ChatTabIntegrationListener;
import com.galaxyleveling.listener.QuestObjectiveListener;
import com.galaxyleveling.service.*;
import com.galaxyleveling.storage.ProfileStorage;
import com.galaxyleveling.storage.YAMLProfileStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class GalaxyLeveling extends JavaPlugin {
    private static GalaxyLeveling instance;
    
    private PlayerProfileService playerProfileService;
    private XpService xpService;
    private StatEngine statEngine;
    private RankManager rankManager;
    private ClassManager classManager;
    private QuestManager questManager;
    private QuestProgressService questProgressService;
    private LeaderboardService leaderboardService;
    private ProfileStorage storage;
    
    private StatusGUI statusGUI;
    private QuestsGUI questsGUI;
    private ClassSelectionGUI classSelectionGUI;
    private LeaderboardGUI leaderboardGUI;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        saveResource("config.yml", false);
        saveResource("ranks.yml", false);
        saveResource("classes.yml", false);
        saveResource("quests.yml", false);
        saveResource("messages.yml", false);
        
        storage = new YAMLProfileStorage(this);
        
        rankManager = new RankManager(this);
        classManager = new ClassManager(this);
        questManager = new QuestManager(this);
        playerProfileService = new PlayerProfileService(this, storage);
        xpService = new XpService(this);
        statEngine = new StatEngine(this);
        questProgressService = new QuestProgressService(this);
        leaderboardService = new LeaderboardService(this);
        
        statusGUI = new StatusGUI(this);
        questsGUI = new QuestsGUI(this);
        classSelectionGUI = new ClassSelectionGUI(this);
        leaderboardGUI = new LeaderboardGUI(this);
        
        new QuestObjectiveListener(this);
        new ChatTabIntegrationListener(this);
        
        getCommand("status").setExecutor(new StatusCommand(this));
        getCommand("quests").setExecutor(new QuestsCommand(this));
        getCommand("classe").setExecutor(new ClasseCommand(this));
        getCommand("topsistema").setExecutor(new TopsistemaCommand(this));
        getCommand("system").setExecutor(new SistemaCommand(this));
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SistemaExpansion(this).register();
            getLogger().info("PlaceholderAPI integration abilitata.");
        } else {
            getLogger().info("PlaceholderAPI non trovato. Integrazione disabilitata.");
        }
        
        getLogger().info("GalaxyLeveling abilitato con successo!");
    }

    @Override
    public void onDisable() {
        if (playerProfileService != null) {
            playerProfileService.saveAllOnline();
        }
        if (leaderboardService != null) {
            leaderboardService.stop();
        }
        if (storage != null) {
            storage.close();
        }
        getLogger().info("GalaxyLeveling disabilitato.");
    }

    public static GalaxyLeveling getInstance() { return instance; }
    public PlayerProfileService getPlayerProfileService() { return playerProfileService; }
    public XpService getXpService() { return xpService; }
    public StatEngine getStatEngine() { return statEngine; }
    public RankManager getRankManager() { return rankManager; }
    public ClassManager getClassManager() { return classManager; }
    public QuestManager getQuestManager() { return questManager; }
    public QuestProgressService getQuestProgressService() { return questProgressService; }
    public LeaderboardService getLeaderboardService() { return leaderboardService; }
    
    public String getMessage(String path) {
        return getConfig().getString("messages." + path, "&cMessaggio non trovato: " + path);
    }
}
