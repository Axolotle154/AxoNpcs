package org.axostudio.axonpcs.manager;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.event.AxoNPCHideEvent;
import org.axostudio.axonpcs.api.event.AxoNPCShowEvent;
import org.axostudio.axonpcs.model.NPCPosition;
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
    private static final long MILLIS_PER_SECOND = 1000L;

    private final AxoNPCsPlugin plugin;
    private final Map<UUID, Set<String>> visible = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> viewersByNpc = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> turningToPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Set<SchedulerUtil.TaskHandle>> tasks = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActiveMillis = new ConcurrentHashMap<>();
    private final Set<String> sleeping = ConcurrentHashMap.newKeySet();

    public NPCViewerManager(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(Player player) {
        if (plugin.isShuttingDown() || !player.isOnline()) {
            return;
        }
        stop(player);
        schedule(player);
    }

    public void restartAll() {
        if (plugin.isShuttingDown()) {
            return;
        }
        cancelAllTasks();
        sleeping.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerUtil().runEntity(player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                hideAll(player);
                schedule(player);
            });
        }
    }

    private void schedule(Player player) {
        long delay = joinDelayTicks();
        long visibilityInterval = visibilityIntervalTicks();
        track(player, plugin.getSchedulerUtil().runEntityDelayed(player, () -> refreshVisibility(player), delay));
        track(player, plugin.getSchedulerUtil().runEntityTimer(player, () -> refreshVisibility(player), delay + visibilityInterval, visibilityInterval));
        if (rotationEnabled()) {
            long rotationInterval = rotationIntervalTicks();
            track(player, plugin.getSchedulerUtil().runEntityTimer(player, () -> updateRotations(player), delay + rotationInterval, rotationInterval));
        }
    }

    public void stop(Player player) {
        cancelTasks(player.getUniqueId());
        if (plugin.isShuttingDown()) {
            hideAllImmediately(player);
            return;
        }
        hideAll(player);
    }

    public void tick(Player player) {
        refreshVisibility(player);
        updateRotations(player);
    }

    public void refreshVisibility(Player player) {
        if (plugin.isShuttingDown()) {
            return;
        }
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
            } else if (shouldSee) {
                addViewer(player.getUniqueId(), npc);
            } else if (currentlyVisible) {
                hide(player, npc);
            } else {
                updateIdleState(npc);
            }
        }
    }

    public void updateRotations(Player player) {
        if (plugin.isShuttingDown() || !rotationEnabled()) {
            return;
        }
        if (!player.isOnline()) {
            stop(player);
            return;
        }
        Set<String> ids = visible.get(player.getUniqueId());
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Location playerLocation = player.getLocation();
        for (String id : Set.copyOf(ids)) {
            plugin.getNpcManager().get(id).ifPresent(npc -> {
                if (!isVisible(player, npc) || isSleeping(npc)) {
                    return;
                }
                updateTurnToPlayer(player, playerLocation, npc);
            });
        }
    }

    public void show(Player player, VirtualNPC npc) {
        if (plugin.isShuttingDown() || !player.isOnline() || isVisible(player, npc)) {
            return;
        }
        wakeNPC(npc);
        if (!plugin.getPacketManager().show(player, npc)) {
            return;
        }
        visible.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(npc.getId());
        addViewer(player.getUniqueId(), npc);
        plugin.getServer().getPluginManager().callEvent(new AxoNPCShowEvent(player, npc));
    }

    public void hide(Player player, VirtualNPC npc) {
        if (plugin.isShuttingDown()) {
            hideImmediately(player, npc);
            return;
        }
        Set<String> playerVisible = visible.get(player.getUniqueId());
        if (playerVisible == null || !playerVisible.remove(npc.getId())) {
            return;
        }
        forgetTurnToPlayer(player.getUniqueId(), npc.getId());
        removeViewer(player.getUniqueId(), npc);
        plugin.getPacketManager().hide(player, npc);
        plugin.getServer().getPluginManager().callEvent(new AxoNPCHideEvent(player, npc));
        if (playerVisible.isEmpty()) {
            visible.remove(player.getUniqueId());
        }
    }

    public void hideAll(Player player) {
        if (plugin.isShuttingDown()) {
            hideAllImmediately(player);
            return;
        }
        Set<String> ids = visible.remove(player.getUniqueId());
        turningToPlayer.remove(player.getUniqueId());
        if (ids == null || ids.isEmpty()) {
            removePlayerFromViewerCache(player.getUniqueId());
            plugin.getPacketManager().hideAll(player);
            return;
        }
        Set<String> copy = new HashSet<>(ids);
        for (String id : copy) {
            plugin.getNpcManager().get(id).ifPresent(npc -> {
                removeViewer(player.getUniqueId(), npc);
                plugin.getPacketManager().hide(player, npc);
                plugin.getServer().getPluginManager().callEvent(new AxoNPCHideEvent(player, npc));
            });
        }
        plugin.getPacketManager().hideAll(player);
    }

    public void hideAll() {
        if (plugin.isShuttingDown()) {
            hideAllImmediately();
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerUtil().runEntity(player, () -> hideAll(player));
        }
    }

    public void hideNPCFromAll(VirtualNPC npc) {
        if (plugin.isShuttingDown()) {
            hideNPCFromAllImmediately(npc);
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerUtil().runEntity(player, () -> hide(player, npc));
        }
    }

    public void refreshNPC(VirtualNPC npc) {
        if (plugin.isShuttingDown()) {
            return;
        }
        wakeNPC(npc);
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerUtil().runEntity(player, () -> {
                if (isVisible(player, npc)) {
                    hide(player, npc);
                }
                refreshVisibility(player);
                updateRotations(player);
            });
        }
    }

    public void refreshAll() {
        if (plugin.isShuttingDown()) {
            return;
        }
        sleeping.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerUtil().runEntity(player, () -> {
                hideAll(player);
                refreshVisibility(player);
                updateRotations(player);
            });
        }
    }

    public void wakeNPC(VirtualNPC npc) {
        lastActiveMillis.put(npc.getId(), System.currentTimeMillis());
        sleeping.remove(npc.getId());
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
                wakeNPC(npc.get());
                return npc;
            }
        }
        return Optional.empty();
    }

    public int viewerCount(VirtualNPC npc) {
        Set<UUID> viewers = viewersByNpc.get(npc.getId());
        return viewers == null ? 0 : viewers.size();
    }

    public void shutdownNow() {
        cancelAllTasks();
        hideAllImmediately();
        visible.clear();
        viewersByNpc.clear();
        turningToPlayer.clear();
        sleeping.clear();
        lastActiveMillis.clear();
    }

    public void hideAllImmediately() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideAllImmediately(player);
        }
    }

    public void hideAllImmediately(Player player) {
        Set<String> ids = visible.remove(player.getUniqueId());
        if (ids != null) {
            for (String id : ids) {
                plugin.getNpcManager().get(id).ifPresent(npc -> removeViewer(player.getUniqueId(), npc));
            }
        } else {
            removePlayerFromViewerCache(player.getUniqueId());
        }
        turningToPlayer.remove(player.getUniqueId());
        if (plugin.getPacketManager() != null) {
            plugin.getPacketManager().hideAll(player);
        }
    }

    private void hideImmediately(Player player, VirtualNPC npc) {
        Set<String> playerVisible = visible.get(player.getUniqueId());
        if (playerVisible != null) {
            playerVisible.remove(npc.getId());
            forgetTurnToPlayer(player.getUniqueId(), npc.getId());
            removeViewer(player.getUniqueId(), npc);
            if (playerVisible.isEmpty()) {
                visible.remove(player.getUniqueId());
            }
        }
        if (plugin.getPacketManager() != null) {
            plugin.getPacketManager().hide(player, npc);
        }
    }

    private void hideNPCFromAllImmediately(VirtualNPC npc) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideImmediately(player, npc);
        }
    }

    private boolean shouldSee(Location playerLocation, VirtualNPC npc) {
        if (playerLocation.getWorld() == null || !playerLocation.getWorld().getName().equals(npc.getPosition().world())) {
            return false;
        }
        double distanceSquared = distanceSquared(playerLocation, npc.getPosition());
        if (isSleeping(npc) && distanceSquared > detectionDistanceSquared(npc)) {
            return false;
        }
        if (isSleeping(npc)) {
            wakeNPC(npc);
        }
        double viewDistance = visibilityDistance(npc);
        return distanceSquared <= viewDistance * viewDistance;
    }

    private void updateTurnToPlayer(Player player, Location playerLocation, VirtualNPC npc) {
        if (!npc.isTurnToPlayer() || playerLocation.getWorld() == null || !playerLocation.getWorld().getName().equals(npc.getPosition().world())) {
            resetTurnToPlayer(player, npc);
            return;
        }
        double distance = rotationDistance(npc);
        if (distance <= 0.0D || distanceSquared(playerLocation, npc.getPosition()) > distance * distance) {
            resetTurnToPlayer(player, npc);
            return;
        }
        float yaw = yawTo(player, npc);
        float pitch = pitchTo(player, npc);
        turningToPlayer.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(npc.getId());
        wakeNPC(npc);
        plugin.getPacketManager().updateRotation(player, npc, yaw, pitch);
    }

    private void resetTurnToPlayer(Player player, VirtualNPC npc) {
        if (!forgetTurnToPlayer(player.getUniqueId(), npc.getId())) {
            return;
        }
        plugin.getPacketManager().updateRotation(player, npc, npc.getPosition().yaw(), npc.getPosition().pitch());
    }

    private boolean forgetTurnToPlayer(UUID playerId, String npcId) {
        Set<String> playerTurning = turningToPlayer.get(playerId);
        if (playerTurning == null) {
            return false;
        }
        boolean removed = playerTurning.remove(npcId);
        if (playerTurning.isEmpty()) {
            turningToPlayer.remove(playerId);
        }
        return removed;
    }

    private void addViewer(UUID playerId, VirtualNPC npc) {
        viewersByNpc.computeIfAbsent(npc.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(playerId);
        wakeNPC(npc);
    }

    private void removeViewer(UUID playerId, VirtualNPC npc) {
        Set<UUID> viewers = viewersByNpc.get(npc.getId());
        if (viewers != null) {
            viewers.remove(playerId);
            if (viewers.isEmpty()) {
                viewersByNpc.remove(npc.getId());
                lastActiveMillis.put(npc.getId(), System.currentTimeMillis());
            }
        }
        updateIdleState(npc);
    }

    private void removePlayerFromViewerCache(UUID playerId) {
        for (String npcId : Set.copyOf(viewersByNpc.keySet())) {
            Set<UUID> viewers = viewersByNpc.get(npcId);
            if (viewers == null) {
                continue;
            }
            viewers.remove(playerId);
            if (viewers.isEmpty()) {
                viewersByNpc.remove(npcId);
                lastActiveMillis.put(npcId, System.currentTimeMillis());
            }
        }
    }

    private void updateIdleState(VirtualNPC npc) {
        if (!idleModeEnabled()) {
            sleeping.remove(npc.getId());
            return;
        }
        Set<UUID> viewers = viewersByNpc.get(npc.getId());
        if (viewers != null && !viewers.isEmpty()) {
            wakeNPC(npc);
            return;
        }
        long now = System.currentTimeMillis();
        long lastActive = lastActiveMillis.computeIfAbsent(npc.getId(), ignored -> now);
        if (now - lastActive >= sleepAfterMillis()) {
            sleeping.add(npc.getId());
        }
    }

    private boolean isSleeping(VirtualNPC npc) {
        return idleModeEnabled() && sleeping.contains(npc.getId());
    }

    private double visibilityDistance(VirtualNPC npc) {
        double distance = Math.max(1.0D, npc.getViewDistance());
        if (optimizationEnabled()) {
            distance = Math.min(distance, configDouble("optimization.visibility.max-distance", distance));
        }
        return Math.max(1.0D, distance);
    }

    private double detectionDistanceSquared(VirtualNPC npc) {
        double distance = Math.max(visibilityDistance(npc), configDouble("optimization.idle-mode.wake-up-range", visibilityDistance(npc)));
        return distance * distance;
    }

    private double rotationDistance(VirtualNPC npc) {
        double distance = Math.max(0.0D, npc.getTurnToPlayerDistance());
        if (optimizationEnabled()) {
            distance = Math.min(distance, configDouble("optimization.rotation.max-distance", distance));
        }
        return Math.max(0.0D, distance);
    }

    private long joinDelayTicks() {
        return Math.max(0L, plugin.getConfig().getLong("rendering.join-delay-ticks", 20L));
    }

    private long visibilityIntervalTicks() {
        long legacy = Math.max(1L, plugin.getConfig().getLong("rendering.update-interval-ticks", 10L));
        if (!optimizationEnabled()) {
            return legacy;
        }
        long visibility = Math.max(1L, plugin.getConfig().getLong("optimization.visibility.update-interval-ticks", legacy));
        if (!plugin.getConfig().getBoolean("optimization.cache.viewers", true)) {
            return visibility;
        }
        return Math.max(1L, plugin.getConfig().getLong("optimization.cache.refresh-interval-ticks", visibility));
    }

    private long rotationIntervalTicks() {
        long legacy = Math.max(1L, plugin.getConfig().getLong("rendering.update-interval-ticks", 10L));
        if (!optimizationEnabled()) {
            return legacy;
        }
        return Math.max(1L, plugin.getConfig().getLong("optimization.rotation.update-interval-ticks", legacy));
    }

    private long sleepAfterMillis() {
        return Math.max(0L, (long) (configDouble("optimization.idle-mode.sleep-after-seconds", 10.0D) * MILLIS_PER_SECOND));
    }

    private boolean optimizationEnabled() {
        return plugin.getConfig().getBoolean("optimization.enabled", false);
    }

    private boolean idleModeEnabled() {
        return optimizationEnabled() && plugin.getConfig().getBoolean("optimization.idle-mode.enabled", true);
    }

    private boolean rotationEnabled() {
        return !optimizationEnabled() || plugin.getConfig().getBoolean("optimization.rotation.enabled", true);
    }

    private double configDouble(String path, double fallback) {
        return plugin.getConfig().contains(path) ? plugin.getConfig().getDouble(path) : fallback;
    }

    private double distanceSquared(Location playerLocation, NPCPosition position) {
        double dx = playerLocation.getX() - position.x();
        double dy = playerLocation.getY() - position.y();
        double dz = playerLocation.getZ() - position.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private float yawTo(Player player, VirtualNPC npc) {
        NPCPosition position = npc.getPosition();
        Location target = player.getEyeLocation();
        double dx = target.getX() - position.x();
        double dz = target.getZ() - position.z();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private float pitchTo(Player player, VirtualNPC npc) {
        NPCPosition position = npc.getPosition();
        Location target = player.getEyeLocation();
        double dx = target.getX() - position.x();
        double dz = target.getZ() - position.z();
        double eyeY = position.y() + 1.62D * npc.getScale();
        double dy = target.getY() - eyeY;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, horizontal));
    }

    private void track(Player player, SchedulerUtil.TaskHandle task) {
        if (task == null) {
            return;
        }
        tasks.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(task);
    }

    private void cancelTasks(UUID playerId) {
        Set<SchedulerUtil.TaskHandle> playerTasks = tasks.remove(playerId);
        if (playerTasks == null) {
            return;
        }
        for (SchedulerUtil.TaskHandle task : playerTasks) {
            task.cancel();
        }
    }

    private void cancelAllTasks() {
        for (UUID playerId : Set.copyOf(tasks.keySet())) {
            cancelTasks(playerId);
        }
    }
}
