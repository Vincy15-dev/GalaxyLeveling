package com.galaxyleveling.util;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.Registry;
import com.galaxyleveling.GalaxyLeveling;
import com.galaxyleveling.model.StatDefinition;

public class AttributeRegistryHelper {
    
    public static Attribute getAttributeForStat(StatDefinition stat) {
        switch (stat) {
            case FORZA:
                return Registry.ATTRIBUTE.get(NamespacedKey.minecraft("attack_damage"));
            case AGILITA:
                return Registry.ATTRIBUTE.get(NamespacedKey.minecraft("movement_speed"));
            case VITALITA:
                return Registry.ATTRIBUTE.get(NamespacedKey.minecraft("max_health"));
            case INTELLIGENZA:
                return Registry.ATTRIBUTE.get(NamespacedKey.minecraft("luck"));
            case PERCEZIONE:
                return Registry.ATTRIBUTE.get(NamespacedKey.minecraft("knockback_resistance"));
            default:
                return null;
        }
    }
    
    public static NamespacedKey getModifierKey(GalaxyLeveling plugin, StatDefinition stat, String suffix) {
        return new NamespacedKey(plugin, "stat_" + stat.name().toLowerCase() + "_" + suffix);
    }
}
