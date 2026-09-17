package org.axostudio.axonpcs.runtime.service

import org.axostudio.axonpcs.api.event.npc.AxoNPCCreateEvent
import org.axostudio.axonpcs.api.event.npc.AxoNPCDeleteEvent
import org.axostudio.axonpcs.api.model.AxoNPC
import org.axostudio.axonpcs.api.service.AxoNPCService
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcTransform
import org.axostudio.axonpcs.platform.scheduler.SchedulerAdapter
import org.axostudio.axonpcs.render.skin.SkinService
import org.axostudio.axonpcs.runtime.registry.NpcRegistry
import org.axostudio.axonpcs.storage.repository.NpcRepository
import org.axostudio.axonpcs.tracking.spatial.SpatialChunkGrid
import org.axostudio.axonpcs.tracking.viewer.VisibilityController
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.Locale
import java.util.Optional

class NpcService(
    private val npcRegistry: NpcRegistry,
    private val npcRepository: NpcRepository,
    private val spatialGrid: SpatialChunkGrid,
    private val visibilityController: VisibilityController,
    private val skinService: SkinService,
    private val scheduler: SchedulerAdapter
) : AxoNPCService {

    override fun getNPC(id: String): Optional<AxoNPC> {
        return Optional.ofNullable(npcRegistry.get(id))
    }

    override fun getAllNPCs(): Collection<AxoNPC> {
        return npcRegistry.getAll()
    }

    override fun getNPCsByGroup(group: String?): List<AxoNPC> {
        val all = npcRegistry.getAll()
        val checkNone = group == null || group.isBlank() || group.equals("none", ignoreCase = true) || group.equals("root", ignoreCase = true)
        return all.filter { npc ->
            if (checkNone) {
                npc.group.isNullOrBlank()
            } else {
                group.equals(npc.group, ignoreCase = true)
            }
        }.sortedBy { it.id }
    }

    override fun exists(id: String): Boolean {
        return npcRegistry.get(id) != null || npcRepository.findFile(id) != null
    }

    override fun createNPC(id: String, location: Location): AxoNPC {
        require(isValidId(id)) { "Invalid NPC id: $id. Only letters, numbers, _ and - are allowed." }
        if (exists(id)) {
            throw IllegalArgumentException("NPC with id $id already exists!")
        }

        val definition = NpcDefinition(
            id = id.lowercase(Locale.ROOT),
            transform = NpcTransform.fromBukkit(location)
        )

        val legacyEvent = org.axostudio.axonpcs.api.event.AxoNPCCreateEvent(definition)
        Bukkit.getPluginManager().callEvent(legacyEvent)
        if (legacyEvent.isCancelled) {
            throw IllegalStateException("NPC creation was cancelled by an event listener.")
        }

        val event = AxoNPCCreateEvent(definition)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) {
            throw IllegalStateException("NPC creation was cancelled by an event listener.")
        }

        npcRegistry.register(definition)
        spatialGrid.index(definition)
        npcRepository.save(definition)

        visibilityController.tickAll()
        return definition
    }

    override fun copyNPC(sourceId: String, newId: String, location: Location?): AxoNPC {
        require(isValidId(newId)) { "Invalid NPC id: $newId. Only letters, numbers, _ and - are allowed." }
        if (exists(newId)) {
            throw IllegalArgumentException("NPC with id $newId already exists!")
        }
        val source = npcRegistry.get(sourceId)
            ?: throw IllegalArgumentException("Source NPC with id $sourceId not found!")

        val targetTransform = if (location != null) {
            NpcTransform.fromBukkit(location)
        } else {
            source.transform
        }

        val definition = source.copy(newId, targetTransform)

        val legacyEvent = org.axostudio.axonpcs.api.event.AxoNPCCreateEvent(definition)
        Bukkit.getPluginManager().callEvent(legacyEvent)
        if (legacyEvent.isCancelled) {
            throw IllegalStateException("NPC creation was cancelled by an event listener.")
        }

        val event = AxoNPCCreateEvent(definition)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) {
            throw IllegalStateException("NPC creation was cancelled by an event listener.")
        }

        npcRegistry.register(definition)
        spatialGrid.index(definition)
        npcRepository.save(definition)

        visibilityController.tickAll()
        return definition
    }

    override fun copyNPC(sourceId: String, newId: String): AxoNPC {
        return copyNPC(sourceId, newId, null)
    }

    override fun deleteNPC(id: String): Boolean {
        val npc = npcRegistry.get(id) ?: return false

        val legacyEvent = org.axostudio.axonpcs.api.event.AxoNPCDeleteEvent(npc)
        Bukkit.getPluginManager().callEvent(legacyEvent)
        if (legacyEvent.isCancelled) return false

        val event = AxoNPCDeleteEvent(npc)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return false

        visibilityController.hideNpc(npc)

        spatialGrid.unindex(npc)
        npcRegistry.unregister(id)
        npcRepository.delete(npc)
        return true
    }

    override fun reload(): Int {
        // Existing sessions reference the old runtime entity IDs. Despawn them before
        // replacing the registry so clients cannot retain ghost NPCs after a reload.
        visibilityController.hideAllSessions()
        val loaded = npcRepository.loadAll()
        spatialGrid.clear()
        npcRegistry.clear()

        for (npc in loaded) {
            npcRegistry.register(npc)
            spatialGrid.index(npc)
            // Resolve skins if needed
            if (npc.skin.value().isBlank() && npc.skin.source().isNotBlank()) {
                skinService.resolve(npc.skin.source()).thenAccept { resolved ->
                    scheduler.runGlobal {
                        npc.skin = resolved
                        save(npc)
                        visibilityController.refreshNpc(npc)
                    }
                }
            }
        }

        visibilityController.tickAll()
        return loaded.size
    }

    override fun getViewerCount(npc: AxoNPC): Int {
        val state = npcRegistry.getState(npc.id)
        return state?.viewerCount() ?: 0
    }

    override fun isVisibleTo(player: Player, npc: AxoNPC): Boolean {
        val tracker = visibilityController.getTracker(player)
        return tracker.isVisible(npc.id)
    }

    fun save(npc: NpcDefinition) {
        npcRepository.save(npc)
    }

    fun moveToGroup(npc: NpcDefinition, group: String?) {
        npcRepository.moveToGroup(npc, group)
    }

    fun setLocation(npc: NpcDefinition, newLocation: Location) {
        val oldTransform = npc.transform
        npc.transform = NpcTransform.fromBukkit(newLocation)
        spatialGrid.updatePosition(npc, oldTransform)
        save(npc)

        visibilityController.refreshNpc(npc)
    }

    fun setRotation(npc: NpcDefinition, yaw: Float, pitch: Float) {
        npc.transform = npc.transform.withRotation(yaw, pitch)
        save(npc)
        visibilityController.refreshNpc(npc)
    }

    private fun isValidId(id: String): Boolean =
        id.matches(Regex("^[a-zA-Z0-9_-]+$"))
}
