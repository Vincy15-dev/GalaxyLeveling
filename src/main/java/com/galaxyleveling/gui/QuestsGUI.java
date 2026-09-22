package com.galaxyleveling.gui;

import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.PlayerProfile;
import com.galaxyleveling.model.Quest;
import com.galaxyleveling.model.QuestProgress;
import com.galaxyleveling.service.QuestManager;
import com.galaxyleveling.service.QuestProgressService;
import com.galaxyleveling.util.ColorUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * QUESTS GUI con tabs: Giornaliere / Settimanali / Storia / Prove di Rango.
 * Identificata tramite InventoryHolder.
 */
public class QuestsGUI implements InventoryHolder {

    public enum QuestTab {
        DAILY("Giornaliere", Material.CLOCK),
        WEEKLY("Settimanali", Material.CALENDAR),
        STORY("Storia", Material.WRITTEN_BOOK),
        TRIAL("Prove di Rango", Material.DIAMOND_SWORD);
        
        private final String displayName;
        private final Material icon;
        
        QuestTab(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
        
        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
    }
    
    private final GalaxyLeveling plugin;
    private Inventory inventory;
    private final Map<UUID, QuestTab> playerCurrentTab = new HashMap<>();

    public QuestsGUI(GalaxyLeveling plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void openGUI(Player player) {
        openGUI(player, QuestTab.DAILY);
    }
    
    public void openGUI(Player player, QuestTab initialTab) {
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ColorUtils.colorize(plugin.getMessages().getString("error.profile-not-loaded")));
            return;
        }
        
        playerCurrentTab.put(player.getUniqueId(), initialTab);
        inventory = Bukkit.createInventory(this, 54, ColorUtils.colorize("&b&lMISSIONI"));
        
        fillBackground();
        setTabButtons(player, initialTab);
        setQuestList(player, profile, initialTab);
        
        player.openInventory(inventory);
    }
    
    private void fillBackground() {
        ItemStack glass = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
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
    
    private void setTabButtons(Player player, QuestTab currentTab) {
        int slot = 0;
        for (QuestTab tab : QuestTab.values()) {
            boolean isActive = tab == currentTab;
            ItemStack item = new ItemStack(isActive ? Material.GOLD_BLOCK : Material.IRON_BLOCK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                ChatColor color = isActive ? ChatColor.GOLD : ChatColor.WHITE;
                meta.setDisplayName(ColorUtils.colorize(color + "&l" + tab.getDisplayName()));
                
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.colorize("&7Clicca per visualizzare"));
                lore.add(ColorUtils.colorize("&7le missioni " + tab.getDisplayName().toLowerCase() + "."));
                if (isActive) {
                    lore.add("");
                    lore.add(ColorUtils.colorize("&a&l[SELEZIONATO]"));
                }
                
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }
        
        // Slot 8: Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta meta = close.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&c&lChiudi"));
            close.setItemMeta(meta);
        }
        inventory.setItem(8, close);
    }
    
    private void setQuestList(Player player, PlayerProfile profile, QuestTab tab) {
        QuestManager qm = plugin.getQuestManager();
        QuestProgressService qps = plugin.getQuestProgressService();
        
        List<Quest> quests = new ArrayList<>();
        switch (tab) {
            case DAILY:
                quests = qm.getQuestsByCategory("DAILY");
                break;
            case WEEKLY:
                quests = qm.getQuestsByCategory("WEEKLY");
                break;
            case STORY:
                quests = qm.getQuestsByCategory("STORY");
                break;
            case TRIAL:
                quests = qm.getQuestsByCategory("TRIAL");
                break;
        }
        
        // Ordina per livello richiesto
        quests.sort(Comparator.comparingInt(Quest::getMinLevel));
        
        int startSlot = 9;
        for (Quest quest : quests) {
            if (startSlot >= 54) break;
            
            boolean isLocked = isQuestLocked(player, profile, quest);
            QuestProgress progress = qps.getQuestProgress(player, quest.getId());
            
            ItemStack item = createQuestItem(quest, progress, isLocked, profile);
            inventory.setItem(startSlot++, item);
        }
    }
    
    private boolean isQuestLocked(Player player, PlayerProfile profile, Quest quest) {
        // Check livello minimo
        if (profile.getLevel() < quest.getMinLevel()) return true;
        
        // Check grado minimo
        if (quest.getMinRank() != null) {
            if (profile.getCurrentRank() == null || 
                profile.getCurrentRank().getId().compareTo(quest.getMinRank()) < 0) {
                return true;
            }
        }
        
        // Check classe richiesta
        if (quest.getRequiresClass() != null) {
            if (profile.getChosenClass() == null || 
                !profile.getChosenClass().getId().equals(quest.getRequiresClass())) {
                return true;
            }
        }
        
        // Check prerequisiti
        if (quest.getPrerequisiteQuestId() != null) {
            QuestProgress prereqProgress = plugin.getQuestProgressService()
                .getQuestProgress(player, quest.getPrerequisiteQuestId());
            if (prereqProgress == null || !prereqProgress.isCompleted()) {
                return true;
            }
        }
        
        return false;
    }
    
