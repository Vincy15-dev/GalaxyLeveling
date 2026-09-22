package com.example.plugin.sistema.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento chiamato quando un giocatore sale di livello nel sistema IL SISTEMA.
 */
public class PlayerLevelUpEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final Player player;
    private final int oldLevel;
    private final int newLevel;
    
    public PlayerLevelUpEvent(Player player, int oldLevel, int newLevel) {
        super(false); // async = false, evento main thread
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public int getOldLevel() {
        return oldLevel;
    }
    
    public int getNewLevel() {
        return newLevel;
    }
}
