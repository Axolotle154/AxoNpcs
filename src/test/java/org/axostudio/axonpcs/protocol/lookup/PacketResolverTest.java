package org.axostudio.axonpcs.protocol.lookup;

import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketResolverTest {
    @Test
    void allocatesPacketWithoutCallingEntityConstructorAndPopulatesFinalFields() throws Throwable {
        MethodHandle allocator = PacketResolver.allocator(EntityConstructorPacket.class);
        EntityConstructorPacket packet = (EntityConstructorPacket) allocator.invoke();

        PacketResolver.setter(EntityConstructorPacket.class, "entityId").invoke(packet, 42);
        PacketResolver.setter(EntityConstructorPacket.class, "yHeadRot").invoke(packet, (byte) 64);

        assertEquals(42, packet.entityId);
        assertEquals((byte) 64, packet.yHeadRot);
    }

    private static final class EntityConstructorPacket {
        private final int entityId;
        private final byte yHeadRot;

        private EntityConstructorPacket(Object entity, byte yHeadRot) {
            throw new AssertionError("The NMS entity constructor must not be invoked");
        }
    }
}
