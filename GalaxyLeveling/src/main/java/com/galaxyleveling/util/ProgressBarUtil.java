package com.galaxyleveling.util;

public class ProgressBarUtil {
    public static String createProgressBar(double current, double max, int length, char filledChar, char emptyChar) {
        if (max <= 0) return "";
        double percentage = Math.min(1.0, Math.max(0.0, current / max));
        int filledCount = (int) (percentage * length);
        int emptyCount = length - filledCount;
        
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filledCount; i++) {
            bar.append(filledChar);
        }
        for (int i = 0; i < emptyCount; i++) {
            bar.append(emptyChar);
        }
        return bar.toString();
    }
}
