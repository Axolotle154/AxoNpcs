package org.axostudio.axonpcs.api;

import org.axostudio.axonpcs.api.model.AxoNPC;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface NPCActionExecutor {
    void execute(Player player, AxoNPC npc, String value);
}
