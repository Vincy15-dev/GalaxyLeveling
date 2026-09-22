package com.galaxyleveling.gui;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.model.StatDefinition;
import com.galaxyleveling.service.PlayerProfileService;
import com.galaxyleveling.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatusGUI implements Listener, InventoryHolder {

    private final GalaxyLeveling plugin;
    private static final int INVENTORY_SIZE = 54;
    private static final String TITLE = "&b&lSTATO DEL SISTEMA";
    
    // Allocazioni pending per giocatore (transazionale)
    private final Map<UUID, Map<StatDefinition, Integer>> pendingAllocations = new HashMap<>();

    public StatusGUI(GalaxyLeveling plugin) {
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

        // Pulisci pending allocations per questo giocatore
        pendingAllocations.remove(player.getUniqueId());

        // Riga 0: Info giocatore (level, XP, rank, punti non spesi)
        setInfoRow(inv, profile, player);

        // Righe 1-4: Statistiche (5 righe per 5 stat)
        setStatRows(inv, profile, player);

        // Riga 5: Bottoni Conferma/Annulla
        setActionButtons(inv, profile);

        player.openInventory(inv);
    }

    private void setInfoRow(Inventory inv, PlayerProfile profile, Player player) {
        // Slot 0: Player Head con info
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&f&l" + player.getName()));
            meta.setLore(java.util.Arrays.asList(
                ColorUtils.colorize("&eLivello: &f" + profile.getLevel()),
                ColorUtils.colorize("&7XP: &f" + profile.getTotalXp() + " / " + plugin.getStatEngine().getXpForLevel(profile.getLevel() + 1)),
                ColorUtils.colorize("&bGrado: &r" + profile.getCurrentRank().getColorHex() + profile.getCurrentRank().getDisplayName()),
                ColorUtils.colorize(""),
                ColorUtils.colorize("&6Punti disponibili: &e" + profile.getUnspentStatPoints())
            ));
        }
        item.setItemMeta(meta);
        inv.setItem(0, item);

        // Slot 1-8: Glass pane decorativi
        ItemStack filler = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 1; i <= 8; i++) {
            inv.setItem(i, filler);
        }
    }

    private void setStatRows(Inventory inv, PlayerProfile profile, Player player) {
        StatDefinition[] stats = StatDefinition.values();
        int startRow = 1; // Inizia dalla seconda riga

        for (int i = 0; i < stats.length; i++) {
            StatDefinition stat = stats[i];
            int currentVal = profile.getStatValue(stat);
            
            // Calcola effetto prossimo punto (con moltiplicatore classe)
            double nextPointEffect = plugin.getStatEngine().getEffectiveStatBonus(stat, currentVal + 1, player);
            double currentEffect = plugin.getStatEngine().getEffectiveStatBonus(stat, currentVal, player);
            double delta = nextPointEffect - currentEffect;

            ItemStack item = new ItemStack(stat.getDisplayMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize(stat.getDisplayName()));
                meta.setLore(java.util.Arrays.asList(
                    ColorUtils.colorize("&7Valore attuale: &f" + String.format("%.2f", currentEffect)),
                    ColorUtils.colorize("&7Punti assegnati: &e" + currentVal),
                    ColorUtils.colorize(""),
                    ColorUtils.colorize("&aEffetto prossimo punto: +" + String.format("%.2f", delta)),
                    ColorUtils.colorize(""),
                    ColorUtils.colorize("&eClicca SX: +1"),
                    ColorUtils.colorize("&eShift+Clicca SX: +5")
                ));
            }
            item.setItemMeta(meta);
            
            int slot = startRow * 9 + i;
            inv.setItem(slot, item);
        }
    }

    private void setActionButtons(Inventory inv, PlayerProfile profile) {
        // Bottone Conferma (slot 48)
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(ColorUtils.colorize("&a&lCONFERMA"));
            confirmMeta.setLore(java.util.Arrays.asList(
                ColorUtils.colorize("&7Applica le modifiche alle statistiche.")
            ));
        }
        confirm.setItemMeta(confirmMeta);
        inv.setItem(48, confirm);

        // Bottone Annulla (slot 50)
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ColorUtils.colorize("&c&lANNULLA"));
            cancelMeta.setLore(java.util.Arrays.asList(
                ColorUtils.colorize("&7Scarta le modifiche pending.")
            ));
        }
        cancel.setItemMeta(cancelMeta);
        inv.setItem(50, cancel);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory().getHolder() != this) return;

        event.setCancelled(true); // Cancella tutti i click di default

        Player player = (Player) event.getWhoClicked();
        PlayerProfileService profileService = plugin.getPlayerProfileService();
        
        if (!profileService.isProfileReady(player.getUniqueId())) return;
        
        PlayerProfile profile = profileService.getProfile(player.getUniqueId());
        int slot = event.getSlot();

        // Click su statistica (righe 1-4, slot 9-44 nelle colonne 0-8)
        if (slot >= 9 && slot <= 44) {
            int statIndex = slot % 9;
            if (statIndex < StatDefinition.values().length) {
                StatDefinition stat = StatDefinition.values()[statIndex];
                boolean shift = event.isShiftClick();
                int amount = shift ? 5 : 1;

                // Modifica pending allocation
                Map<StatDefinition, Integer> pending = pendingAllocations.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
                int currentPending = pending.getOrDefault(stat, 0);
                
                // Controlla se ha punti sufficienti
                int totalPending = pending.values().stream().mapToInt(Integer::intValue).sum();
                if (profile.getUnspentStatPoints() >= totalPending + amount) {
                    pending.put(stat, currentPending + amount);
                    player.sendMessage(ColorUtils.colorize("&aAggiunti " + amount + " punti pending a " + stat.getDisplayName()));
                } else {
                    player.sendMessage(ColorUtils.colorize("&cPunti insufficienti!"));
                }
            }
        }

        // Click su Conferma (slot 48)
        if (slot == 48) {
            Map<StatDefinition, Integer> pending = pendingAllocations.get(player.getUniqueId());
            if (pending != null && !pending.isEmpty()) {
                int totalSpent = pending.values().stream().mapToInt(Integer::intValue).sum();
                if (profile.getUnspentStatPoints() >= totalSpent) {
                    profile.addUnspentStatPoints(-totalSpent);
                    for (Map.Entry<StatDefinition, Integer> entry : pending.entrySet()) {
                        profile.addStatValue(entry.getKey(), entry.getValue());
                    }
                    plugin.getStatEngine().recomputeAllModifiers(player);
                    player.sendMessage(ColorUtils.colorize("&aStatistiche aggiornate con successo!"));
                    pendingAllocations.remove(player.getUniqueId());
                    openGUI(player); // Ricarica GUI
                }
            } else {
                player.sendMessage(ColorUtils.colorize("&cNessuna modifica da confermare."));
            }
        }

        // Click su Annulla (slot 50)
        if (slot == 50) {
            pendingAllocations.remove(player.getUniqueId());
            player.sendMessage(ColorUtils.colorize("&cModifiche annullate."));
            openGUI(player); // Ricarica GUI
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (event.getInventory().getHolder() != this) return;

        // Scarta modifiche pending alla chiusura
        pendingAllocations.remove(event.getPlayer().getUniqueId());
    }
}
