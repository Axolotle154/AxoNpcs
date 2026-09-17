package org.axostudio.axonpcs.tracking.viewer

import org.axostudio.axonpcs.api.event.npc.AxoNPCHideEvent
import org.axostudio.axonpcs.api.event.npc.AxoNPCShowEvent
import org.axostudio.axonpcs.config.ConfigManager
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.platform.scheduler.SchedulerAdapter
import org.axostudio.axonpcs.platform.scheduler.TaskHandle
import org.axostudio.axonpcs.protocol.adapter.ProtocolAdapter
import org.axostudio.axonpcs.runtime.registry.NpcRegistry
import org.axostudio.axonpcs.tracking.spatial.SpatialChunkGrid
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VisibilityController(
    private val protocol: ProtocolAdapter,
    private val configManager: ConfigManager,
    private val scheduler: SchedulerAdapter,
    private val npcRegistry: NpcRegistry,
    private val spatialGrid: SpatialChunkGrid
) {
    private val trackers = ConcurrentHashMap<UUID, PlayerTracker>()
    private var tickerTask: TaskHandle? = null
    private var rotationTask: TaskHandle? = null

    fun start() {
        stop()
        val interval = maxOf(1L, configManager.settings.visibilityIntervalTicks)
        tickerTask = scheduler.runGlobalRepeating(interval, interval) {
            tickAll()
        }

        if (configManager.settings.rotationEnabled) {
            val rotInterval = maxOf(1L, configManager.settings.rotationIntervalTicks)
            rotationTask = scheduler.runGlobalRepeating(rotInterval, rotInterval) {
                updateAllRotations()
            }
        }
    }

    fun stop() {
        tickerTask?.cancel()
        tickerTask = null
        rotationTask?.cancel()
        rotationTask = null

        trackers.values.forEach { it.hideAll() }
        trackers.clear()
    }

    fun getTracker(player: Player): PlayerTracker {
        return trackers.computeIfAbsent(player.uniqueId) {
            PlayerTracker(player, protocol, configManager, scheduler) { npcRegistry.nextEntityId() }
        }
    }

    fun removeTracker(player: Player) {
        val tracker = trackers.remove(player.uniqueId)
        tracker?.hideAll()
    }

    /** Despawns every client-side NPC while retaining the player trackers. */
    fun hideAllSessions() {
        trackers.values.forEach { tracker ->
            scheduler.runEntity(tracker.player) {
                tracker.hideAll()
            }
        }
    }

    fun hideNpc(npc: NpcDefinition) {
        val state = npcRegistry.getState(npc.id)
        trackers.values.forEach { tracker ->
            scheduler.runEntity(tracker.player) {
                tracker.hide(npc, state)
            }
        }
    }

    /** Recreates one NPC for viewers that currently have a session for it. */
    fun refreshNpc(npc: NpcDefinition) {
        val state = npcRegistry.getState(npc.id) ?: return
        scheduler.runGlobal {
            for (player in Bukkit.getOnlinePlayers()) {
                scheduler.runEntity(player) {
                    val tracker = trackers[player.uniqueId] ?: return@runEntity
                    val wasVisible = tracker.isVisible(npc.id)
                    if (wasVisible) {
                        tracker.hide(npc, state)
                    }
                    if (tracker.state == ViewerState.ACTIVE) {
                        if (wasVisible) {
                            scheduler.runEntityDelayed(player, 2L) {
                                if (tracker.state == ViewerState.ACTIVE && !tracker.isVisible(npc.id)) {
                                    tickPlayer(player, tracker)
                                }
                            }
                        } else {
                            tickPlayer(player, tracker)
                        }
                    }
                }
            }
        }
    }

    fun tickAll() {
        scheduler.runGlobal {
            for (player in Bukkit.getOnlinePlayers()) {
                scheduler.runEntity(player) {
                    val tracker = getTracker(player)
                    if (tracker.state == ViewerState.ACTIVE) {
                        tickPlayer(player, tracker)
                    }
                }
            }
        }
    }

    fun tickPlayer(player: Player, tracker: PlayerTracker = getTracker(player)) {
        if (!player.isOnline || tracker.state != ViewerState.ACTIVE) return
        val playerLoc = player.location
        val searchRadius = configManager.settings.visibilityMaxDistance

        // 1. Find candidates via spatial chunk grid in O(1)
        val candidateIds = spatialGrid.findNearbyNpcIds(playerLoc, searchRadius)
        val currentlyVisible = tracker.getVisibleIds()

        // Hide NPCs that are no longer candidates or out of range
        for (visibleId in currentlyVisible) {
            val npc = npcRegistry.get(visibleId)
            if (npc == null || !shouldSee(playerLoc, npc)) {
                val state = npcRegistry.getState(visibleId)
                if (npc != null) {
                    tracker.hide(npc, state)
                    Bukkit.getPluginManager().callEvent(AxoNPCHideEvent(player, npc))
                }
            }
        }

        // Show candidates that are within view distance
        for (candidateId in candidateIds) {
            if (tracker.isVisible(candidateId)) continue
            val npc = npcRegistry.get(candidateId) ?: continue
            if (shouldSee(playerLoc, npc)) {
                val state = npcRegistry.getState(candidateId) ?: continue
                if (tracker.show(npc, state)) {
                    Bukkit.getPluginManager().callEvent(AxoNPCShowEvent(player, npc))
                }
            }
        }
    }

    fun updateAllRotations() {
        scheduler.runGlobal {
            for (player in Bukkit.getOnlinePlayers()) {
                scheduler.runEntity(player) {
                    val tracker = trackers[player.uniqueId] ?: return@runEntity
                    if (tracker.state != ViewerState.ACTIVE) return@runEntity
                    updatePlayerRotations(player, tracker)
                }
            }
        }
    }

    fun updatePlayerRotations(player: Player, tracker: PlayerTracker) {
        val playerLoc = player.location
        val eyeLoc = player.eyeLocation

        for (npcId in tracker.getVisibleIds()) {
            val npc = npcRegistry.get(npcId) ?: continue
            if (!npc.isTurnToPlayer) continue

            val session = tracker.getSession(npcId) ?: continue
            val distSq = npc.transform.distanceSquared(playerLoc)
            val maxDist = npc.turnToPlayerDistance
            if (distSq > maxDist * maxDist) {
                // Reset to default rotation if player is too far
                session.updateRotation(npc.transform.yaw, npc.transform.pitch)
                continue
            }

            // Calculate look angles
            val dx = eyeLoc.x - npc.transform.x
            val dz = eyeLoc.z - npc.transform.z
            val eyeY = npc.transform.y + 1.62 * npc.scale
            val dy = eyeLoc.y - eyeY
            val horizontal = Math.sqrt(dx * dx + dz * dz)

            val yaw = Math.toDegrees(Math.atan2(-dx, dz)).toFloat()
            val pitch = (-Math.toDegrees(Math.atan2(dy, horizontal))).toFloat()

            session.updateRotation(yaw, pitch)
        }
    }

    private fun shouldSee(playerLoc: Location, npc: NpcDefinition): Boolean {
        if (!npc.isEnabled) return false
        val npcWorld = npc.transform.world
        if (playerLoc.world == null || !playerLoc.world.name.equals(npcWorld, ignoreCase = true)) {
            return false
        }

        val distSq = npc.transform.distanceSquared(playerLoc)
        val maxDist = minOf(npc.viewDistance, configManager.settings.visibilityMaxDistance)
        return distSq <= maxDist * maxDist
    }
}
