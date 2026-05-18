package org.axostudio.axonpcs.api;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.model.AxoNPC;
import org.axostudio.axonpcs.api.model.NPCAction;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.axostudio.axonpcs.api.model.NPCSkin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

public final class AxoNPCsAPIImpl implements AxoNPCsAPI {
    private final AxoNPCsPlugin plugin;

    public AxoNPCsAPIImpl(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<AxoNPC> getNPC(String id) {
        return plugin.getNpcManager().get(id).map(npc -> npc);
    }

    @Override
    public AxoNPC createNPC(String id, Location location) {
        return plugin.getNpcManager().create(id, location);
    }

    @Override
    public boolean deleteNPC(String id) {
        return plugin.getNpcManager().delete(id);
    }

    @Override
    public boolean exists(String id) {
        return plugin.getNpcManager().exists(id);
    }

    @Override
    public Collection<AxoNPC> getNPCs() {
        return plugin.getNpcManager().all().stream().map(npc -> (AxoNPC) npc).toList();
    }

    @Override
    public boolean showNPC(Player player, String id) {
        return plugin.getNpcManager().get(id).map(npc -> {
            plugin.getViewerManager().show(player, npc);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean hideNPC(Player player, String id) {
        return plugin.getNpcManager().get(id).map(npc -> {
            plugin.getViewerManager().hide(player, npc);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean setDisplayName(String id, String displayName) {
        return plugin.getNpcManager().setDisplayName(id, displayName);
    }

    @Override
    public boolean setSkin(String id, NPCSkin skin) {
        return plugin.getNpcManager().setSkin(id, skin);
    }

    @Override
    public boolean setLocation(String id, Location location) {
        return plugin.getNpcManager().setLocation(id, location);
    }

    @Override
    public boolean setRotation(String id, float yaw, float pitch) {
        return plugin.getNpcManager().setRotation(id, yaw, pitch);
    }

    @Override
    public boolean addAction(String id, NPCActionTrigger trigger, NPCAction action) {
        return plugin.getNpcManager().addAction(id, trigger, action);
    }

    @Override
    public boolean removeAction(String id, NPCActionTrigger trigger, int index) {
        return plugin.getNpcManager().removeAction(id, trigger, index);
    }

    @Override
    public void registerAction(String type, NPCActionExecutor executor) {
        plugin.getActionManager().register(type, executor);
    }

    @Override
    public int reloadNPCs() {
        plugin.getViewerManager().hideAll();
        int count = plugin.getNpcManager().reload();
        plugin.getViewerManager().refreshAll();
        return count;
    }
}
