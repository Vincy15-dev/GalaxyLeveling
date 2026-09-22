package com.example.plugin.sistema.gui;

import com.example.plugin.sistema.SistemaPlugin;
import com.example.plugin.sistema.models.PlayerClass;
import com.example.plugin.sistema.models.PlayerProfile;
import com.example.plugin.sistema.models.StatDefinition;
import com.example.plugin.sistema.utils.ColorUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI per la selezione della classe (permanente) o visualizzazione info se già scelta.
 * Design: tema olografico blu/ciano con glass pane.
 */
public class ClassSelectionGUI implements InventoryHolder, Listener {

    private final SistemaPlugin plugin;
    private final Player viewer;
    private final PlayerProfile profile;
    private Inventory inventory;
    
    private static final int SIZE = 54; // 6 righe
    private static final String TITLE = ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "SELEZIONE CLASSE";

    // Slot layout
    private static final int[] CLASS_SLOTS = {10, 12, 14, 16}; // 4 classi max
    private static final int CONFIRM_SLOT = 31; // Centro
    private static final int CANCEL_SLOT = 49; // Bottom center
    private static final int INFO_SLOT = 4; // Top center

    private PlayerClass selectedClass = null;
    private boolean isConfirming = false;

    public ClassSelectionGUI(SistemaPlugin plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.profile = plugin.getPlayerProfileService().getProfile(viewer.getUniqueId());
        
        if (profile == null) {
            viewer.sendMessage(plugin.getMessage("error.profile-not-loaded"));
            return;
        }

        init();
    }

    private void init() {
        inventory = Bukkit.createInventory(this, SIZE, TITLE);
        setupGlassBorder();
        setupContent();
    }

    private void setupGlassBorder() {
        ItemStack glass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }

