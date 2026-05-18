package org.axostudio.axonpcs.packet;

import org.axostudio.axonpcs.AxoNPCsPlugin;

public final class PacketBackendFactory {
    private PacketBackendFactory() {
    }

    public static PacketBackend create(AxoNPCsPlugin plugin) {
        return new NativePacketBackend(plugin);
    }
}
