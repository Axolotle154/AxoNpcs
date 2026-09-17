package org.axostudio.axonpcs.runtime.registry

import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcRuntimeState
import java.lang.reflect.Method
import java.util.Locale
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class NpcRegistry {
    private val npcsById = ConcurrentHashMap<String, NpcDefinition>()
    private val stateById = ConcurrentHashMap<String, NpcRuntimeState>()
    private val npcByEntityId = ConcurrentHashMap<Int, NpcDefinition>()

    private val fallbackIdCounter = AtomicInteger(1_000_000_000)
    private val nextEntityIdMethod: Method? = findNextEntityIdMethod()

    fun nextEntityId(): Int {
        if (nextEntityIdMethod != null) {
            try {
                return nextEntityIdMethod.invoke(null) as Int
            } catch (ignored: Throwable) {
            }
        }
        return fallbackIdCounter.getAndIncrement()
    }

    fun register(npc: NpcDefinition, existingEntityId: Int? = null): NpcRuntimeState {
        val normalizedId = normalizeId(npc.id)
        npcsById[normalizedId] = npc

        val entityId = existingEntityId ?: nextEntityId()
        val state = NpcRuntimeState(entityId)
        stateById[normalizedId] = state
        npcByEntityId[entityId] = npc
        return state
    }

    fun unregister(id: String): NpcDefinition? {
        val normalizedId = normalizeId(id)
        val npc = npcsById.remove(normalizedId)
        val state = stateById.remove(normalizedId)
        if (state != null) {
            npcByEntityId.remove(state.entityId)
        }
        return npc
    }

    fun get(id: String): NpcDefinition? = npcsById[normalizeId(id)]

    fun getState(id: String): NpcRuntimeState? = stateById[normalizeId(id)]

    fun getByEntityId(entityId: Int): NpcDefinition? = npcByEntityId[entityId]

    fun getAll(): Collection<NpcDefinition> = npcsById.values

    fun clear() {
        npcsById.clear()
        stateById.clear()
        npcByEntityId.clear()
    }

    private fun normalizeId(id: String): String = id.trim().lowercase(Locale.ROOT)

    private fun findNextEntityIdMethod(): Method? {
        return try {
            val entityClass = Class.forName("net.minecraft.world.entity.Entity")
            val method = entityClass.getMethod("nextEntityId")
            method.isAccessible = true
            method
        } catch (ignored: Throwable) {
            null
        }
    }
}
