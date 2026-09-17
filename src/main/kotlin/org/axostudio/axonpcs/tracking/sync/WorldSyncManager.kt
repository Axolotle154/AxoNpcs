package org.axostudio.axonpcs.tracking.sync

import org.axostudio.axonpcs.config.ConfigManager
import org.axostudio.axonpcs.platform.scheduler.SchedulerAdapter
import org.axostudio.axonpcs.protocol.adapter.ProtocolAdapter
import org.axostudio.axonpcs.tracking.viewer.ViewerState
import org.axostudio.axonpcs.tracking.viewer.VisibilityController
import org.bukkit.entity.Player

class WorldSyncManager(
    private val visibilityController: VisibilityController,
    private val protocol: ProtocolAdapter,
    private val configManager: ConfigManager,
    private val scheduler: SchedulerAdapter,
    private val isBedrockPlayer: (Player) -> Boolean
) {

    fun handleJoin(player: Player) {
        val tracker = visibilityController.getTracker(player)
        tracker.state = ViewerState.CONNECTED

        // Give the client time to complete login terrain generation before spawning entities
        val delay = maxOf(2L, configManager.settings.joinDelayTicks)
        scheduler.runEntityDelayed(player, delay) {
            if (player.isOnline) {
                tracker.state = ViewerState.ACTIVE
                visibilityController.tickPlayer(player, tracker)
            }
        }
    }

    fun handleQuit(player: Player) {
        visibilityController.removeTracker(player)
        protocol.uninject(player)
    }

    fun handleWorldChange(player: Player) {
        val tracker = visibilityController.getTracker(player)
        // Set to TRANSITIONING: clears old entity mappings without sending invalid packets
        tracker.resetForWorldTransition()

        val delay = if (isBedrockPlayer(player)) {
            configManager.settings.bedrockWorldChangeDelayTicks
        } else {
            configManager.settings.worldChangeDelayTicks
        }

        // First attempt after world change
        scheduler.runEntityDelayed(player, maxOf(2L, delay)) {
            if (player.isOnline) {
                tracker.state = ViewerState.ACTIVE
                visibilityController.tickPlayer(player, tracker)
            }
        }

        // Safety second check 20 ticks later in case client chunk loading took longer
        scheduler.runEntityDelayed(player, delay + 20L) {
            if (player.isOnline && tracker.state == ViewerState.ACTIVE) {
                visibilityController.tickPlayer(player, tracker)
            }
        }
    }

    fun handleRespawn(player: Player) {
        handleWorldChange(player)
    }

    fun handleTeleport(player: Player, fromWorld: String?, toWorld: String?, distanceSquared: Double) {
        if (fromWorld != null && toWorld != null && !fromWorld.equals(toWorld, ignoreCase = true)) {
            // World change handled by onWorldChange
            return
        }

        if (distanceSquared > 64.0 * 64.0) {
            // Long-distance teleport in same world: hide far away NPCs and re-check
            val tracker = visibilityController.getTracker(player)
            scheduler.runEntityDelayed(player, 2L) {
                if (player.isOnline && tracker.state == ViewerState.ACTIVE) {
                    visibilityController.tickPlayer(player, tracker)
                }
            }
        }
    }
}
