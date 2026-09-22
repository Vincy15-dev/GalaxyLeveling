package com.galaxyleveling.gui;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.leaderboard.LeaderboardService;
import com.galaxyleveling.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI Leaderboard: mostra top N giocatori da snapshot cache.
 * Identificata tramite InventoryHolder (non per titolo).
 */
public class LeaderboardGUI implements Listener, InventoryHolder {

    private final GalaxyLeveling plugin;
    private final LeaderboardService leaderboardService;
    private final int size = 27; // 3 righe

    public LeaderboardGUI(GalaxyLeveling plugin, LeaderboardService leaderboardService) {
        this.plugin = plugin;
        this.leaderboardService = leaderboardService;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, size, ColorUtils.colorize("&b&lCLASSIFICA"));
    }

    public void openGUI(Player player) {
        Inventory inv = getInventory();

        // Popola con voci leaderboard
        List<LeaderboardService.LeaderboardEntry> snapshot = leaderboardService.getSnapshot();
        
        for (int i = 0; i < Math.min(snapshot.size(), 9); i++) {
            LeaderboardService.LeaderboardEntry entry = snapshot.get(i);
            ItemStack item = createEntryItem(entry, i + 1);
            inv.setItem(i, item);
        }

        // Separatori nelle righe vuote
        ItemStack separator = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta sepMeta = separator.getItemMeta();
        if (sepMeta != null) {
            sepMeta.setDisplayName(" ");
            separator.setItemMeta(sepMeta);
        }
        for (int i = 9; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, separator);
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createEntryItem(LeaderboardService.LeaderboardEntry entry, int position) {
        Material material;
        if (position == 1) material = Material.GOLD_BLOCK;
        else if (position == 2) material = Material.IRON_BLOCK;
        else if (position == 3) material = Material.BRONZE_BLOCK; // Nota: BRONZE_BLOCK non esiste in vanilla, uso COPPER_BLOCK
        else material = Material.SKULL_PLAYER;

        if (material == Material.BRONZE_BLOCK) material = Material.COPPER_BLOCK;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String color = entry.getRankColor();
            meta.setDisplayName(ColorUtils.colorize(color + "#" + position + " " + entry.getPlayerName()));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&fLivello: &e" + entry.getLevel()));
            lore.add(ColorUtils.colorize("&fGrado: " + color + entry.getRankDisplay()));
            lore.add(ColorUtils.colorize("&fXP Totale: &6" + entry.getTotalXp()));
            lore.add("");
            lore.add(ColorUtils.colorize("&7Dati aggiornati ogni " + 
                (plugin.getConfig().getInt("system.ranks.update-interval", 300) / 60) + " minuti."));

            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory().getHolder() != this) return;

        event.setCancelled(true);
    }
}
