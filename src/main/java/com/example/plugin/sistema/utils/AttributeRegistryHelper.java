package com.example.plugin.sistema.utils;

import org.bukkit.attribute.Attribute;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.Nullable;

/**
 * Helper per accedere correttamente al Registry degli Attribute in Spigot 1.21.5.
 * In 1.21+, Attribute non è più un enum ma usa il registry system.
 */
public class AttributeRegistryHelper {

    /**
     * Ottiene un Attribute dal registry usando la chiave namespaced.
     * Evita l'uso deprecato di Attribute.valueOf o costanti GENERIC_.
     * 
     * @param key La chiave dell'attributo (es. "max_health", "movement_speed").
     * @return L'Attribute corrispondente, o null se non trovato.
     */
    @Nullable
    public static Attribute getAttribute(String key) {
        NamespacedKey namespacedKey = NamespacedKey.minecraft(key);
        return Registry.ATTRIBUTE.get(namespacedKey);
    }

    // Costanti pre-risolte per performance e sicurezza
    public static final Attribute MAX_HEALTH = getAttribute("max_health");
    public static final Attribute MOVEMENT_SPEED = getAttribute("movement_speed");
    public static final Attribute ATTACK_DAMAGE = getAttribute("attack_damage");
    public static final Attribute ATTACK_SPEED = getAttribute("attack_speed");
    public static final Attribute ATTACK_KNOCKBACK = getAttribute("attack_knockback");
    public static final Attribute KNOCKBACK_RESISTANCE = getAttribute("knockback_resistance");
    public static final Attribute SAFE_FALL_DISTANCE = getAttribute("safe_fall_distance");
    public static final Attribute LUCK = getAttribute("luck");
    public static final Attribute ARMOR = getAttribute("armor");
    public static final Attribute ARMOR_TOUGHNESS = getAttribute("armor_toughness");

    /**
     * Verifica se un Attribute è valido (non null).
     */
    public static boolean isValid(Attribute attribute) {
        return attribute != null;
    }
}
