package org.axostudio.axonpcs.util;

import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicBoolean;

public final class PacketEventsGuard {
    private static final AtomicBoolean VIA_ACCESSOR_WARNING_SENT = new AtomicBoolean(false);

    private PacketEventsGuard() {
    }

    public static boolean canUsePacketEvents(Plugin plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) {
            return true;
        }
        try {
            ViaVersionUtil.checkIfViaIsPresent();
            if (ViaVersionUtil.getViaVersionAccessor() != null) {
                return true;
            }
        } catch (LinkageError | RuntimeException exception) {
            warnOnce(plugin, "PacketEvents could not initialize the ViaVersion hook: " + exception.getMessage());
            return false;
        }
        warnOnce(plugin, "PacketEvents returned a null ViaVersionAccessor. Packet NPC packets are being skipped to avoid a PacketEvents NullPointerException.");
        return false;
    }

    public static void reportViaVersionStatus(Plugin plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) {
            return;
        }
        if (canUsePacketEvents(plugin)) {
            plugin.getLogger().info("PacketEvents ViaVersion hook is ready.");
        }
    }

    private static void warnOnce(Plugin plugin, String message) {
        if (VIA_ACCESSOR_WARNING_SENT.compareAndSet(false, true)) {
            plugin.getLogger().warning(message);
            plugin.getLogger().warning("Check paper-plugin.yml: ViaVersion must load before AxoNPCs and join-classpath must be true when PacketEvents is bundled.");
        }
    }
}
