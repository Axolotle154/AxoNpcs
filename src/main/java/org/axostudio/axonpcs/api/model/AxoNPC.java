package org.axostudio.axonpcs.api.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stable public view of an AxoNPCs NPC.
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

    default boolean isSoundEnabled() {
        return true;
    }

    default double getMaxHealth() {
        return 20.0D;
    }

    double getScale();

    double getViewDistance();

    double getInteractionCooldownSeconds();

    boolean isTurnToPlayer();

    double getTurnToPlayerDistance();

    Map<NPCEquipmentSlot, ItemStack> getEquipment();

    default Map<String, String> getAppearance() {
        return Map.of();
    }

    List<NPCAction> getActions(NPCActionTrigger trigger);

    default String getGroup() {
        return null;
    }
}
