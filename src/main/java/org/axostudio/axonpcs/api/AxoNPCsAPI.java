package org.axostudio.axonpcs.api;

import org.axostudio.axonpcs.api.model.AxoNPC;
import org.axostudio.axonpcs.api.model.NPCAction;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.axostudio.axonpcs.api.model.NPCSkin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

/**
 * Public integration surface for AxoNPCs.
 */
public interface AxoNPCsAPI {
    Optional<AxoNPC> getNPC(String id);

    AxoNPC createNPC(String id, Location location);

    boolean deleteNPC(String id);

    boolean exists(String id);

    Collection<AxoNPC> getNPCs();

    boolean showNPC(Player player, String id);

    boolean hideNPC(Player player, String id);

    boolean setDisplayName(String id, String displayName);

    boolean setSkin(String id, NPCSkin skin);

    boolean setLocation(String id, Location location);

    boolean setRotation(String id, float yaw, float pitch);

    boolean addAction(String id, NPCActionTrigger trigger, NPCAction action);

    boolean removeAction(String id, NPCActionTrigger trigger, int index);

    void registerAction(String type, NPCActionExecutor executor);

    int reloadNPCs();
}
