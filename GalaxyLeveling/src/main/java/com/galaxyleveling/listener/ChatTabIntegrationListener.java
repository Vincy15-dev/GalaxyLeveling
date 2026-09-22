package com.galaxyleveling.listener;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.util.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatTabIntegrationListener implements Listener {
    private final GalaxyLeveling plugin;

    public ChatTabIntegrationListener(GalaxyLeveling plugin) {
        this.plugin = plugin;
        // Registra solo se abilitato in config (default OFF)
        if (!plugin.getConfig().getBoolean("integration.use-direct-chat-hook", false)) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        // Hook diretto per chat: aggiunge prefix del rank
        // DISABILITARE se si usa un plugin di chat esterno (TAB, EssentialsXChat, ecc.)
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) return;

        String rankId = profile.getCurrentRankId();
        var rank = plugin.getRankManager().getRankById(rankId);
        if (rank != null) {
            String prefix = ColorUtils.colorize(rank.getColorHex() + "[" + rank.getDisplayName() + "] ");
            event.setFormat(prefix + "%s: %s");
        }
    }
}
