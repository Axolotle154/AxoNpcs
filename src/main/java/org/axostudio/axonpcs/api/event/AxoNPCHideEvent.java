package org.axostudio.axonpcs.api.event;

import org.axostudio.axonpcs.api.model.AxoNPC;
import org.bukkit.entity.Player;

public class AxoNPCHideEvent extends AxoNPCEvent {
    private final Player player;

    public AxoNPCHideEvent(Player player, AxoNPC npc) {
        super(npc);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
