package com.example.plugin.sistema.utils;

import net.md_5.bungee.api.ChatColor;

/**
 * Utility per creare barre di progresso testuali per GUI e messaggi.
 */
public class ProgressBarUtil {

    private static final String COMPLETED_BAR = "▉";
    private static final String EMPTY_BAR = "░";

    /**
     * Crea una barra di progresso colorata.
     * @param current Valore attuale.
     * @param max Valore massimo.
     * @param length Lunghezza della barra in caratteri.
     * @param completedColor Colore per la parte completata.
     * @param emptyColor Colore per la parte vuota.
     * @return La stringa rappresentante la barra.
     */
    public static String getProgressBar(double current, double max, int length, ChatColor completedColor, ChatColor emptyColor) {
        if (max <= 0) max = 1;
        if (current < 0) current = 0;
        if (current > max) current = max;

        double percent = current / max;
        int completedChars = (int) (percent * length);
        int emptyChars = length - completedChars;

        StringBuilder sb = new StringBuilder();
        sb.append(completedColor);
        
        for (int i = 0; i < completedChars; i++) {
            sb.append(COMPLETED_BAR);
        }
        
        sb.append(emptyColor);
        
        for (int i = 0; i < emptyChars; i++) {
            sb.append(EMPTY_BAR);
        }

        return sb.toString();
    }

    /**
     * Versione semplificata con colori default (Aqua/Gray).
     */
    public static String getProgressBar(double current, double max, int length) {
        return getProgressBar(current, max, length, ChatColor.AQUA, ChatColor.GRAY);
    }
}
