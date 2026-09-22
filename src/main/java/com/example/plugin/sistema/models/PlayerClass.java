package com.example.plugin.sistema.models;

import com.example.plugin.sistema.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Rappresenta una Classe/Specializzazione scelta dal giocatore.
 * I dati sono caricati da classes.yml.
 */
public class PlayerClass {

    private final String id;
    private final String displayName;
    private final Material iconMaterial;
    private final List<String> colorGradient; // Lista di 2 colori HEX per il gradiente
    private final List<String> description;
    private final Map<StatDefinition, Double> statBonus; // Moltiplicatori per statistica
    private final String passiveDescription;
    private final double focusRegenBonus; // Bonus alla rigenerazione del focus

    public PlayerClass(String id, String displayName, Material iconMaterial, 
                       List<String> colorGradient, List<String> description,
                       Map<StatDefinition, Double> statBonus, String passiveDescription, double focusRegenBonus) {
        this.id = id;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.colorGradient = colorGradient != null ? colorGradient : Arrays.asList("#FFFFFF", "#CCCCCC");
        this.description = description != null ? description : new ArrayList<>();
        this.statBonus = statBonus != null ? statBonus : new HashMap<>();
        this.passiveDescription = passiveDescription;
        this.focusRegenBonus = focusRegenBonus;
    }

    /**
     * Factory method per creare una PlayerClass da una ConfigurationSection (YAML).
     */
    public static PlayerClass fromConfig(ConfigurationSection section) {
        if (section == null) return null;

        String id = section.getString("id");
        String displayName = section.getString("display-name", "Unknown Class");
        
        Material iconMaterial = Material.STONE;
        try {
            String iconStr = section.getString("icon", "STONE");
            iconMaterial = Material.valueOf(iconStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Usa materiale default se l'icona non è valida
        }

        List<String> colorGradient = section.getStringList("color-gradient");
        if (colorGradient.isEmpty()) {
            colorGradient = Arrays.asList("#FFFFFF", "#CCCCCC");
        }

        List<String> description = section.getStringList("description");
        
        Map<StatDefinition, Double> statBonus = new HashMap<>();
        ConfigurationSection bonusSection = section.getConfigurationSection("stat-bonus");
        if (bonusSection != null) {
            for (String key : bonusSection.getKeys(false)) {
                StatDefinition stat = StatDefinition.fromId(key);
                if (stat != null) {
                    statBonus.put(stat, bonusSection.getDouble(key, 1.0));
                }
            }
        }

        String passiveDescription = section.getString("passive", "");
        double focusRegenBonus = section.getDouble("focus-regen-bonus", 0.0);

        if (id == null) return null;

        return new PlayerClass(id, displayName, iconMaterial, colorGradient, description, statBonus, passiveDescription, focusRegenBonus);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public List<String> getColorGradient() {
        return colorGradient;
    }

    public List<String> getDescription() {
        return ColorUtils.translate(description);
    }

    /**
     * Ottiene il moltiplicatore per una specifica statistica.
     * Ritorna 1.0 se nessun bonus è definito per quella stat.
     */
    public double getStatMultiplier(StatDefinition stat) {
        return statBonus.getOrDefault(stat, 1.0);
    }

    public Map<StatDefinition, Double> getAllStatBonuses() {
        return Collections.unmodifiableMap(statBonus);
    }

    public String getPassiveDescription() {
        return passiveDescription;
    }

    public double getFocusRegenBonus() {
        return focusRegenBonus;
    }

    /**
     * Crea un ItemStack rappresentante la classe per le GUI.
     */
    public ItemStack createDisplayItem() {
        // Nota: ItemBuilder sarebbe qui se esistesse già, usiamo codice inline semplice
        ItemStack item = new ItemStack(iconMaterial);
        // La formattazione completa della lore verrà fatta nella GUI
        return item;
    }
}
