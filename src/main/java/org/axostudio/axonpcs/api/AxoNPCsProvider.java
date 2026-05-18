package org.axostudio.axonpcs.api;

/**
 * Static provider for plugins that want to access AxoNPCs after it is enabled.
 */
public final class AxoNPCsProvider {
    private static volatile AxoNPCsAPI api;

    private AxoNPCsProvider() {
    }

    public static AxoNPCsAPI getAPI() {
        AxoNPCsAPI current = api;
        if (current == null) {
            throw new IllegalStateException("AxoNPCs API is not available yet");
        }
        return current;
    }

    public static boolean isAvailable() {
        return api != null;
    }

    public static void register(AxoNPCsAPI api) {
        AxoNPCsProvider.api = api;
    }

    public static void unregister(AxoNPCsAPI api) {
        if (AxoNPCsProvider.api == api) {
            AxoNPCsProvider.api = null;
        }
    }
}
