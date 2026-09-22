package com.galaxyleveling;

import com.galaxyleveling.command.*;
import com.galaxyleveling.integration.SistemaExpansion;
import com.galaxyleveling.service.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class GalaxyLeveling extends JavaPlugin {

    private static GalaxyLeveling instance;
    
    // Services
    private PlayerProfileService playerProfileService;
    private StatEngine statEngine;
    private XpService xpService;
    private LevelingManager levelingManager;
    private RankManager rankManager;
    private ClassManager classManager;
    private QuestManager questManager;
    private QuestProgressService questProgressService;
    private FocusService focusService;
    private LeaderboardService leaderboardService;

    @Override
    public void onEnable() {
        instance = this;
        
        // Salva resources di default se non esistono
        saveDefaultConfig();
        saveResource("config.yml", false);
        saveResource("ranks.yml", false);
        saveResource("classes.yml", false);
        saveResource("quests.yml", false);
        saveResource("messages.yml", false);

        // Inizializza servizi in ordine di dipendenza
        rankManager = new RankManager(this);
        classManager = new ClassManager(this);
        playerProfileService = new PlayerProfileService(this);
        statEngine = new StatEngine(this);
        xpService = new XpService(this);
        levelingManager = new LevelingManager(this);
        questManager = new QuestManager(this);
        questProgressService = new QuestProgressService(this);
        focusService = new FocusService(this);
        leaderboardService = new LeaderboardService(this);

        // Registra comandi
        getCommand("status").setExecutor(new StatusCommand(this));
        getCommand("quests").setExecutor(new QuestsCommand(this));
        getCommand("classe").setExecutor(new ClasseCommand(this));
        getCommand("topsistema").setExecutor(new TopsistemaCommand(this));
        getCommand("system").setExecutor(new SistemaCommand(this));
        getCommand("system").setTabCompleter(new SistemaTabCompleter(this));

        // PlaceholderAPI integration (soft-depend)
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SistemaExpansion(this).register();
            getLogger().info("PlaceholderAPI trovato - espansione registrata.");
        } else {
            getLogger().info("PlaceholderAPI non trovato - integrazione disabilitata.");
        }

        getLogger().info("GalaxyLeveling caricato correttamente!");
    }

    @Override
    public void onDisable() {
        // Salva tutti i profili online
        if (playerProfileService != null) {
            playerProfileService.saveAllOnline();
        }
        
        // Ferma task async
        if (leaderboardService != null) {
            leaderboardService.shutdown();
        }
        
        getLogger().info("GalaxyLeveling disabilitato.");
    }

    public static GalaxyLeveling getInstance() {
        return instance;
    }

    // Getters per servizi
    public PlayerProfileService getPlayerProfileService() { return playerProfileService; }
    public StatEngine getStatEngine() { return statEngine; }
    public XpService getXpService() { return xpService; }
    public LevelingManager getLevelingManager() { return levelingManager; }
    public RankManager getRankManager() { return rankManager; }
    public ClassManager getClassManager() { return classManager; }
    public QuestManager getQuestManager() { return questManager; }
    public QuestProgressService getQuestProgressService() { return questProgressService; }
    public FocusService getFocusService() { return focusService; }
    public LeaderboardService getLeaderboardService() { return leaderboardService; }
    
    public String getMessage(String path) {
        return getConfig().getString("messages." + path, "&cMessaggio non trovato: " + path);
    }
}