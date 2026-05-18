package org.axostudio.axonpcs.api.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stable public view of an AxoNPCs NPC. Implementations are owned by AxoNPCs.
 */
public interface AxoNPC {
    String getId();

    UUID getUniqueId();

    String getType();

    Location getLocation();

    String getDisplayName();

    NPCSkin getSkin();

    String getGlowing();

    boolean isCollidable();

    double getScale();

    double getViewDistance();

    double getInteractionCooldownSeconds();

    Map<NPCEquipmentSlot, ItemStack> getEquipment();

    List<NPCAction> getActions(NPCActionTrigger trigger);
}
