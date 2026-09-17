package org.axostudio.axonpcs.api.event.npc;

import org.axostudio.axonpcs.api.model.AxoNPC;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class AxoNPCEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AxoNPC npc;

    protected AxoNPCEvent(AxoNPC npc) {
        this.npc = npc;
    }

    protected AxoNPCEvent(AxoNPC npc, boolean isAsync) {
        super(isAsync);
        this.npc = npc;
    }

    public AxoNPC getNPC() {
        return npc;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
