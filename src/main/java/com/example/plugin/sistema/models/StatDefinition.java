package com.example.plugin.sistema.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Definizioni delle 5 statistiche del sistema.
 */
public enum StatDefinition {
    FORZA("forza", "strength"),
    AGILITA("agilita", "agility"),
    VITALITA("vitalita", "vitality"),
    INTELLIGENZA("intelligenza", "intelligence"),
    PERCEZIONE("percezione", "perception");

    private final String id;
    private final String configKey;

    StatDefinition(String id, String configKey) {
        this.id = id;
        this.configKey = configKey;
    }

    public String getId() {
        return id;
    }

    public String getConfigKey() {
        return configKey;
    }

    /**
     * Ottiene una StatDefinition dal suo ID (case-insensitive).
     */
    public static StatDefinition fromId(String id) {
        if (id == null) return null;
        for (StatDefinition stat : values()) {
            if (stat.getId().equalsIgnoreCase(id)) {
                return stat;
            }
        }
        return null;
    }

    /**
     * Mappa utile per iterare sulle statistiche senza chiamare values() ogni volta.
     */
    public static final Map<String, StatDefinition> BY_ID = new HashMap<>();
    static {
        for (StatDefinition stat : values()) {
            BY_ID.put(stat.getId(), stat);
        }
    }
}
