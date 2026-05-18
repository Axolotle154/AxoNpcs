package org.axostudio.axonpcs.packet;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.axostudio.axonpcs.model.VirtualNPC;
import org.axostudio.axonpcs.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public final class NativePacketBackend implements PacketBackend {
    private static final String HANDLER_NAME = "axonpcs_native_interact";

    private final AxoNPCsPlugin plugin;
    private final Map<UUID, Map<String, NativeNpcSession>> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> injectedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<SchedulerUtil.TaskHandle> tabRemoveTasks = ConcurrentHashMap.newKeySet();
    private NativePacketFactory packets;

    public NativePacketBackend(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "Native Paper";
    }

    @Override
    public boolean enable() {
        try {
            packets = new NativePacketFactory(plugin);
            for (Player player : Bukkit.getOnlinePlayers()) {
                inject(player);
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize native packet backend.", exception);
            return false;
        }
    }

    @Override
    public void disable() {
        for (SchedulerUtil.TaskHandle task : tabRemoveTasks) {
            task.cancel();
        }
        tabRemoveTasks.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideAll(player);
            uninject(player);
        }
        sessions.clear();
        injectedPlayers.clear();
        packets = null;
    }

    @Override
    public void inject(Player player) {
        if (plugin.isShuttingDown() || !player.isOnline() || packets == null) {
            return;
        }
        Object channel = packets.channel(player);
        if (!(channel instanceof io.netty.channel.Channel nettyChannel)) {
            return;
        }
        injectedPlayers.add(player.getUniqueId());
        Runnable injectTask = () -> {
            ChannelPipeline pipeline = nettyChannel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                return;
            }
            NativeInboundHandler handler = new NativeInboundHandler(player.getUniqueId());
            if (pipeline.get("packet_handler") != null) {
                pipeline.addBefore("packet_handler", HANDLER_NAME, handler);
            } else {
                pipeline.addLast(HANDLER_NAME, handler);
            }
        };
        if (nettyChannel.eventLoop().inEventLoop()) {
            injectTask.run();
        } else {
            nettyChannel.eventLoop().execute(injectTask);
        }
    }

    @Override
    public void uninject(Player player) {
        injectedPlayers.remove(player.getUniqueId());
        if (packets == null) {
            return;
        }
        Object channel = packets.channel(player);
        if (!(channel instanceof io.netty.channel.Channel nettyChannel)) {
            return;
        }
        Runnable removeTask = () -> {
            ChannelPipeline pipeline = nettyChannel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
        };
        if (nettyChannel.eventLoop().inEventLoop()) {
            removeTask.run();
        } else if (plugin.isShuttingDown()) {
            removeTask.run();
        } else {
            nettyChannel.eventLoop().execute(removeTask);
        }
    }

    @Override
    public boolean show(Player player, VirtualNPC npc) {
        if (plugin.isShuttingDown() || packets == null || !player.isOnline()) {
            return false;
        }
        if (!npc.getType().equalsIgnoreCase("PLAYER")) {
            plugin.getLogger().fine("Only PLAYER native NPCs are implemented right now; got " + npc.getType());
        }
        inject(player);
        hide(player, npc);
        try {
            NativeNpcSession session = packets.createSession(player, npc);
            packets.send(player, packets.playerInfoAdd(session));
            packets.send(player, packets.teamCreate(session));
            packets.send(player, packets.addEntity(session));
            packets.send(player, packets.metadata(session));
            packets.send(player, packets.equipment(npc));
            packets.send(player, packets.attributes(npc));
            packets.send(player, packets.bodyRotation(npc));
            Object headRotation = packets.headRotation(npc);
            if (headRotation != null) {
                packets.send(player, headRotation);
            }
            sessions.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>()).put(npc.getId(), session);
            scheduleTabRemove(player, npc, session);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not show native NPC " + npc.getId() + " to " + player.getName(), exception);
            return false;
        }
    }

    @Override
    public void hide(Player player, VirtualNPC npc) {
        Map<String, NativeNpcSession> playerSessions = sessions.get(player.getUniqueId());
        if (playerSessions == null) {
            return;
        }
        NativeNpcSession session = playerSessions.remove(npc.getId());
        if (session == null || packets == null) {
            return;
        }
        try {
            packets.send(player, packets.removeEntity(npc));
            packets.send(player, packets.playerInfoRemove(session));
            packets.send(player, packets.teamRemove(session));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            plugin.getLogger().log(Level.FINE, "Could not hide native NPC " + npc.getId() + " from " + player.getName(), exception);
        }
        if (playerSessions.isEmpty()) {
            sessions.remove(player.getUniqueId());
        }
    }

    @Override
    public void hideAll(Player player) {
        Map<String, NativeNpcSession> playerSessions = sessions.remove(player.getUniqueId());
        if (playerSessions == null || packets == null) {
            return;
        }
        for (NativeNpcSession session : playerSessions.values()) {
            try {
                packets.send(player, packets.removeEntity(session.npc()));
                packets.send(player, packets.playerInfoRemove(session));
                packets.send(player, packets.teamRemove(session));
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                plugin.getLogger().log(Level.FINE, "Could not hide native NPC " + session.npc().getId() + " from " + player.getName(), exception);
            }
        }
    }

    @Override
    public void updateRotation(VirtualNPC npc) {
        if (plugin.isShuttingDown() || packets == null) {
            return;
        }
        for (Map.Entry<UUID, Map<String, NativeNpcSession>> entry : sessions.entrySet()) {
            if (!entry.getValue().containsKey(npc.getId())) {
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }
            try {
                packets.send(player, packets.bodyRotation(npc));
                Object headRotation = packets.headRotation(npc);
                if (headRotation != null) {
                    packets.send(player, headRotation);
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                plugin.getLogger().log(Level.FINE, "Could not update native NPC rotation for " + npc.getId(), exception);
            }
        }
    }

    @Override
    public void updateRotation(Player player, VirtualNPC npc, float yaw, float pitch) {
        if (plugin.isShuttingDown() || packets == null || player == null || !player.isOnline()) {
            return;
        }
        Map<String, NativeNpcSession> playerSessions = sessions.get(player.getUniqueId());
        if (playerSessions == null || !playerSessions.containsKey(npc.getId())) {
            return;
        }
        try {
            packets.send(player, packets.bodyRotation(npc, yaw, pitch));
            Object headRotation = packets.headRotation(npc, yaw);
            if (headRotation != null) {
                packets.send(player, headRotation);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            plugin.getLogger().log(Level.FINE, "Could not update native NPC look target for " + npc.getId(), exception);
        }
    }

    @Override
    public int viewerCount(VirtualNPC npc) {
        int count = 0;
        for (Map<String, NativeNpcSession> playerSessions : sessions.values()) {
            if (playerSessions.containsKey(npc.getId())) {
                count++;
            }
        }
        return count;
    }

    private void scheduleTabRemove(Player player, VirtualNPC npc, NativeNpcSession session) {
        if (plugin.isShuttingDown()) {
            return;
        }
        AtomicReference<SchedulerUtil.TaskHandle> handleRef = new AtomicReference<>();
        SchedulerUtil.TaskHandle handle = plugin.getSchedulerUtil().runEntityDelayed(player, () -> {
            SchedulerUtil.TaskHandle task = handleRef.get();
            if (task != null) {
                tabRemoveTasks.remove(task);
            }
            Map<String, NativeNpcSession> playerSessions = sessions.get(player.getUniqueId());
            if (plugin.isShuttingDown() || playerSessions == null || playerSessions.get(npc.getId()) != session || packets == null) {
                return;
            }
            try {
                packets.send(player, packets.playerInfoRemove(session));
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                plugin.getLogger().log(Level.FINE, "Could not remove native NPC " + npc.getId() + " from tab list", exception);
            }
        }, 40L);
        handleRef.set(handle);
        tabRemoveTasks.add(handle);
    }

    private boolean handleInboundPacket(UUID playerId, Object packet) {
        if (plugin.isShuttingDown() || !injectedPlayers.contains(playerId) || packets == null) {
            return false;
        }
        NPCActionTrigger trigger = packets.interactionTrigger(packet);
        if (trigger == null) {
            return false;
        }
        int entityId = packets.interactionEntityId(packet);
        if (entityId < 0) {
            return false;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return false;
        }
        boolean matched = plugin.getViewerManager().findVisibleByEntityId(playerId, entityId).map(npc -> {
            if (plugin.isShuttingDown()) {
                return false;
            }
            plugin.getSchedulerUtil().runEntity(player, () -> plugin.getActionManager().handleInteract(player, npc, trigger));
            return true;
        }).orElse(false);
        return matched;
    }

    private final class NativeInboundHandler extends ChannelDuplexHandler {
        private final UUID playerId;

        private NativeInboundHandler(UUID playerId) {
            this.playerId = playerId;
        }

        @Override
        public void channelRead(ChannelHandlerContext context, Object packet) throws Exception {
            if (handleInboundPacket(playerId, packet)) {
                return;
            }
            super.channelRead(context, packet);
        }
    }

    record NativeNpcSession(VirtualNPC npc, String profileName, String teamName, Object gameProfile) {
    }
}
