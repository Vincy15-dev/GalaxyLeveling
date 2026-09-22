package com.example.plugin.sistema.models;

import com.example.plugin.sistema.utils.ColorUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rappresenta un Grado (Rank) nel sistema di progressione.
 * I dati sono caricati da ranks.yml.
 */
public class Rank {

    private final String id;
    private final String displayName;
    private final String colorHex;
    private final int minLevel;
    private final int maxLevel;
    private final String requiresTrial; // ID della quest trial, null se nessuna

    public Rank(String id, String displayName, String colorHex, int minLevel, int maxLevel, String requiresTrial) {
        this.id = id;
        this.displayName = displayName;
        this.colorHex = colorHex;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.requiresTrial = requiresTrial;
    }

    /**
     * Factory method per creare un Rank da una ConfigurationSection (YAML).
     */
    public static Rank fromConfig(ConfigurationSection section) {
        if (section == null) return null;
        
        String id = section.getString("id");
        String displayName = section.getString("display-name", "Unknown");
        String colorHex = section.getString("color", "#FFFFFF");
        int minLevel = section.getInt("min-level", 1);
        int maxLevel = section.getInt("max-level", 100);
        String requiresTrial = section.getString("requires-trial");
        
        if (id == null) return null;
        
        return new Rank(id, displayName, colorHex, minLevel, maxLevel, requiresTrial);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Ottiene il colore formattato come ChatColor.
     */
    public String getColorFormatted() {
        return ColorUtils.translate("&" + colorHex); // O usa ChatColor.of(colorHex) direttamente
    }

    public String getColorHex() {
        return colorHex;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Controlla se questo rank richiede una trial quest per essere superato.
     */
    public boolean hasTrial() {
        return requiresTrial != null && !requiresTrial.isEmpty();
    }

    public String getRequiresTrial() {
        return requiresTrial;
    }

    /**
     * Controlla se un livello dato rientra in questo rank.
     */
    public boolean containsLevel(int level) {
        return level >= minLevel && level <= maxLevel;
    }
}
