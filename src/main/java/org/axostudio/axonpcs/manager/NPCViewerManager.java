package org.axostudio.axonpcs.manager;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.event.AxoNPCHideEvent;
import org.axostudio.axonpcs.api.event.AxoNPCShowEvent;
import org.axostudio.axonpcs.model.VirtualNPC;
import org.axostudio.axonpcs.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NPCViewerManager {
    private final AxoNPCsPlugin plugin;
    private final Map<UUID, Set<String>> visible = new ConcurrentHashMap<>();
    private final Map<UUID, SchedulerUtil.TaskHandle> tasks = new ConcurrentHashMap<>();

    public NPCViewerManager(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(Player player) {
        stop(player);
        long delay = plugin.getConfig().getLong("rendering.join-delay-ticks", 20L);
        long interval = Math.max(1L, plugin.getConfig().getLong("rendering.update-interval-ticks", 10L));
        plugin.getSchedulerUtil().runEntityDelayed(player, () -> tick(player), delay);
        SchedulerUtil.TaskHandle task = plugin.getSchedulerUtil().runEntityTimer(player, () -> tick(player), delay + interval, interval);
        tasks.put(player.getUniqueId(), task);
    }

    public void stop(Player player) {
        SchedulerUtil.TaskHandle task = tasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        hideAll(player);
    }

    public void tick(Player player) {
        if (!player.isOnline()) {
            stop(player);
            return;
        }
        Location playerLocation = player.getLocation();
        for (VirtualNPC npc : plugin.getNpcManager().all()) {
            boolean shouldSee = shouldSee(playerLocation, npc);
            boolean currentlyVisible = isVisible(player, npc);
            if (shouldSee && !currentlyVisible) {
                show(player, npc);
            } else if (!shouldSee && currentlyVisible) {
                hide(player, npc);
            }
        }
    }

    public void show(Player player, VirtualNPC npc) {
        if (!player.isOnline() || isVisible(player, npc)) {
            return;
        }
        if (!plugin.getPacketManager().show(player, npc)) {
            return;
        }
        visible.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(npc.getId());
        plugin.getServer().getPluginManager().callEvent(new AxoNPCShowEvent(player, npc));
    }

    public void hide(Player player, VirtualNPC npc) {
        Set<String> playerVisible = visible.get(player.getUniqueId());
        if (playerVisible == null || !playerVisible.remove(npc.getId())) {
            return;
        }
        plugin.getPacketManager().hide(player, npc);
        plugin.getServer().getPluginManager().callEvent(new AxoNPCHideEvent(player, npc));
        if (playerVisible.isEmpty()) {
            visible.remove(player.getUniqueId());
        }
    }

    public void hideAll(Player player) {
        Set<String> ids = visible.remove(player.getUniqueId());
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Set<String> copy = new HashSet<>(ids);
        for (String id : copy) {
            plugin.getNpcManager().get(id).ifPresent(npc -> {
                plugin.getPacketManager().hide(player, npc);
                plugin.getServer().getPluginManager().callEvent(new AxoNPCHideEvent(player, npc));
            });
        }
        plugin.getPacketManager().hideAll(player);
    }

    public void hideAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerUtil().runEntity(player, () -> hideAll(player));
        }
    }

    public void hideNPCFromAll(VirtualNPC npc) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerUtil().runEntity(player, () -> hide(player, npc));
        }
    }

    public void refreshNPC(VirtualNPC npc) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerUtil().runEntity(player, () -> {
                if (isVisible(player, npc)) {
                    hide(player, npc);
                }
                tick(player);
            });
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerUtil().runEntity(player, () -> {
                hideAll(player);
                tick(player);
            });
        }
    }

    public boolean isVisible(Player player, VirtualNPC npc) {
        Set<String> ids = visible.get(player.getUniqueId());
        return ids != null && ids.contains(npc.getId());
    }

    public Optional<VirtualNPC> findVisibleByEntityId(UUID playerId, int entityId) {
        Set<String> ids = visible.get(playerId);
        if (ids == null) {
            return Optional.empty();
        }
        for (String id : ids) {
            Optional<VirtualNPC> npc = plugin.getNpcManager().get(id);
            if (npc.isPresent() && npc.get().getEntityId() == entityId) {
                return npc;
            }
        }
        return Optional.empty();
    }

    public int viewerCount(VirtualNPC npc) {
        return plugin.getPacketManager().viewerCount(npc);
    }

    private boolean shouldSee(Location playerLocation, VirtualNPC npc) {
        if (playerLocation.getWorld() == null || !playerLocation.getWorld().getName().equals(npc.getPosition().world())) {
            return false;
        }
        double distanceSquared = playerLocation.distanceSquared(npc.getLocation());
        double viewDistance = npc.getViewDistance();
        return distanceSquared <= viewDistance * viewDistance;
    }
}
