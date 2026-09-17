package org.axostudio.axonpcs.protocol.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.axostudio.axonpcs.protocol.inbound.InboundInteractPacket;
import org.axostudio.axonpcs.protocol.lookup.MethodHandleTable;
import org.axostudio.axonpcs.protocol.packet.NativePacketFactory;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

public final class ChannelInjector {
    public static final String HANDLER_NAME = "axonpcs_interact_handler";
    private final MethodHandleTable table;
    private final NativePacketFactory packetFactory;
    private final Set<UUID> injected = ConcurrentHashMap.newKeySet();

    public ChannelInjector(MethodHandleTable table, NativePacketFactory packetFactory) {
        this.table = table;
        this.packetFactory = packetFactory;
    }

    public Channel getChannel(Player player) {
        try {
            Object nmsPlayer = table.craftPlayerGetHandle.invoke(player);
            Object listener = table.serverPlayerConnectionGetter.invoke(nmsPlayer);
            Object connection = table.connectionConnectionGetter.invoke(listener);
            return (Channel) table.connectionChannelGetter.invoke(connection);
        } catch (Throwable t) {
            return null;
        }
    }

    public void inject(Player player, BiPredicate<UUID, InboundInteractPacket> interactionListener) {
        if (!player.isOnline()) return;
        Channel channel = getChannel(player);
        if (channel == null) return;

        injected.add(player.getUniqueId());
        Runnable task = () -> {
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
            NativeInboundHandler handler = new NativeInboundHandler(player.getUniqueId(), packetFactory, interactionListener);
            if (pipeline.get("packet_handler") != null) {
                pipeline.addBefore("packet_handler", HANDLER_NAME, handler);
            } else {
                pipeline.addLast(HANDLER_NAME, handler);
            }
        };

        if (channel.eventLoop().inEventLoop()) {
            task.run();
        } else {
            channel.eventLoop().execute(task);
        }
    }

    public void uninject(Player player) {
        injected.remove(player.getUniqueId());
        Channel channel = getChannel(player);
        if (channel == null) return;

        Runnable task = () -> {
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
        };

        if (channel.eventLoop().inEventLoop()) {
            task.run();
        } else {
            channel.eventLoop().execute(task);
        }
    }

    public boolean isInjected(UUID uuid) {
        return injected.contains(uuid);
    }
}
