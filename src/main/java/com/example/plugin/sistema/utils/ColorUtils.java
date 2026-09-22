package com.example.plugin.sistema.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility per la gestione dei colori HEX e gradienti per Spigot 1.21.x
 */
public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Traduce una stringa con codici colore legacy (&) e HEX (&#RRGGBB) in ChatColor.
     */
    public static String translate(String text) {
        if (text == null) return "";
        
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);
        
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Traduce una lista di stringhe applicando colori e gradienti.
     */
    public static List<String> translate(List<String> lines) {
        if (lines == null) return new ArrayList<>();
        List<String> translated = new ArrayList<>();
        for (String line : lines) {
            translated.add(translate(line));
        }
        return translated;
    }

    /**
     * Crea un gradiente di colore tra due colori HEX su una stringa.
     * @param text Il testo da colorare.
     * @param startHex Colore iniziale (es. "#00AAAA").
     * @param endHex Colore finale (es. "#0066CC").
     * @return La stringa con il gradiente applicato.
     */
    public static String applyGradient(String text, String startHex, String endHex) {
        if (text == null || text.isEmpty()) return "";
        
        ChatColor start = ChatColor.of(startHex);
        ChatColor end = ChatColor.of(endHex);
        
        StringBuilder result = new StringBuilder();
        int len = text.length();
        
        for (int i = 0; i < len; i++) {
            double ratio = (double) i / (len - 1);
            // Interpolazione semplice non supportata direttamente da ChatColor, 
            // usiamo un approccio semplificato: o start o end o alternanza se necessario.
            // Per un gradiente reale servirebbe calcolare RGB intermedi.
            // Implementazione RGB reale:
            Color c1 = fromChatColor(start);
            Color c2 = fromChatColor(end);
            
            int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
            int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
            int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
            
            result.append(ChatColor.of(Color.fromRGB(r, g, b))).append(text.charAt(i));
        }
        
        return result.toString();
    }
    
    private static Color fromChatColor(ChatColor chatColor) {
        // Estrae il colore Bungee sottostante se è un colore RGB
        try {
            java.lang.reflect.Field field = ChatColor.class.getDeclaredField("color");
            field.setAccessible(true);
            Object obj = field.get(chatColor);
            if (obj instanceof Color) {
                return (Color) obj;
            }
        } catch (Exception e) {
            // Fallback o log
        }
        return Color.WHITE;
    }
}
