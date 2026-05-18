package org.axostudio.axonpcs.packet;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public final class EntityIdAllocator {
    private static final AtomicInteger FALLBACK_COUNTER = new AtomicInteger(1_000_000_000);
    private static final Method NEXT_ENTITY_ID = findNextEntityIdMethod();

    private EntityIdAllocator() {
    }

    public static int nextEntityId() {
        if (NEXT_ENTITY_ID != null) {
            try {
                return (Integer) NEXT_ENTITY_ID.invoke(null);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return FALLBACK_COUNTER.getAndIncrement();
    }

    private static Method findNextEntityIdMethod() {
        try {
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity", false, EntityIdAllocator.class.getClassLoader());
            Method method = entityClass.getMethod("nextEntityId");
            method.setAccessible(true);
            return method;
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError exception) {
            return null;
        }
    }
}
