package org.axostudio.axonpcs.api.service;

import org.axostudio.axonpcs.api.model.AxoNPC;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Public service contract for interacting with AxoNPCs.
 */
public interface AxoNPCService {
    Optional<AxoNPC> getNPC(String id);

    Collection<AxoNPC> getAllNPCs();

    List<AxoNPC> getNPCsByGroup(String group);

    boolean exists(String id);

    AxoNPC createNPC(String id, Location location);

    default AxoNPC copyNPC(String sourceId, String newId, Location location) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default AxoNPC copyNPC(String sourceId, String newId) {
        return copyNPC(sourceId, newId, null);
    }

    boolean deleteNPC(String id);

    int reload();

    int getViewerCount(AxoNPC npc);

    boolean isVisibleTo(Player player, AxoNPC npc);
}
