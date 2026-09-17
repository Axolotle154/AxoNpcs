package org.axostudio.axonpcs.api.service;

import java.util.Objects;

/**
 * Static provider for the AxoNPCService.
 */
public final class AxoNPCProvider {
    private static AxoNPCService instance;

    private AxoNPCProvider() {
    }

    public static AxoNPCService get() {
        AxoNPCService current = instance;
        if (current == null) {
            throw new IllegalStateException("AxoNPCService is not registered yet. Is AxoNPCs enabled?");
        }
        return current;
    }

    public static void register(AxoNPCService service) {
        instance = Objects.requireNonNull(service, "service cannot be null");
    }

    public static void unregister() {
        instance = null;
    }
}
