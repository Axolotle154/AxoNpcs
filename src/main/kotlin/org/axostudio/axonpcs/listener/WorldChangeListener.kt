package org.axostudio.axonpcs.listener

import org.axostudio.axonpcs.tracking.sync.WorldSyncManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent

class WorldChangeListener(
    private val worldSyncManager: WorldSyncManager
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        worldSyncManager.handleWorldChange(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onRespawn(event: PlayerRespawnEvent) {
        worldSyncManager.handleRespawn(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) {
        val fromWorld = event.from.world?.name
        val toWorld = event.to.world?.name
        val distSq = if (fromWorld != null && toWorld != null && fromWorld == toWorld) {
            event.from.distanceSquared(event.to)
        } else {
            Double.MAX_VALUE
        }
        worldSyncManager.handleTeleport(event.player, fromWorld, toWorld, distSq)
    }
}
