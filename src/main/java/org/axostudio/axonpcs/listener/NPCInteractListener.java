package org.axostudio.axonpcs.listener;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.entity.Player;

public final class NPCInteractListener implements PacketListener {
    private final AxoNPCsPlugin plugin;

    public NPCInteractListener(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        int entityId = -1;
        NPCActionTrigger trigger = NPCActionTrigger.RIGHT_CLICK;
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            entityId = new WrapperPlayClientInteractEntity(event).getEntityId();
        } else if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            entityId = new WrapperPlayClientAttack(event).getEntityId();
            trigger = NPCActionTrigger.LEFT_CLICK;
        }
        if (entityId == -1 || !(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        int finalEntityId = entityId;
        NPCActionTrigger finalTrigger = trigger;
        plugin.getViewerManager().findVisibleByEntityId(player.getUniqueId(), finalEntityId).ifPresent(npc -> {
            event.setCancelled(true);
            plugin.getSchedulerUtil().runEntity(player, () -> plugin.getActionManager().handleInteract(player, npc, finalTrigger));
        });
    }
}
