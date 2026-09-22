package com.galaxyleveling.command;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.gui.ClassSelectionGUI;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.model.Rank;
import com.galaxyleveling.service.PlayerProfileService;
import com.galaxyleveling.util.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClasseCommand implements CommandExecutor {

    private final GalaxyLeveling plugin;

    public ClasseCommand(GalaxyLeveling plugin) {
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

        PlayerProfile profile = profileService.getProfile(player.getUniqueId());

        // Se ha già scelto una classe, mostra info
        if (profile.getChosenClassId() != null) {
            String classId = profile.getChosenClassId();
            var playerClass = plugin.getClassManager().getClassById(classId);
            if (playerClass != null) {
                player.sendMessage(ColorUtils.colorize("&6=== La tua Classe ==="));
                player.sendMessage(ColorUtils.colorize("&eNome: &f" + playerClass.getDisplayName()));
                player.sendMessage(ColorUtils.colorize("&eDescrizione: &f" + playerClass.getDescription()));
                player.sendMessage(ColorUtils.colorize("&eBonus Statistiche:"));
                playerClass.getStatBonuses().forEach((stat, mult) -> 
                    player.sendMessage(ColorUtils.colorize("  - &f" + stat.getDisplayName() + ": &ax" + String.format("%.2f", mult)))
                );
                player.sendMessage(ColorUtils.colorize("&cLa scelta della classe è permanente. Non puoi cambiarla."));
            } else {
                player.sendMessage(ColorUtils.colorize("&cClasse trovata nei dati ma non definita nella config."));
            }
            return true;
        }

        // Controlla se sbloccato
        Rank currentRank = profile.getCurrentRank();
        String unlockRankId = plugin.getConfig().getString("classes.unlock-rank", "c");
        Rank unlockRank = plugin.getRankManager().getRankById(unlockRankId);

        if (unlockRank == null || currentRank.getLevelRequirement() < unlockRank.getLevelRequirement()) {
            String msg = plugin.getMessage("error.class-not-unlocked").replace("%rank%", unlockRank != null ? unlockRank.getDisplayName() : unlockRankId);
            player.sendMessage(ColorUtils.colorize(msg));
            return true;
        }

        // Apre GUI selezione
        ClassSelectionGUI gui = new ClassSelectionGUI(plugin);
        gui.openGUI(player);

        return true;
    }
}
