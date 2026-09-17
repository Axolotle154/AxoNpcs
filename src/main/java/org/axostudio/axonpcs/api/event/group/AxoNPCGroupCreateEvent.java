package org.axostudio.axonpcs.api.event.group;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AxoNPCGroupCreateEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String groupName;
    private boolean cancelled;

    public AxoNPCGroupCreateEvent(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
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
