package com.galaxyleveling.command;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.gui.LeaderboardGUI;
import com.galaxyleveling.service.PlayerProfileService;
import com.galaxyleveling.util.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TopsistemaCommand implements CommandExecutor {

    private final GalaxyLeveling plugin;

    public TopsistemaCommand(GalaxyLeveling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize("&cQuesto comando può essere usato solo da giocatori."));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("system.use")) {
            player.sendMessage(ColorUtils.colorize(plugin.getMessage("error.no-permission")));
            return true;
        }

        PlayerProfileService profileService = plugin.getPlayerProfileService();
        if (!profileService.isProfileReady(player.getUniqueId())) {
            player.sendMessage(ColorUtils.colorize(plugin.getMessage("error.profile-not-loaded")));
            return true;
        }

        LeaderboardGUI gui = new LeaderboardGUI(plugin);
        gui.openGUI(player);

        return true;
    }
}