    private ItemStack createQuestItem(Quest quest, QuestProgress progress, boolean isLocked, PlayerProfile profile) {
        Material material = quest.getIconMaterial() != null ? 
            quest.getIconMaterial() : Material.PAPER;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ChatColor color = isLocked ? ChatColor.GRAY : ChatColor.WHITE;
            meta.setDisplayName(ColorUtils.colorize(color + quest.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            
            if (isLocked) {
                lore.add(ColorUtils.colorize("&c&lBLOCCATA"));
                lore.add("");
                // Motivo specifico
                if (profile.getLevel() < quest.getMinLevel()) {
                    lore.add(ColorUtils.colorize("&7Livello richiesto: &c" + quest.getMinLevel()));
                    lore.add(ColorUtils.colorize("&7Il tuo livello: &e" + profile.getLevel()));
                } else if (quest.getMinRank() != null && 
                          (profile.getCurrentRank() == null || profile.getCurrentRank().getId().compareTo(quest.getMinRank()) < 0)) {
                    lore.add(ColorUtils.colorize("&7Grado richiesto: &c" + quest.getMinRank()));
                } else if (quest.getRequiresClass() != null && 
                          (profile.getChosenClass() == null || !profile.getChosenClass().getId().equals(quest.getRequiresClass()))) {
                    lore.add(ColorUtils.colorize("&7Classe richiesta: &c" + quest.getRequiresClass()));
                } else if (quest.getPrerequisiteQuestId() != null) {
                    lore.add(ColorUtils.colorize("&7Prerequisito: &cCompleta prima \"" + 
                        plugin.getQuestManager().getQuestById(quest.getPrerequisiteQuestId()).getDisplayName() + "\""));
                }
            } else {
                lore.add(ColorUtils.colorize("&7Tipo: &f" + quest.getType()));
                lore.add(ColorUtils.colorize("&7Obiettivo: &f" + quest.getTarget() + " x" + quest.getAmount()));
                lore.add("");
                
                if (progress != null && !progress.isCompleted()) {
                    double percent = quest.getAmount() > 0 ? (double) progress.getProgress() / quest.getAmount() : 0;
                    String bar = ColorUtils.createProgressBar(percent, 15, "&a", "&7");
                    lore.add(ColorUtils.colorize("&7Progresso: " + bar));
                    lore.add(ColorUtils.colorize("&f" + progress.getProgress() + "&7/&f" + quest.getAmount()));
                } else if (progress != null && progress.isCompleted()) {
                    lore.add(ColorUtils.colorize("&a&lCOMPLETATA!"));
                    lore.add(ColorUtils.colorize("&eClicca per reclamare la ricompensa."));
                } else {
                    lore.add(ColorUtils.colorize("&eClicca per accettare."));
                }
                
                lore.add("");
                lore.add(ColorUtils.colorize("&6Ricompense:"));
                lore.add(ColorUtils.colorize("  &b+ " + quest.getXpReward() + " XP"));
                if (quest.getStatPointsReward() > 0) {
                    lore.add(ColorUtils.colorize("  &d+ " + quest.getStatPointsReward() + " Punti Stat"));
                }
                if (!quest.getCommandRewards().isEmpty()) {
                    lore.add(ColorUtils.colorize("  &7Comandi speciali"));
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof QuestsGUI)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        PlayerProfile profile = plugin.getPlayerProfileService().getProfile(player.getUniqueId());
        if (profile == null) return;
        
        QuestTab currentTab = playerCurrentTab.get(player.getUniqueId());
        if (currentTab == null) currentTab = QuestTab.DAILY;
        
        // Click su tab (slot 0-3)
        if (slot >= 0 && slot <= 3) {
            QuestTab newTab = QuestTab.values()[slot];
            if (newTab != currentTab) {
                openGUI(player, newTab);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            }
            return;
        }
        
        // Click su chiudi (slot 8)
        if (slot == 8) {
            player.closeInventory();
            return;
        }
        
        // Click su quest (slot 9+)
        if (slot >= 9) {
            QuestManager qm = plugin.getQuestManager();
            QuestProgressService qps = plugin.getQuestProgressService();
            
            // Trova la quest corrispondente
            List<Quest> quests = qm.getQuestsByCategory(currentTab.name());
            int questIndex = slot - 9;
            if (questIndex < quests.size()) {
                Quest quest = quests.get(questIndex);
                
                if (isQuestLocked(player, profile, quest)) {
                    player.sendMessage(ColorUtils.colorize("&cQuesta missione è bloccata per te."));
                    return;
                }
                
                QuestProgress progress = qps.getQuestProgress(player, quest.getId());
                if (progress == null) {
                    // Accetta la quest
                    qps.acceptQuest(player, quest);
                    player.sendMessage(ColorUtils.colorize("&aMissione accettata: &f" + quest.getDisplayName()));
                    openGUI(player, currentTab);
                } else if (progress.isCompleted()) {
                    // Reclama ricompensa
                    qps.claimReward(player, quest);
                    openGUI(player, currentTab);
                } else {
                    player.sendMessage(ColorUtils.colorize("&7Missione in corso: " + 
                        progress.getProgress() + "/" + quest.getAmount()));
                }
            }
        }
    }
}
