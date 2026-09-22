package com.galaxyleveling.gui;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.model.StatDefinition;
import com.galaxyleveling.service.StatEngine;
import com.galaxyleveling.util.ColorUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * STATUS GUI transazionale: le modifiche alle statistiche sono solo pending fino al conferma.
 * Identificata tramite InventoryHolder, non per titolo.
 */
public class StatusGUI implements InventoryHolder {

    private final GalaxyLeveling plugin;
    private final Map<UUID, PendingAllocation> pendingAllocations = new HashMap<>();
    private Inventory inventory;

    // Classe interna per tracciare allocazioni pending
    public static class PendingAllocation {
        public Map<StatDefinition, Integer> pendingStats = new EnumMap<>(StatDefinition.class);
        public int totalPointsToSpend = 0;
        
        public PendingAllocation(int points) {
            this.totalPointsToSpend = points;
            for (StatDefinition stat : StatDefinition.values()) {
                pendingStats.put(stat, 0);
            }
        }
        
        public int getPendingFor(StatDefinition stat) {
            return pendingStats.getOrDefault(stat, 0);
        }
        
        public void addPending(StatDefinition stat, int amount) {
            pendingStats.put(stat, pendingStats.getOrDefault(stat, 0) + amount);
            totalPointsToSpend -= amount;
        }
        
        public void removePending(StatDefinition stat, int amount) {
            int current = pendingStats.getOrDefault(stat, 0);
            int toRemove = Math.min(current, amount);
            pendingStats.put(stat, current - toRemove);
            totalPointsToSpend += toRemove;
        }
        
        public int getTotalPending() {
            int sum = 0;
            for (int val : pendingStats.values()) sum += val;
            return sum;
        }
    }

    public StatusGUI(GalaxyLeveling plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Apre la GUI con stato corrente + allocazioni pending se esistenti.
     */
    public void openGUI(Player player) {
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ColorUtils.colorize(plugin.getMessages().getString("error.profile-not-loaded")));
            return;
        }

        int unspentPoints = profile.getUnspentStatPoints();
        // Recupera o crea allocazione pending
        PendingAllocation pending = pendingAllocations.computeIfAbsent(
            player.getUniqueId(), 
            k -> new PendingAllocation(unspentPoints)
        );
        
        // Se i punti non corrispondono (es. reload), resetta pending
        if (pending.totalPointsToSpend != unspentPoints && pending.getTotalPending() == 0) {
            pending.totalPointsToSpend = unspentPoints;
        }

        inventory = Bukkit.createInventory(this, 54, ColorUtils.colorize("&b&lSTATO DEL SISTEMA"));
        
        // Riempi sfondo con vetro blu scuro
        fillBackground();
        
        // Slot 0-8: Info giocatore (livello, XP, rank, classe, punti)
        setInfoRow(player, profile, pending);
        
        // Slot 9-17: Separatori / Header stats
        setStatHeaders();
        
        // Slot 18-35: Righe statistiche (5 righe x 5 colonne = 25 slot, ma ne usiamo 18-35 = 18 slot)
        // Ogni statistica ha: icona, valore attuale, effetto prossimo punto (con moltiplicatore classe)
        setStatRows(player, profile, pending);
        
        // Slot 36-44: Azioni (Conferma, Annulla, Info)
        setActionRow(pending);
        
        // Slot 45-53: Footer decorativo
        setFooter();
        
