package org.axostudio.axonpcs.packet;

import org.axostudio.axonpcs.model.VirtualNPC;
import org.bukkit.entity.Player;

/**
 * Internal packet abstraction used by AxoNPCs.
 *
 * <p>This keeps the rest of the plugin independent from the native packet
 * implementation details.</p>
 */
public interface PacketBackend {
    String name();

    boolean enable();

    void disable();

    void inject(Player player);

    void uninject(Player player);

    boolean show(Player player, VirtualNPC npc);

    void hide(Player player, VirtualNPC npc);

    void hideAll(Player player);

    void updateRotation(VirtualNPC npc);

    void updateRotation(Player player, VirtualNPC npc, float yaw, float pitch);

    int viewerCount(VirtualNPC npc);
}
