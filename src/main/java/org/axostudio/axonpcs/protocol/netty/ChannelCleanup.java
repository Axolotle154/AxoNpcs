package org.axostudio.axonpcs.protocol.netty;

import org.bukkit.entity.Player;

public final class ChannelCleanup {
    private final ChannelInjector injector;

    public ChannelCleanup(ChannelInjector injector) {
        this.injector = injector;
    }

    public void cleanup(Player player) {
        injector.uninject(player);
    }
}
