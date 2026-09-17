package org.axostudio.axonpcs.api;

import org.axostudio.axonpcs.api.model.AxoNPC;
import org.axostudio.axonpcs.api.service.AxoNPCProvider;
import org.axostudio.axonpcs.api.service.AxoNPCService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Legacy provider for AxoNPCs API.
 * Maintained for backward compatibility with plugins such as AxoHologram.
 */
public final class AxoNPCsProvider {
    private static volatile AxoNPCsAPI instance;

    private AxoNPCsProvider() {
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public static AxoNPCsAPI getAPI() {
        AxoNPCsAPI current = instance;
        if (current == null) {
            throw new IllegalStateException("AxoNPCs API is not available yet. Is AxoNPCs enabled?");
        }
        return current;
    }

    public static AxoNPCsAPI get() {
        return getAPI();
    }

    public static void register(AxoNPCsAPI api) {
        instance = api;
        AxoNPCProvider.register(api);
    }

    public static void register(AxoNPCService service) {
        if (service instanceof AxoNPCsAPI api) {
            register(api);
        } else {
            register(new AxoNPCsAPI() {
                @Override
                public Optional<AxoNPC> getNPC(String id) {
                    return service.getNPC(id);
                }

                @Override
                public Collection<AxoNPC> getAllNPCs() {
                    return service.getAllNPCs();
                }

                @Override
                public List<AxoNPC> getNPCsByGroup(String group) {
                    return service.getNPCsByGroup(group);
                }

                @Override
                public boolean exists(String id) {
                    return service.exists(id);
                }

                @Override
                public AxoNPC createNPC(String id, Location location) {
                    return service.createNPC(id, location);
                }

                @Override
                public boolean deleteNPC(String id) {
                    return service.deleteNPC(id);
                }

                @Override
                public int reload() {
                    return service.reload();
                }

                @Override
                public int getViewerCount(AxoNPC npc) {
                    return service.getViewerCount(npc);
                }

                @Override
                public boolean isVisibleTo(Player player, AxoNPC npc) {
                    return service.isVisibleTo(player, npc);
                }

                @Override
                public AxoNPC copyNPC(String sourceId, String newId, Location location) {
                    return service.copyNPC(sourceId, newId, location);
                }

                @Override
                public AxoNPC copyNPC(String sourceId, String newId) {
                    return service.copyNPC(sourceId, newId);
                }
            });
        }
    }

    public static void unregister() {
        instance = null;
        AxoNPCProvider.unregister();
    }
}
