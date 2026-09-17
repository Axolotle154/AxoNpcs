package org.axostudio.axonpcs.render.session

import net.kyori.adventure.text.Component
import org.axostudio.axonpcs.config.ConfigManager
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcRuntimeState
import org.axostudio.axonpcs.domain.npc.NpcTransform
import org.axostudio.axonpcs.platform.scheduler.SchedulerAdapter
import org.axostudio.axonpcs.platform.scheduler.TaskHandle
import org.axostudio.axonpcs.protocol.adapter.ProtocolAdapter
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.UUID
import java.util.logging.Level

class NativeNpcSession(
    val viewer: Player,
    val npc: NpcDefinition,
    val runtimeState: NpcRuntimeState,
    val protocol: ProtocolAdapter,
    val configManager: ConfigManager,
    val scheduler: SchedulerAdapter,
    nextEntityIdProvider: () -> Int
) {
    val isPlayerLike: Boolean = npc.type.equals("PLAYER", ignoreCase = true)
    val profileName: String = if (isPlayerLike) {
        val baseId = npc.id.take(14)
        "!$baseId"
    } else ""
    val teamName: String = "axo_${npc.id.take(12)}"

    var gameProfile: Any? = null
        private set

    val labelLines: List<Component> = parseDisplayNameLines(npc.displayName)
    val labelEntityIds: IntArray = IntArray(labelLines.size) { nextEntityIdProvider() }

    @Volatile
    var renderTransform: NpcTransform = npc.transform

    @Volatile
    var playerInfoRevision: Long = 0L

    private var playerInfoRemovalTask: TaskHandle? = null

    init {
        if (isPlayerLike) {
            try {
                val skin = npc.skin
                gameProfile = protocol.factory().createGameProfile(
                    npc.uuid,
                    profileName,
                    skin.value(),
                    skin.signature()
                )
            } catch (t: Throwable) {
                // Fallback empty profile
            }
        }
    }

    fun spawn(): Boolean {
        if (!viewer.isOnline) return false
        val factory = protocol.factory()

        try {
            // 1. PlayerInfo if player
            if (isPlayerLike && gameProfile != null) {
                val addInfoPacket = factory.playerInfoAdd(npc.uuid, gameProfile, null)
                protocol.sendPacket(viewer, addInfoPacket)
            }

            // 2. Scoreboard team (to hide vanilla nametag and disable collision)
            val teamPacket = factory.teamCreate(teamName, if (isPlayerLike) profileName else npc.uuid.toString(), npc.isCollidable, npc.glowing)
            protocol.sendPacket(viewer, teamPacket)

            // 3. Add Entity packet
            val entityType = try {
                EntityType.valueOf(npc.type.uppercase(java.util.Locale.ROOT))
            } catch (e: Exception) {
                EntityType.PLAYER
            }
            val livingEntityType = entityType.entityClass?.let {
                LivingEntity::class.java.isAssignableFrom(it)
            } == true
            val addEntityPacket = factory.addEntity(runtimeState.entityId, npc.uuid, renderTransform.toBukkitLocation() ?: viewer.location, entityType)
            protocol.sendPacket(viewer, addEntityPacket)

            // 4. Metadata packet
            val metadataPacket = factory.metadata(
                runtimeState.entityId,
                isPlayerLike,
                livingEntityType,
                !npc.glowing.equals("off", ignoreCase = true),
                if (!isPlayerLike && npc.displayName != null) configManager.parse(npc.displayName!!) else null,
                npc.isSoundEnabled,
                if (npc.maxHealth > 0.0) npc.maxHealth else 20.0,
                0.toByte()
            )
            protocol.sendPacket(viewer, metadataPacket)

            // Scale and max health are attributes, not metadata. Only living entity
            // clients accept this packet type.
            if (livingEntityType) {
                val attributesPacket = factory.attributes(runtimeState.entityId, npc.scale, npc.maxHealth)
                if (attributesPacket != null) {
                    protocol.sendPacket(viewer, attributesPacket)
                }
            }

            // 5. Equipment
            val equipPacket = factory.equipment(runtimeState.entityId, npc.equipment)
            protocol.sendPacket(viewer, equipPacket)

            // 6. Rotations
            val rotPacket = factory.bodyRotation(runtimeState.entityId, renderTransform.yaw, renderTransform.pitch)
            protocol.sendPacket(viewer, rotPacket)
            val headPacket = factory.headRotation(runtimeState.entityId, renderTransform.yaw)
            if (headPacket != null) {
                protocol.sendPacket(viewer, headPacket)
            }

            // 7. Hologram labels (Armor Stands for multi-line display)
            spawnHologram()

            // Schedule tablist cleanup after 20 ticks so skin textures cache on client
            if (isPlayerLike) {
                schedulePlayerInfoRemoval()
            }
            return true
        } catch (t: Throwable) {
            // Best-effort cleanup for a partially spawned session, then allow the
            // tracker to retry on a later visibility tick.
            despawn()
            Bukkit.getLogger().log(
                Level.WARNING,
                "[AxoNPCs] Failed to spawn NPC '${npc.id}' for ${viewer.name}",
                t
            )
            return false
        }
    }

    fun despawn() {
        cancelPlayerInfoRemoval()
        val factory = protocol.factory()

        try {
            // Remove main entity
            val removePacket = factory.removeEntities(runtimeState.entityId)
            protocol.sendPacket(viewer, removePacket)

            // Remove hologram labels
            if (labelEntityIds.isNotEmpty()) {
                val removeLabels = factory.removeEntities(*labelEntityIds)
                protocol.sendPacket(viewer, removeLabels)
            }

            // Remove PlayerInfo
            if (isPlayerLike) {
                val removeInfo = factory.playerInfoRemove(npc.uuid)
                protocol.sendPacket(viewer, removeInfo)
            }

            // Remove Team
            val removeTeam = factory.teamRemove(teamName)
            protocol.sendPacket(viewer, removeTeam)
        } catch (t: Throwable) {
        }
    }

    fun updateRotation(yaw: Float, pitch: Float) {
        if (!viewer.isOnline) return
        try {
            val factory = protocol.factory()
            val body = factory.bodyRotation(runtimeState.entityId, yaw, pitch)
            protocol.sendPacket(viewer, body)
            val head = factory.headRotation(runtimeState.entityId, yaw)
            if (head != null) {
                protocol.sendPacket(viewer, head)
            }
        } catch (ignored: Throwable) {
        }
    }

    private fun spawnHologram() {
        if (labelLines.isEmpty()) return
        val factory = protocol.factory()
        val bukkitLoc = renderTransform.toBukkitLocation() ?: return

        for (i in labelLines.indices) {
            val labelId = labelEntityIds[i]
            val line = labelLines[i]
            val yOffset = 1.8 * npc.scale + (labelLines.size - 1 - i) * 0.28
            val labelLoc = bukkitLoc.clone().add(0.0, yOffset, 0.0)

            try {
                val addArmorStand = factory.createArmorStandLabel(labelId, UUID.randomUUID(), labelLoc, line)
                protocol.sendPacket(viewer, addArmorStand)

                val meta = factory.labelMetadata(labelId, line)
                protocol.sendPacket(viewer, meta)
            } catch (ignored: Throwable) {
            }
        }
    }

    private fun schedulePlayerInfoRemoval() {
        cancelPlayerInfoRemoval()
        val currentRev = ++playerInfoRevision

        playerInfoRemovalTask = scheduler.runEntityDelayed(viewer, 20L) {
            if (viewer.isOnline && playerInfoRevision == currentRev) {
                try {
                    val removePacket = protocol.factory().playerInfoRemove(npc.uuid)
                    protocol.sendPacket(viewer, removePacket)
                } catch (ignored: Throwable) {
                }
            }
        }
    }

    private fun cancelPlayerInfoRemoval() {
        playerInfoRevision++
        playerInfoRemovalTask?.cancel()
        playerInfoRemovalTask = null
    }

    private fun parseDisplayNameLines(raw: String?): List<Component> {
        if (raw.isNullOrBlank()) return emptyList()
        val formatted = raw.replace("\\n", "\n")
        return formatted.split("\n").map { configManager.parse(it) }
    }
}
