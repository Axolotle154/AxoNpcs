package org.axostudio.axonpcs.tracking.viewer

import org.axostudio.axonpcs.config.ConfigManager
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcRuntimeState
import org.axostudio.axonpcs.platform.scheduler.SchedulerAdapter
import org.axostudio.axonpcs.protocol.adapter.ProtocolAdapter
import org.axostudio.axonpcs.render.session.NativeNpcSession
import org.bukkit.entity.Player
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class PlayerTracker(
    val player: Player,
    private val protocol: ProtocolAdapter,
    private val configManager: ConfigManager,
    private val scheduler: SchedulerAdapter,
    private val nextEntityIdProvider: () -> Int
) {
    @Volatile
    var state: ViewerState = ViewerState.CONNECTED

    private val visibleSessions = ConcurrentHashMap<String, NativeNpcSession>()

    fun isVisible(npcId: String): Boolean =
        visibleSessions.containsKey(npcId.lowercase(Locale.ROOT))

    fun getSession(npcId: String): NativeNpcSession? =
        visibleSessions[npcId.lowercase(Locale.ROOT)]

    fun getVisibleIds(): Set<String> = visibleSessions.keys

    fun show(npc: NpcDefinition, runtimeState: NpcRuntimeState): Boolean {
        if (!player.isOnline || state != ViewerState.ACTIVE) return false
        val key = npc.id.lowercase(Locale.ROOT)
        if (visibleSessions.containsKey(key)) return false

        val session = NativeNpcSession(
            viewer = player,
            npc = npc,
            runtimeState = runtimeState,
            protocol = protocol,
            configManager = configManager,
            scheduler = scheduler,
            nextEntityIdProvider = nextEntityIdProvider
        )

        if (!session.spawn()) return false

        visibleSessions[key] = session
        runtimeState.addViewer(player.uniqueId)
        return true
    }

    fun hide(npc: NpcDefinition, runtimeState: NpcRuntimeState?): Boolean {
        val key = npc.id.lowercase(Locale.ROOT)
        val session = visibleSessions.remove(key) ?: return false

        runtimeState?.removeViewer(player.uniqueId)
        session.despawn()
        return true
    }

    fun hideAll() {
        for ((_, session) in visibleSessions) {
            session.runtimeState.removeViewer(player.uniqueId)
            session.despawn()
        }
        visibleSessions.clear()
    }

    /**
     * Called when the client changes dimensions or respawns.
     * The Minecraft client automatically purges all client-side entities on dimension change,
     * so we clear our internal map without dispatching stale destroy packets.
     */
    fun resetForWorldTransition() {
        state = ViewerState.TRANSITIONING
        for ((_, session) in visibleSessions) {
            session.runtimeState.removeViewer(player.uniqueId)
        }
        visibleSessions.clear()
    }
}
