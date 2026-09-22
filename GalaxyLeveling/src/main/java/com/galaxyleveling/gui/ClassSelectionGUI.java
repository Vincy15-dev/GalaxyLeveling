package com.galaxyleveling.gui;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerClass;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.service.ClassManager;
import com.galaxyleveling.service.PlayerProfileService;
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
import java.util.UUID;

public class ClassSelectionGUI implements Listener, InventoryHolder {

    private final GalaxyLeveling plugin;
    private static final int INVENTORY_SIZE = 27;
    private static final String TITLE = "&b&lSELEZIONE CLASSE";
    
    // Conferma pending per giocatore
    private final java.util.Map<UUID, String> pendingClassSelection = new java.util.HashMap<>();

    public ClassSelectionGUI(GalaxyLeveling plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, INVENTORY_SIZE, ColorUtils.colorize(TITLE));
    }

    public void openGUI(Player player) {
        Inventory inv = getInventory();
        PlayerProfileService profileService = plugin.getPlayerProfileService();
        PlayerProfile profile = profileService.getProfile(player.getUniqueId());
        
        if (profile == null) return;

        ClassManager classManager = plugin.getClassManager();
        int slot = 0;

        for (PlayerClass pClass : classManager.getAllClasses().values()) {
            if (slot >= 8) break; // Lascia ultimo slot per info
            
            ItemStack item = createClassItem(pClass, profile);
            inv.setItem(slot, item);
            slot++;
        }
        
        // Slot info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&6&lINFORMAZIONI"));
            meta.setLore(List.of(
                ColorUtils.colorize("&7Scegli una classe permanentemente."),
                ColorUtils.colorize("&cLa scelta non può essere annullata!"),
                ColorUtils.colorize(""),
                ColorUtils.colorize("&eClicca su una classe per selezionarla.")
            ));
        }
        info.setItemMeta(meta);
        inv.setItem(8, info);

        player.openInventory(inv);
    }

    private ItemStack createClassItem(PlayerClass pClass, PlayerProfile profile) {
        Material material = pClass.getIconMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(pClass.getColorGradient() + pClass.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" + pClass.getDescription()));
            lore.add(ColorUtils.colorize(""));
            lore.add(ColorUtils.colorize("&6Bonus Statistiche:"));
            pClass.getStatBonuses().forEach((stat, mult) -> 
                lore.add(ColorUtils.colorize("  &f- " + stat.getDisplayName() + ": &ax" + String.format("%.2f", mult)))
            );
            lore.add(ColorUtils.colorize(""));
            
            if (profile.getChosenClassId() != null && profile.getChosenClassId().equals(pClass.getId())) {
                lore.add(ColorUtils.colorize("&a&lCLASSE ATTUALE"));
            } else if (pendingClassSelection.containsValue(pClass.getId())) {
                lore.add(ColorUtils.colorize("&e&lCONFERMA SELEZIONE?"));
                lore.add(ColorUtils.colorize("&7Clicca di nuovo per confermare."));
            } else {
                lore.add(ColorUtils.colorize("&eClicca per selezionare."));
            }
            
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
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        if (slot == 8) return; // Slot info
        
        PlayerProfileService profileService = plugin.getPlayerProfileService();
        PlayerProfile profile = profileService.getProfile(player.getUniqueId());
        if (profile == null) return;
        
        // Se ha già una classe, non può cambiare
        if (profile.getChosenClassId() != null) {
            player.sendMessage(ColorUtils.colorize("&cHai già scelto una classe permanentemente."));
            player.closeInventory();
            return;
        }
        
        ClassManager classManager = plugin.getClassManager();
        var classes = classManager.getAllClasses().values();
        int index = 0;
        for (PlayerClass pClass : classes) {
            if (index == slot) {
                String pending = pendingClassSelection.get(player.getUniqueId());
                if (pending != null && pending.equals(pClass.getId())) {
                    // Conferma selezione
                    profile.setChosenClassId(pClass.getId());
                    plugin.getStatEngine().recomputeAllModifiers(player);
                    player.sendMessage(ColorUtils.colorize("&aClasse " + pClass.getDisplayName() + " selezionata permanentemente!"));
                    pendingClassSelection.remove(player.getUniqueId());
                    player.closeInventory();
                } else {
                    // Prima selezione - mostra conferma
                    pendingClassSelection.put(player.getUniqueId(), pClass.getId());
                    player.sendMessage(ColorUtils.colorize("&eClicca di nuovo su " + pClass.getDisplayName() + " per confermare."));
                    openGUI(player); // Ricarica per mostrare stato pending
                }
                return;
            }
            index++;
        }
    }
}