        player.openInventory(inventory);
    }
    
    private void fillBackground() {
        ItemStack glass = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        for (int i = 0; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glass);
            }
        }
    }
    
    private void setInfoRow(Player player, PlayerProfile profile, PendingAllocation pending) {
        // Slot 0: Player Head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&f&l" + player.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7Livello: &e" + profile.getLevel()));
            lore.add("");
            
            String rankDisplay = profile.getCurrentRank() != null ? 
                profile.getCurrentRank().getDisplayName() : "Nessuno";
            ChatColor rankColor = profile.getCurrentRank() != null ? 
                ChatColor.of(profile.getCurrentRank().getColor()) : ChatColor.GRAY;
            lore.add(ColorUtils.colorize("&7Grado: " + rankColor + rankDisplay));
            
            if (profile.getChosenClass() != null) {
                lore.add(ColorUtils.colorize("&7Classe: &d" + profile.getChosenClass().getDisplayName()));
            } else {
                lore.add(ColorUtils.colorize("&7Classe: &cNessuna (usa /classe)"));
            }
            
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        inventory.setItem(0, head);
        
        // Slot 1: XP Bar item (Experience Bottle)
        ItemStack xpItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        meta = xpItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&b&lProgresso XP"));
            long xpNeeded = plugin.getXpService().getXpForNextLevel(profile.getLevel());
            long currentXpInLevel = profile.getTotalXp() - plugin.getXpService().calculateTotalXpForLevel(profile.getLevel());
            double percent = xpNeeded > 0 ? (double) currentXpInLevel / xpNeeded : 0;
            String bar = ColorUtils.createProgressBar(percent, 20, "&e", "&7");
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&fXP Totale: &e" + profile.getTotalXp()));
            lore.add(ColorUtils.colorize("&fXP per livello " + (profile.getLevel() + 1) + ": &e" + currentXpInLevel + "&7/&e" + xpNeeded));
            lore.add("");
            lore.add(ColorUtils.colorize(bar));
            meta.setLore(lore);
            xpItem.setItemMeta(meta);
        }
        inventory.setItem(1, xpItem);
        
        // Slot 4: Punti disponibili (con pending evidenziato)
        ItemStack pointsItem = new ItemStack(Material.BOOK);
        meta = pointsItem.getItemMeta();
        if (meta != null) {
            int actualAvailable = pending.totalPointsToSpend;
            meta.setDisplayName(ColorUtils.colorize("&e&lPunti Statistica"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7Disponibili: &e" + actualAvailable));
            if (pending.getTotalPending() > 0) {
                lore.add(ColorUtils.colorize("&cIn attesa: &f" + pending.getTotalPending()));
                lore.add(ColorUtils.colorize("&7Clicca su una statistica per allocare."));
            } else {
                lore.add(ColorUtils.colorize("&7Nessuna modifica in sospeso."));
            }
            meta.setLore(lore);
            pointsItem.setItemMeta(meta);
        }
        inventory.setItem(4, pointsItem);
    }
    
    private void setStatHeaders() {
        String[] headers = {"Forza", "Agilità", "Vitalità", "Intelligenza", "Percezione"};
        ChatColor[] colors = {ChatColor.RED, ChatColor.AQUA, ChatColor.GREEN, ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE};
        
        for (int i = 0; i < 5; i++) {
            ItemStack header = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
            ItemMeta meta = header.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize(colors[i] + "&l" + headers[i]));
                header.setItemMeta(meta);
            }
            inventory.setItem(9 + i, header);
        }
    }
    
    private void setStatRows(Player player, PlayerProfile profile, PendingAllocation pending) {
        StatDefinition[] stats = StatDefinition.values();
        StatEngine statEngine = plugin.getStatEngine();
        
        for (int i = 0; i < stats.length; i++) {
            StatDefinition stat = stats[i];
            int currentValue = profile.getStatValue(stat);
            int pendingValue = pending.getPendingFor(stat);
            int effectiveValue = currentValue + pendingValue;
            
            // Calcola effetto del prossimo punto CON moltiplicatore classe
            double nextPointEffect = statEngine.calculateNextPointEffect(stat, player);
            
            ItemStack statItem = createStatItem(stat, currentValue, pendingValue, effectiveValue, nextPointEffect, pending.totalPointsToSpend > 0);
            inventory.setItem(18 + i, statItem);
        }
    }
    
    private ItemStack createStatItem(StatDefinition stat, int current, int pending, int effective, double nextEffect, boolean canAllocate) {
        Material material = stat.getDisplayMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ChatColor color = stat.getDisplayColor();
            meta.setDisplayName(ColorUtils.colorize(color + "&l" + stat.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7Valore attuale: &f" + current));
            if (pending != 0) {
                lore.add(ColorUtils.colorize("&eModifica: &f" + (pending > 0 ? "+" : "") + pending));
                lore.add(ColorUtils.colorize("&aNuovo valore: &f" + effective));
            }
            lore.add("");
            lore.add(ColorUtils.colorize("&7Effetto prossimo punto:"));
            lore.add(ColorUtils.colorize("&f" + stat.formatEffect(nextEffect)));
            lore.add("");
            
            if (canAllocate) {
                lore.add(ColorUtils.colorize("&e&l[+1] &7Clicca SX per +1"));
                lore.add(ColorUtils.colorize("&e&l[+5] &7Shift+Clicca SX per +5"));
                lore.add(ColorUtils.colorize("&c&l[-1] &7Clicca DX per rimuovere"));
            } else {
                lore.add(ColorUtils.colorize("&cNessun punto disponibile"));
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private void setActionRow(PendingAllocation pending) {
        // Slot 36: Conferma (solo se ci sono pending)
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = confirm.getItemMeta();
        if (meta != null) {
            boolean hasPending = pending.getTotalPending() > 0;
            meta.setDisplayName(ColorUtils.colorize(hasPending ? "&a&lCONFERMA" : "&7&lCONFERMA"));
            List<String> lore = new ArrayList<>();
            if (hasPending) {
                lore.add(ColorUtils.colorize("&7Applica &f" + pending.getTotalPending() + "&7 punti statistica."));
                lore.add(ColorUtils.colorize("&eClicca per confermare!"));
            } else {
                lore.add(ColorUtils.colorize("&7Nessuna modifica da confermare."));
            }
            meta.setLore(lore);
            confirm.setItemMeta(meta);
        }
        inventory.setItem(36, confirm);
        
        // Slot 40: Annulla
        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        meta = cancel.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&c&lANNULLA"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7Rimuovi tutte le modifiche pending."));
            lore.add(ColorUtils.colorize("&eClicca per annullare!"));
            meta.setLore(lore);
            cancel.setItemMeta(meta);
        }
        inventory.setItem(40, cancel);
        
        // Slot 44: Info
        ItemStack info = new ItemStack(Material.BOOK);
        meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&b&lINFO"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7Statistiche descrizioni."));
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        inventory.setItem(44, info);
    }
    
    private void setFooter() {
        // Solo decorativo
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof StatusGUI)) return;
        
        event.setCancelled(true); // Cancella tutti i click/drag
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) return;
        
        PendingAllocation pending = pendingAllocations.get(player.getUniqueId());
        if (pending == null) return;
        
        // Click su statistiche (slot 18-22)
        if (slot >= 18 && slot <= 22) {
            StatDefinition stat = StatDefinition.values()[slot - 18];
            int current = profile.getStatValue(stat);
            boolean isShift = event.isShiftClick();
            
            if (event.isLeftClick()) {
                int amount = isShift ? 5 : 1;
                // Controlla se può allocare
                int maxAllowed = Math.min(amount, pending.totalPointsToSpend);
                int currentPending = pending.getPendingFor(stat);
                int maxStat = plugin.getConfig().getInt("system.stats.max-value", 100);
                
                // Non superare il cap
                int spaceUntilCap = maxStat - (current + currentPending);
                if (spaceUntilCap <= 0) {
                    player.sendMessage(ColorUtils.colorize("&cHai raggiunto il massimo per questa statistica!"));
                    return;
                }
                
                int toAdd = Math.min(maxAllowed, spaceUntilCap);
                if (toAdd > 0 && pending.totalPointsToSpend >= toAdd) {
                    pending.addPending(stat, toAdd);
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                    openGUI(player); // Ricarica GUI
                }
            } else if (event.isRightClick()) {
                // Rimuovi pending
                int currentPending = pending.getPendingFor(stat);
                if (currentPending > 0) {
                    int toRemove = isShift ? 5 : 1;
                    pending.removePending(stat, toRemove);
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                    openGUI(player);
                }
            }
        }
        
        // Click su Conferma (slot 36)
        if (slot == 36) {
            if (pending.getTotalPending() > 0) {
                // Applica modifiche
                for (Map.Entry<StatDefinition, Integer> entry : pending.pendingStats.entrySet()) {
                    if (entry.getValue() > 0) {
                        profile.setStatValue(entry.getKey(), profile.getStatValue(entry.getKey()) + entry.getValue());
                    }
                }
                profile.setUnspentStatPoints(pending.totalPointsToSpend);
                
                // Salva async
                plugin.getPlayerProfileService().saveProfile(profile, false);
                
                // Ricomputa attributi
                plugin.getStatEngine().recomputeAllModifiers(player);
                
                player.sendMessage(ColorUtils.colorize("&aStatistiche aggiornate con successo!"));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                
                pendingAllocations.remove(player.getUniqueId());
                player.closeInventory();
            }
        }
        
        // Click su Annulla (slot 40)
        if (slot == 40) {
            pendingAllocations.remove(player.getUniqueId());
            player.sendMessage(ColorUtils.colorize("&cModifiche annullate."));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_WOOD_BREAK, 0.5f, 1f);
            player.closeInventory();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        // Se chiude senza confermare, rimuovi pending (già fatto implicitamente, ma puliamo cache)
        pendingAllocations.remove(player.getUniqueId());
    }
}
