package com.galaxyleveling.gui;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.Quest;
import com.galaxyleveling.model.QuestProgress;
import com.galaxyleveling.service.PlayerProfileService;
import com.galaxyleveling.service.QuestManager;
import com.galaxyleveling.service.QuestProgressService;
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

public class QuestsGUI implements Listener, InventoryHolder {

    private final GalaxyLeveling plugin;
    private static final int INVENTORY_SIZE = 54;
    private static final String TITLE = "&b&lMISSIONI";

    public QuestsGUI(GalaxyLeveling plugin) {
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
        var profile = profileService.getProfile(player.getUniqueId());
        if (profile == null) return;

        QuestManager questManager = plugin.getQuestManager();
        QuestProgressService progressService = plugin.getQuestProgressService();

        // Tab: Giornaliere, Settimanali, Storia, Prove di Rango
        // Per semplicità mostro tutte in sequenza con separatori
        
        int slot = 0;
        
        // Sezione DAILY
        addQuestSection(inv, questManager, progressService, player, Quest.Category.DAILY, slot);
        
        player.openInventory(inv);
    }

    private void addQuestSection(Inventory inv, QuestManager qm, QuestProgressService ps, Player player, Quest.Category category, int startSlot) {
        // Titolo sezione
        ItemStack sectionTitle = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = sectionTitle.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&6&l=== " + category.name() + " ==="));
        }
        sectionTitle.setItemMeta(meta);
        inv.setItem(startSlot, sectionTitle);
        
        int slot = startSlot + 1;
        for (Quest quest : qm.getQuestsByCategory(category)) {
            if (slot >= 53) break;
            
            QuestProgress progress = ps.getQuestProgress(player, quest.getId());
            boolean locked = isQuestLocked(quest, player);
            
            ItemStack item = createQuestItem(quest, progress, locked);
            inv.setItem(slot, item);
            slot++;
        }
    }

    private boolean isQuestLocked(Quest quest, Player player) {
        // Controlla livello minimo
        if (player.getLevel() < quest.getMinLevel()) return true;
        
        // Controlla grado minimo
        var profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile != null && quest.getRequiredRank() != null) {
            if (profile.getCurrentRank().getLevelRequirement() < quest.getRequiredRank().getLevelRequirement()) {
                return true;
            }
        }
        
        // Controlla classe richiesta
        if (quest.getRequiredClass() != null) {
            if (profile == null || profile.getChosenClassId() == null || !profile.getChosenClassId().equals(quest.getRequiredClass())) {
                return true;
            }
        }
        
        // Controlla prerequisiti
        if (quest.getPrerequisiteQuestId() != null) {
            var progress = plugin.getQuestProgressService().getQuestProgress(player, quest.getPrerequisiteQuestId());
            if (progress == null || !progress.isCompleted()) {
                return true;
            }
        }
        
        return false;
    }

    private ItemStack createQuestItem(Quest quest, QuestProgress progress, boolean locked) {
        Material material = quest.getIconMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&e" + quest.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7Categoria: &f" + quest.getCategory().name()));
            lore.add(ColorUtils.colorize("&7Livello minimo: &f" + quest.getMinLevel()));
            lore.add(ColorUtils.colorize(""));
            lore.add(ColorUtils.colorize("&f" + quest.getDescription()));
            lore.add(ColorUtils.colorize(""));
            
            if (locked) {
                lore.add(ColorUtils.colorize("&c&lBLOCCATA"));
                lore.add(ColorUtils.colorize("&7Raggiungi i requisiti per sbloccare."));
            } else if (progress != null && progress.isCompleted()) {
                lore.add(ColorUtils.colorize("&a&lCOMPLETATA"));
                lore.add(ColorUtils.colorize("&6Clicca per riscattare la ricompensa!"));
            } else if (progress != null) {
                lore.add(ColorUtils.colorize("&e&lIN CORSO"));
                lore.add(ColorUtils.colorize("&7Progresso: &f" + progress.getCurrentAmount() + "/" + quest.getTargetAmount()));
            } else {
                lore.add(ColorUtils.colorize("&a&lDISPONIBILE"));
                lore.add(ColorUtils.colorize("&7Clicca per accettare."));
            }
            
            lore.add(ColorUtils.colorize(""));
            lore.add(ColorUtils.colorize("&6Ricompense:"));
            lore.add(ColorUtils.colorize("  &bXP: &f" + quest.getXpReward()));
            if (quest.getStatPointsReward() > 0) {
                lore.add(ColorUtils.colorize("  &ePunti Stat: &f" + quest.getStatPointsReward()));
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
        
        if (slot < 1) return; // Salta titolo sezione
        
        // Logica accettazione/completamento missione
        // Implementazione semplificata
    }
}
