package org.axostudio.axonpcs.api.event;

import org.axostudio.axonpcs.api.model.AxoNPC;
import org.bukkit.event.Cancellable;

public class AxoNPCCreateEvent extends AxoNPCEvent implements Cancellable {
    private boolean cancelled;

    public AxoNPCCreateEvent(AxoNPC npc) {
        super(npc);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
