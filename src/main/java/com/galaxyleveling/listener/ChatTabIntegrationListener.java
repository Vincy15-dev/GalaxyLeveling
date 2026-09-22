package com.galaxyleveling.listener;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.service.PlayerProfileService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Hook diretto per chat e tab list (OPZIONALE, disattivato di default).
 * DA MANTENERE DISATTIVATO se si usano plugin come TAB o EssentialsX Chat.
 * Config-gated: integration.use-direct-chat-hook / use-direct-tab-hook.
 */
public class ChatTabIntegrationListener implements Listener {

    private final GalaxyLeveling plugin;
    private final PlayerProfileService profileService;
    private final boolean useDirectChatHook;
    private final boolean useDirectTabHook;

    public ChatTabIntegrationListener(GalaxyLeveling plugin) {
        this.plugin = plugin;
        this.profileService = plugin.getPlayerProfileService();
        this.useDirectChatHook = plugin.getConfig().getBoolean("integration.use-direct-chat-hook", false);
        this.useDirectTabHook = plugin.getConfig().getBoolean("integration.use-direct-tab-hook", false);

        if (useDirectChatHook || useDirectTabHook) {
            plugin.getLogger().warning("Direct chat/tab hook ABILITATO. Se usi TAB/EssentialsX, DISABILITALO in config.yml!");
        }
    }

    /**
     * Hook chat: prepend del prefisso del grado al messaggio.
     * Priorità BASSA per non interferire con chat manager.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!useDirectChatHook) return;

        Player player = event.getPlayer();
        PlayerProfile profile = profileService.getProfile(player.getUniqueId());
        if (profile == null) return;

        var rank = profile.getCurrentRank();
        if (rank != null) {
            String prefix = rank.getColorHex() + rank.getDisplayName() + " &r";
            String originalFormat = event.getFormat();
            // Formato base: [Prefisso] Nome: Messaggio
            event.setFormat(prefix + originalFormat);
        }
    }

    /**
     * Hook tab list: imposta il playerListName con il prefisso del grado.
     * Eseguito al join e quando il grado cambia.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        if (!useDirectTabHook) return;
        updateTabName(event.getPlayer());
    }

    public void updateTabName(Player player) {
        if (!useDirectTabHook) return;

        PlayerProfile profile = profileService.getProfile(player.getUniqueId());
        if (profile == null) return;

        var rank = profile.getCurrentRank();
        if (rank != null) {
            String displayName = rank.getColorHex() + rank.getDisplayName() + " " + player.getName();
            player.setPlayerListName(displayName);
        }
    }
}
