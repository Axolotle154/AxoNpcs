package org.axostudio.axonpcs.listener

import org.axostudio.axonpcs.protocol.adapter.ProtocolAdapter
import org.axostudio.axonpcs.runtime.registry.NpcRegistry
import org.axostudio.axonpcs.runtime.service.NpcActionHandler
import org.axostudio.axonpcs.tracking.sync.WorldSyncManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerConnectionListener(
    private val worldSyncManager: WorldSyncManager,
    private val protocol: ProtocolAdapter,
    private val npcRegistry: NpcRegistry,
    private val actionHandler: NpcActionHandler
) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        protocol.inject(player) { uuid, interactPacket ->
            val npc = npcRegistry.getByEntityId(interactPacket.entityId()) ?: return@inject false
            actionHandler.handleInteract(player, npc, interactPacket.trigger())
            true
        }
        worldSyncManager.handleJoin(player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        actionHandler.clear(player)
        worldSyncManager.handleQuit(player)
    }
}
