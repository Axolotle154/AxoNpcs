package org.axostudio.axonpcs.api.event;

import org.axostudio.axonpcs.api.model.AxoNPC;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event called when an NPC is deleted.
 * Maintained in org.axostudio.axonpcs.api.event for backward compatibility with AxoHologram.
 */
public class AxoNPCDeleteEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AxoNPC npc;
    private boolean cancelled;

    public AxoNPCDeleteEvent(AxoNPC npc) {
        this.npc = npc;
    }

    public AxoNPC getNPC() {
        return npc;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
