package com.galaxyleveling.command;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.model.StatDefinition;
import com.galaxyleveling.service.PlayerProfileService;
import com.galaxyleveling.service.StatEngine;
import com.galaxyleveling.service.XpService;
import com.galaxyleveling.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SistemaCommand implements CommandExecutor, TabCompleter {

    private final GalaxyLeveling plugin;

    public SistemaCommand(GalaxyLeveling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("system.admin")) {
            sender.sendMessage(ColorUtils.colorize(plugin.getMessage("error.no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ColorUtils.colorize("&6Comandi disponibili:"));
            sender.sendMessage(ColorUtils.colorize("&e/system reload &7- Ricarica configurazioni"));
            sender.sendMessage(ColorUtils.colorize("&e/system give <player> xp <amount> &7- Dai XP"));
            sender.sendMessage(ColorUtils.colorize("&e/system give <player> points <amount> &7- Dai punti statistica"));
            sender.sendMessage(ColorUtils.colorize("&e/system setlevel <player> <level> &7- Imposta livello"));
            sender.sendMessage(ColorUtils.colorize("&e/system setstat <player> <stat> <value> &7- Imposta statistica"));
            sender.sendMessage(ColorUtils.colorize("&e/system resetstats <player> &7- Resetta statistiche"));
            sender.sendMessage(ColorUtils.colorize("&e/system setclass <player> <classId> &7- Imposta classe"));
            return true;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "reload":
                plugin.reloadConfig();
                plugin.getRankManager().reloadRanks();
                plugin.getClassManager().reloadClasses();
                plugin.getQuestManager().reloadQuests();
                sender.sendMessage(ColorUtils.colorize("&aConfigurazioni ricaricate con successo!"));
                break;

            case "give":
                handleGive(sender, args);
                break;

            case "setlevel":
                handleSetLevel(sender, args);
                break;

            case "setstat":
                handleSetStat(sender, args);
                break;

            case "resetstats":
                handleResetStats(sender, args);
                break;

            case "setclass":
                handleSetClass(sender, args);
                break;

            default:
                sender.sendMessage(ColorUtils.colorize("&cSottocomando non valido. Usa /system per la lista."));
                break;
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&cUso: /system give <player> xp|points <amount>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtils.colorize(plugin.getMessage("error.invalid-target-player").replace("%player%", args[1])));
            return;
        }

        String type = args[2].toLowerCase();
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cQuantità non valida."));
            return;
        }

        PlayerProfileService profileService = plugin.getPlayerProfileService();
        if (!profileService.isProfileReady(target.getUniqueId())) {
            sender.sendMessage(ColorUtils.colorize(plugin.getMessage("error.profile-not-loaded")));
            return;
        }

        if (type.equals("xp")) {
            XpService xpService = plugin.getXpService();
            xpService.grantXp(target, amount, "ADMIN_COMMAND");
            sender.sendMessage(ColorUtils.colorize("&aDati " + amount + " XP a " + target.getName()));
        } else if (type.equals("points")) {
            PlayerProfile profile = profileService.getProfile(target.getUniqueId());
            profile.addUnspentStatPoints(amount);
            sender.sendMessage(ColorUtils.colorize("&aDati " + amount + " punti statistica a " + target.getName()));
        } else {
            sender.sendMessage(ColorUtils.colorize("&cTipo non valido. Usa 'xp' o 'points'."));
        }
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtils.colorize("&cUso: /system setlevel <player> <level>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtils.colorize(plugin.getMessage("error.invalid-target-player").replace("%player%", args[1])));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cLivello non valido."));
            return;
        }

        PlayerProfileService profileService = plugin.getPlayerProfileService();
        if (!profileService.isProfileReady(target.getUniqueId())) {
            sender.sendMessage(ColorUtils.colorize(plugin.getMessage("error.profile-not-loaded")));
            return;
        }

        PlayerProfile profile = profileService.getProfile(target.getUniqueId());
        StatEngine statEngine = plugin.getStatEngine();
        
        // Calcola XP totale necessario per quel livello
        long totalXpForLevel = 0;
        for (int l = 1; l < level; l++) {
            totalXpForLevel += statEngine.getXpForLevel(l);
        }
        
        profile.setTotalXp(totalXpForLevel);
        profile.setLevel(level);
        statEngine.recomputeAllModifiers(target);
        
        sender.sendMessage(ColorUtils.colorize("&aLivello di " + target.getName() + " impostato a " + level));
        target.sendMessage(ColorUtils.colorize("&6Il tuo livello è stato impostato a " + level + " da un amministratore."));
    }

    private void handleSetStat(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&cUso: /system setstat <player> <stat> <value>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtils.colorize(plugin.getMessage("error.invalid-target-player").replace("%player%", args[1])));
            return;
        }

        StatDefinition stat = StatDefinition.fromId(args[2].toLowerCase());
        if (stat == null) {
            sender.sendMessage(ColorUtils.colorize("&cStatistica non valida. Usa: forza, agilita, vitalita, intelligenza, percezione"));
            return;
        }

        int value;
        try {
            value = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cValore non valido."));
            return;
        }

        PlayerProfileService profileService = plugin.getPlayerProfileService();
        if (!profileService.isProfileReady(target.getUniqueId())) {
            sender.sendMessage(ColorUtils.colorize(plugin.getMessage("error.profile-not-loaded")));
            return;
        }

        PlayerProfile profile = profileService.getProfile(target.getUniqueId());
        profile.setStatValue(stat, value);
        plugin.getStatEngine().recomputeAllModifiers(target);

        sender.sendMessage(ColorUtils.colorize("&a" + stat.getDisplayName() + " di " + target.getName() + " impostata a " + value));
    }

    private void handleResetStats(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUso: /system resetstats <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtils.colorize(plugin.getMessage("error.invalid-target-player").replace("%player%", args[1])));
            return;
        }

        PlayerProfileService profileService = plugin.getPlayerProfileService();
        if (!profileService.isProfileReady(target.getUniqueId())) {
            sender.sendMessage(ColorUtils.colorize(plugin.getMessage("error.profile-not-loaded")));
            return;
        }

        PlayerProfile profile = profileService.getProfile(target.getUniqueId());
        profile.resetStats();
        plugin.getStatEngine().recomputeAllModifiers(target);

        sender.sendMessage(ColorUtils.colorize("&aStatistiche di " + target.getName() + " resettate."));
        target.sendMessage(ColorUtils.colorize("&cLe tue statistiche sono state resettate da un amministratore."));
    }

    private void handleSetClass(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtils.colorize("&cUso: /system setclass <player> <classId>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtils.colorize(plugin.getMessage("error.invalid-target-player").replace("%player%", args[1])));
            return;
        }

        String classId = args[2].toLowerCase();
        if (plugin.getClassManager().getClassById(classId) == null) {
            sender.sendMessage(ColorUtils.colorize("&cClasse non trovata: " + classId));
            return;
        }

        PlayerProfileService profileService = plugin.getPlayerProfileService();
        if (!profileService.isProfileReady(target.getUniqueId())) {
            sender.sendMessage(ColorUtils.colorize(plugin.getMessage("error.profile-not-loaded")));
            return;
        }

        PlayerProfile profile = profileService.getProfile(target.getUniqueId());
        profile.setChosenClassId(classId);
        plugin.getStatEngine().recomputeAllModifiers(target);

        sender.sendMessage(ColorUtils.colorize("&aClasse di " + target.getName() + " impostata a " + classId));
        target.sendMessage(ColorUtils.colorize("&6La tua classe è stata impostata a " + classId + " da un amministratore."));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "give", "setlevel", "setstat", "resetstats", "setclass")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && Arrays.asList("give", "setlevel", "setstat", "resetstats", "setclass").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("xp", "points")
                    .stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setstat")) {
            return Arrays.asList("forza", "agilita", "vitalita", "intelligenza", "percezione")
                    .stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setclass")) {
            return plugin.getClassManager().getAllClasses().keySet().stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
