package com.galaxyleveling.gui;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.service.LeaderboardService;
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

public class LeaderboardGUI implements Listener, InventoryHolder {

    private final GalaxyLeveling plugin;
    private static final int INVENTORY_SIZE = 27;
    private static final String TITLE = "&b&lCLASSIFICA SISTEMA";

    public LeaderboardGUI(GalaxyLeveling plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, INVENTORY_SIZE, ColorUtils.colorize(TITLE));
    }

    public void openGUI(Player player) {
        Inventory inv = getInventory();

        LeaderboardService lbService = plugin.getLeaderboardService();
        var entries = lbService.getLeaderboard();

        int slot = 0;
        for (var entry : entries) {
            if (slot >= 9) break; // Mostra solo top 9 nella prima riga per estetica

            ItemStack item = createEntryItem(entry, slot + 1);
            inv.setItem(slot, item);
            slot++;
        }

        // Riempitivo con glass pane
        ItemStack filler = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int i = slot; i < INVENTORY_SIZE; i++) {
            inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    private ItemStack createEntryItem(LeaderboardService.LeaderboardEntry entry, int position) {
        Material material = switch (position) {
            case 1 -> Material.GOLD_BLOCK;
            case 2 -> Material.IRON_BLOCK;
            case 3 -> Material.BRONZE_BLOCK; // Non esiste in vanilla, uso COPPER_BLOCK
            default -> Material.SKULL_PLAYER;
        };
        
        if (position > 3) material = Material.PLAYER_HEAD;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&6#" + position + " &f" + entry.getPlayerName()));
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize(""));
            lore.add(ColorUtils.colorize("&eLivello: &f" + entry.getLevel()));
            lore.add(ColorUtils.colorize("&7XP Totale: &f" + entry.getTotalXp()));
            lore.add(ColorUtils.colorize(""));
            lore.add(ColorUtils.colorize(entry.getRankColorHex() + entry.getRankDisplayName()));
            
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory().getHolder() != this) return;

        event.setCancelled(true);
    }
}
