package org.axostudio.axonpcs.protocol.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.axostudio.axonpcs.protocol.inbound.InboundInteractPacket;
import org.axostudio.axonpcs.protocol.packet.NativePacketFactory;

import java.util.UUID;
import java.util.function.BiPredicate;

public final class NativeInboundHandler extends ChannelDuplexHandler {
    private final UUID playerUuid;
    private final NativePacketFactory packetFactory;
    private final BiPredicate<UUID, InboundInteractPacket> interactionListener;

    public NativeInboundHandler(UUID playerUuid, NativePacketFactory packetFactory,
                                BiPredicate<UUID, InboundInteractPacket> interactionListener) {
        this.playerUuid = playerUuid;
        this.packetFactory = packetFactory;
        this.interactionListener = interactionListener;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        InboundInteractPacket interact = packetFactory.decodeInbound(msg);
        if (interact != null && interactionListener.test(playerUuid, interact)) {
            // Only virtual entity interactions are consumed. Real entities must continue
            // through vanilla's handler so normal attacks and interactions still work.
            return;
        }
        super.channelRead(ctx, msg);
    }
}
