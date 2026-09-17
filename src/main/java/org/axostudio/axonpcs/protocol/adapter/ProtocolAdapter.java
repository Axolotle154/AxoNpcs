package org.axostudio.axonpcs.protocol.adapter;

import org.axostudio.axonpcs.protocol.inbound.InboundInteractPacket;
import org.axostudio.axonpcs.protocol.lookup.MethodHandleTable;
import org.axostudio.axonpcs.protocol.netty.ChannelInjector;
import org.axostudio.axonpcs.protocol.packet.NativePacketFactory;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.BiPredicate;

public final class ProtocolAdapter {
    private final MethodHandleTable table;
    private final NativePacketFactory packetFactory;
    private final ChannelInjector channelInjector;

    public ProtocolAdapter() throws Exception {
        this.table = new MethodHandleTable();
        this.packetFactory = new NativePacketFactory(table);
        this.channelInjector = new ChannelInjector(table, packetFactory);
    }

    public NativePacketFactory factory() {
        return packetFactory;
    }

    public ProtocolCapabilities capabilities() {
        return table.capabilities;
    }

    public void sendPacket(Player player, Object packet) throws Throwable {
        if (packet == null || !player.isOnline()) return;
        Object handle = table.craftPlayerGetHandle.invoke(player);
        Object listener = table.serverPlayerConnectionGetter.invoke(handle);
        table.sendPacketMethod.invoke(listener, packet);
    }

    public void sendPackets(Player player, Object... packets) {
        if (!player.isOnline()) return;
        try {
            Object handle = table.craftPlayerGetHandle.invoke(player);
            Object listener = table.serverPlayerConnectionGetter.invoke(handle);
            for (Object packet : packets) {
                if (packet != null) {
                    table.sendPacketMethod.invoke(listener, packet);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Installs an inbound handler. The predicate must return {@code true} only when the
     * packet targets one of this plugin's virtual entities and therefore must not reach
     * vanilla's packet handler.
     */
    public void inject(Player player, BiPredicate<UUID, InboundInteractPacket> listener) {
        channelInjector.inject(player, listener);
    }

    public void uninject(Player player) {
        channelInjector.uninject(player);
    }

    public boolean isInjected(UUID uuid) {
        return channelInjector.isInjected(uuid);
    }
}
