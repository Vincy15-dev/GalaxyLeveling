package com.galaxyleveling.service;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class XpService {
    private final GalaxyLeveling plugin;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public XpService(GalaxyLeveling plugin) {
        this.plugin = plugin;
    }

    public void grantXp(Player player, long amount, String source) {
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) return;

        // Moltiplicatore Intelligenza
        double intMultiplier = 1.0 + (profile.getStatValue(com.galaxyleveling.model.StatDefinition.INTELLIGENZA) * 0.02);
        long finalAmount = (long) (amount * intMultiplier);

        profile.setTotalXp(profile.getTotalXp() + finalAmount);
        
        // ActionBar notification
        player.sendActionBar(ColorUtils.colorize("&b+" + finalAmount + " XP"));

        // Controlla level-up
        checkLevelUp(player, profile);
        
        updateBossBar(player);
    }

    private void checkLevelUp(Player player, PlayerProfile profile) {
        int maxLevel = plugin.getConfig().getInt("system.xp.max-level", 100);
        while (profile.getLevel() < maxLevel) {
            long xpNeeded = getXpForLevel(profile.getLevel());
            if (profile.getTotalXp() >= xpNeeded) {
                profile.setLevel(profile.getLevel() + 1);
                profile.setUnspentStatPoints(profile.getUnspentStatPoints() + 3);
                
                // Level up effects
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.sendTitle(ColorUtils.colorize("&e&lLEVEL UP!"), ColorUtils.colorize("&7Livello " + profile.getLevel()), 10, 70, 20);
                
                plugin.getRankManager().checkRankUp(player, profile);
            } else {
                break;
            }
        }
    }

    public long getXpForLevel(int level) {
        int base = plugin.getConfig().getInt("system.xp.base-xp-for-level", 50);
        int linear = plugin.getConfig().getInt("system.xp.linear-coefficient", 25);
        int quadratic = plugin.getConfig().getInt("system.xp.quadratic-coefficient", 5);
        return base + (linear * level) + (quadratic * level * level);
    }

    public void updateBossBar(Player player) {
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) return;

        long xpNeeded = getXpForLevel(profile.getLevel());
        long currentXpInLevel = profile.getTotalXp() - getXpForLevel(profile.getLevel() - 1);
        if (currentXpInLevel < 0) currentXpInLevel = profile.getTotalXp();

        BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(), k -> {
            BossBar newBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
            newBar.addPlayer(player);
            return newBar;
        });

        double progress = Math.min(1.0, (double) currentXpInLevel / xpNeeded);
        bar.setTitle(ColorUtils.colorize("&fLivello " + profile.getLevel() + " &8| &7" + currentXpInLevel + "/" + xpNeeded + " XP"));
        bar.setProgress(progress);
    }

    public void removeBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }
}
