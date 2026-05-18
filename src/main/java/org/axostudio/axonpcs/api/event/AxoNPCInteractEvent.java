package org.axostudio.axonpcs.api.event;

import org.axostudio.axonpcs.api.model.AxoNPC;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class AxoNPCInteractEvent extends AxoNPCEvent implements Cancellable {
    private final Player player;
    private final NPCActionTrigger trigger;
    private boolean cancelled;

    public AxoNPCInteractEvent(Player player, AxoNPC npc, NPCActionTrigger trigger) {
        super(npc);
        this.player = player;
        this.trigger = trigger;
    }

    public Player getPlayer() {
        return player;
    }

    public NPCActionTrigger getTrigger() {
        return trigger;
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
