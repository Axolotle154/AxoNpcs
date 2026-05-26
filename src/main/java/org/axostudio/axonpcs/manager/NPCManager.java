package org.axostudio.axonpcs.manager;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.event.AxoNPCCreateEvent;
import org.axostudio.axonpcs.api.event.AxoNPCDeleteEvent;
import org.axostudio.axonpcs.api.model.NPCAction;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.axostudio.axonpcs.api.model.NPCSkin;
import org.axostudio.axonpcs.model.NPCPosition;
import org.axostudio.axonpcs.model.VirtualNPC;
import org.axostudio.axonpcs.packet.EntityIdAllocator;
import org.axostudio.axonpcs.storage.NPCStorageManager;
import org.axostudio.axonpcs.util.IdValidator;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NPCManager {
    private final AxoNPCsPlugin plugin;
    private final NPCStorageManager storageManager;
    private final Map<String, VirtualNPC> npcs = new ConcurrentHashMap<>();

    public NPCManager(AxoNPCsPlugin plugin, NPCStorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    public int reload() {
        npcs.clear();
        for (VirtualNPC npc : storageManager.loadAll()) {
            npcs.put(npc.getId(), npc);
        }
        return npcs.size();
    }

    public Optional<VirtualNPC> get(String id) {
        return Optional.ofNullable(npcs.get(IdValidator.normalize(id)));
    }

    public Collection<VirtualNPC> all() {
        ArrayList<VirtualNPC> list = new ArrayList<>(npcs.values());
        list.sort(Comparator.comparing(VirtualNPC::getId));
        return list;
    }

    public boolean exists(String id) {
        return npcs.containsKey(IdValidator.normalize(id));
    }

    public boolean importNPC(VirtualNPC npc, boolean overwrite) {
        String id = IdValidator.requireValid(npc.getId());
        boolean exists = npcs.containsKey(id) || storageManager.exists(id);
        if (exists && !overwrite) {
            return false;
        }
        VirtualNPC previous = npcs.get(id);
        if (previous != null && plugin.getViewerManager() != null) {
            plugin.getViewerManager().hideNPCFromAll(previous);
        }
        storageManager.save(npc);
        if (npc.isEnabled()) {
            npcs.put(id, npc);
        } else {
            npcs.remove(id);
        }
        return true;
    }

    public VirtualNPC create(String rawId, Location location) {
        String id = IdValidator.requireValid(rawId);
        if (exists(id)) {
            throw new IllegalArgumentException("NPC already exists: " + id);
        }
        VirtualNPC npc = new VirtualNPC(
                id,
                UUID.nameUUIDFromBytes(("AxoNPCs:" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                EntityIdAllocator.nextEntityId(),
                NPCPosition.from(location)
        );
        npc.setViewDistance(plugin.getConfig().getDouble("rendering.view-distance", 48.0D));
        npc.setInteractionCooldownSeconds(plugin.getConfig().getDouble("interaction.default-cooldown-seconds", 1.5D));

        AxoNPCCreateEvent event = new AxoNPCCreateEvent(npc);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("NPC creation was cancelled");
        }
        npcs.put(id, npc);
        storageManager.save(npc);
        plugin.getViewerManager().refreshAll();
        return npc;
    }

    public boolean delete(String rawId) {
        String id = IdValidator.normalize(rawId);
        VirtualNPC npc = npcs.get(id);
        if (npc == null) {
            return false;
        }
        AxoNPCDeleteEvent event = new AxoNPCDeleteEvent(npc);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        plugin.getViewerManager().hideNPCFromAll(npc);
        npcs.remove(id);
        storageManager.delete(id);
        return true;
    }

    public void save(VirtualNPC npc) {
        if (plugin.getViewerManager() != null) {
            plugin.getViewerManager().wakeNPC(npc);
        }
        if (plugin.getConfig().getBoolean("storage.save-on-change", true)) {
            storageManager.save(npc);
        }
    }

    public boolean setLocation(String id, Location location) {
        Optional<VirtualNPC> optional = get(id);
        if (optional.isEmpty()) {
            return false;
        }
        VirtualNPC npc = optional.get();
        npc.setPosition(NPCPosition.from(location));
        save(npc);
        plugin.getViewerManager().refreshNPC(npc);
        return true;
    }

    public boolean setRotation(String id, float yaw, float pitch) {
        Optional<VirtualNPC> optional = get(id);
        if (optional.isEmpty()) {
            return false;
        }
        VirtualNPC npc = optional.get();
        npc.setPosition(npc.getPosition().withRotation(yaw, pitch));
        save(npc);
        plugin.getPacketManager().updateRotation(npc);
        return true;
    }

    public boolean setDisplayName(String id, String displayName) {
        Optional<VirtualNPC> optional = get(id);
        if (optional.isEmpty()) {
            return false;
        }
        VirtualNPC npc = optional.get();
        npc.setDisplayName(displayName);
        save(npc);
        plugin.getViewerManager().refreshNPC(npc);
        return true;
    }

    public boolean setSkin(String id, NPCSkin skin) {
        Optional<VirtualNPC> optional = get(id);
        if (optional.isEmpty()) {
            return false;
        }
        VirtualNPC npc = optional.get();
        npc.setSkin(skin);
        save(npc);
        plugin.getViewerManager().refreshNPC(npc);
        return true;
    }

    public boolean addAction(String id, NPCActionTrigger trigger, NPCAction action) {
        Optional<VirtualNPC> optional = get(id);
        if (optional.isEmpty()) {
            return false;
        }
        VirtualNPC npc = optional.get();
        npc.addAction(trigger, action);
        save(npc);
        return true;
    }

    public boolean removeAction(String id, NPCActionTrigger trigger, int index) {
        Optional<VirtualNPC> optional = get(id);
        if (optional.isEmpty()) {
            return false;
        }
        VirtualNPC npc = optional.get();
        boolean removed = npc.removeAction(trigger, index);
        if (removed) {
            save(npc);
        }
        return removed;
    }
}