        // Border superiore e inferiore
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, glass); // Top row
            inventory.setItem(SIZE - 9 + i, glass); // Bottom row
        }
        
        // Border laterali
        for (int i = 0; i < 6; i++) {
            inventory.setItem(i * 9, glass); // Left column
            inventory.setItem(i * 9 + 8, glass); // Right column
        }
    }

    private void setupContent() {
        // Titolo/info centrale
        ItemStack infoItem = createInfoItem();
        inventory.setItem(INFO_SLOT, infoItem);

        // Controlla se il giocatore ha già una classe
        PlayerClass currentClass = profile.getChosenClass();
        
        if (currentClass != null) {
            // Modalità INFO: mostra solo la classe corrente
            showClassInfo(currentClass);
            return;
        }

        // Modalità SELEZIONE: mostra tutte le classi disponibili
        List<PlayerClass> classes = new ArrayList<>(plugin.getClassManager().getAllClasses());
        
        for (int i = 0; i < Math.min(classes.size(), CLASS_SLOTS.length); i++) {
            PlayerClass pClass = classes.get(i);
            ItemStack classIcon = createClassIcon(pClass, false);
            inventory.setItem(CLASS_SLOTS[i], classIcon);
        }

        // Istruzioni
        viewer.sendMessage(ColorUtils.color("&7Clicca su una classe per selezionarla. &c&lATTENZIONE: La scelta è permanente!"));
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        String titleText;
        if (profile.getChosenClass() != null) {
            titleText = "&b&lLA TUA CLASSE";
        } else {
            titleText = "&b&lSCEGLI IL TUO PERCORSO";
        }
        
        if (meta != null) {
            meta.setDisplayName(ColorUtils.color(titleText));
            List<String> lore = new ArrayList<>();
            
            if (profile.getChosenClass() == null) {
                lore.add("");
                lore.add(ColorUtils.color("&7Sei al bivio del tuo destino."));
                lore.add(ColorUtils.color("&7Scegli saggiamente la tua specializzazione."));
                lore.add("");
                lore.add(ColorUtils.color("&eClicca su un'icona per vedere i dettagli."));
                lore.add("");
                lore.add(ColorUtils.color("&c&lIMPORTANTE:"));
                lore.add(ColorUtils.color("&cLa scelta è PERMANENTE."));
                lore.add(ColorUtils.color("&cNon potrai cambiare classe in seguito."));
            } else {
                lore.add("");
                lore.add(ColorUtils.color("&7Hai già scelto il tuo percorso."));
                lore.add(ColorUtils.color("&7Questa è la tua specializzazione:"));
                lore.add("");
                PlayerClass current = profile.getChosenClass();
                lore.add(ColorUtils.color(current.getDisplayName()));
                lore.add(ColorUtils.color("&8" + current.getPassiveText()));
            }
            
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private void showClassInfo(PlayerClass currentClass) {
        // Mostra la classe corrente con tutti i dettagli
        ItemStack classIcon = createClassIcon(currentClass, true);
        inventory.setItem(CONFIRM_SLOT, classIcon);
        
        // Aggiungi item per chiudere
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = closeItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.color("&c&lChiudi"));
            List<String> lore = Arrays.asList("", ColorUtils.color("&7Torna al gioco."));
            meta.setLore(lore);
            closeItem.setItemMeta(meta);
        }
        inventory.setItem(CANCEL_SLOT, closeItem);
    }

    private ItemStack createClassIcon(PlayerClass pClass, boolean isLockedIn) {
        Material material = Material.matchMaterial(pClass.getIconMaterial());
        if (material == null) {
            material = Material.BOOK;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Applica gradiente colore al nome
        String displayName = ColorUtils.applyGradient(
            pClass.getColorGradient(), 
            pClass.getDisplayName()
        );
        
        if (isLockedIn) {
            displayName += ChatColor.RESET + " " + ChatColor.GOLD + "(TUO)";
        }
        
        if (meta != null) {
            meta.setDisplayName(displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            
            // Descrizione
            for (String line : pClass.getDescription()) {
                lore.add(ColorUtils.color("&7" + line));
            }
            
            lore.add("");
            lore.add(ColorUtils.color("&b&lBONUS STATISTICHE:"));
            
            // Mostra bonus statistiche con calcolo dell'effetto reale
            for (Map.Entry<String, Double> entry : pClass.getStatBonusMap().entrySet()) {
                StatDefinition stat = StatDefinition.fromId(entry.getKey());
                if (stat != null) {
                    double multiplier = entry.getValue();
                    String bonusText = multiplier > 1.0 
                        ? "&a+" + ((multiplier - 1.0) * 100) + "%" 
                        : multiplier < 1.0 
                            ? "&c-" + ((1.0 - multiplier) * 100) + "%" 
                            : "&7+0%";
                    
                    lore.add(ColorUtils.color("  " + stat.getDisplayName() + ": " + bonusText));
                }
            }
            
            lore.add("");
            if (pClass.getFocusRegenBonus() > 0) {
                lore.add(ColorUtils.color("&9Focus Regen: &b+" + pClass.getFocusRegenBonus() + "/s"));
                lore.add("");
            }
            
            // Passiva
            if (!pClass.getPassiveText().isEmpty()) {
                lore.add(ColorUtils.color("&e&lPassiva:"));
                lore.add(ColorUtils.color("&7" + pClass.getPassiveText()));
                lore.add("");
            }
            
            if (!isLockedIn) {
                lore.add(ColorUtils.color("&eClicca per selezionare"));
                lore.add(ColorUtils.color("&c&l[Sarà richiesta conferma]"));
            } else {
                lore.add(ColorUtils.color("&a&lClasse Attiva"));
            }
            
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ClassSelectionGUI)) return;
        
        event.setCancelled(true); // Cancel all clicks by default
        
        Player clicker = (Player) event.getWhoClicked();
        if (clicker != viewer) return; // Solo il viewer può interagire
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        // Se il profilo non è caricato, blocca tutto
        if (profile == null) {
            clicker.sendMessage(plugin.getMessage("error.profile-not-loaded"));
            close();
            return;
        }

        // Se ha già una classe, solo il tasto chiudi funziona
        if (profile.getChosenClass() != null) {
            if (slot == CANCEL_SLOT) {
                close();
            }
            return;
        }

        // Controlla se ha raggiunto il rank di sblocco
        if (!plugin.getClassManager().isUnlockRankReached(profile.getCurrentRank())) {
            clicker.sendMessage(plugin.getMessage("error.class-not-unlocked")
                .replace("%rank%", plugin.getClassManager().getUnlockRankId()));
            close();
            return;
        }

        // Gestione selezione classe
        for (int i = 0; i < CLASS_SLOTS.length; i++) {
            if (slot == CLASS_SLOTS[i]) {
                List<PlayerClass> classes = new ArrayList<>(plugin.getClassManager().getAllClasses());
                if (i < classes.size()) {
                    selectedClass = classes.get(i);
                    showConfirmation(selectedClass);
                }
                return;
            }
        }

        // Gestione conferma
        if (slot == CONFIRM_SLOT && isConfirming && selectedClass != null) {
            confirmClassSelection(selectedClass);
            return;
        }

        // Gestione annullamento
        if (slot == CANCEL_SLOT) {
            if (isConfirming) {
                // Torna alla selezione
                isConfirming = false;
                selectedClass = null;
                setupContent();
            } else {
                close();
            }
            return;
        }
    }

    private void showConfirmation(PlayerClass pClass) {
        isConfirming = true;
        
        // Pulisci inventory
        inventory.clear();
        setupGlassBorder();
        
        // Mostra icona della classe selezionata al centro
        ItemStack classIcon = createClassIcon(pClass, false);
        inventory.setItem(CONFIRM_SLOT, classIcon);
        
        // Item conferma
        ItemStack confirmItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = confirmItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.color("&a&lCONFERMA SELEZIONE"));
            List<String> lore = Arrays.asList(
                "",
                ColorUtils.color("&7Stai per scegliere:"),
                ColorUtils.color("&b" + pClass.getDisplayName()),
                "",
                ColorUtils.color("&c&lQUESTA SCELTA È PERMANENTE!"),
                ColorUtils.color("&cNon potrai cambiare classe in seguito."),
                "",
                ColorUtils.color("&eClicca per confermare definitivamente.")
            );
            meta.setLore(lore);
            confirmItem.setItemMeta(meta);
        }
        inventory.setItem(22, confirmItem); // Sopra al centro
        
        // Item annulla
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ColorUtils.color("&c&lANNULLA"));
            List<String> cancelLore = Arrays.asList(
                "",
                ColorUtils.color("&7Torna alla selezione delle classi."),
                ColorUtils.color("&7Nessuna modifica verrà applicata.")
            );
            cancelMeta.setLore(cancelLore);
            cancelItem.setItemMeta(cancelMeta);
        }
        inventory.setItem(CANCEL_SLOT, cancelItem);
        
        viewer.sendMessage(ColorUtils.color("&e&lCONFERMA RICHIESTA: &7Clicca sulla lana verde per confermare la tua scelta."));
    }

    private void confirmClassSelection(PlayerClass pClass) {
        profile.setChosenClass(pClass);
        
        // Ricalcola tutte le statistiche con i nuovi moltiplicatori di classe
        plugin.getStatEngine().recomputeAllModifiers(viewer);
        
        // Salva il profilo async
        plugin.getPlayerProfileService().saveProfile(profile, false);
        
        // Feedback
        viewer.sendMessage(ColorUtils.color("&a&lCLASSE SELEZIONATA: &b" + pClass.getDisplayName()));
        viewer.sendMessage(ColorUtils.color("&7I tuoi attributi sono stati aggiornati con i bonus della classe."));
        
        // Suono di conferma
        viewer.playSound(viewer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        
        close();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ClassSelectionGUI)) return;
        if (event.getPlayer() != viewer) return;
        
        // Se stava confermando ma ha chiuso senza confermare, annulla
        if (isConfirming && profile.getChosenClass() == null) {
            viewer.sendMessage(ColorUtils.color("&7Selezione classe annullata."));
        }
    }

    private void close() {
        viewer.closeInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Apre la GUI a un giocatore
     */
    public static void open(SistemaPlugin plugin, Player player) {
        ClassSelectionGUI gui = new ClassSelectionGUI(plugin, player);
        if (gui.getInventory() != null) {
            player.openInventory(gui.getInventory());
        }
    }
}
