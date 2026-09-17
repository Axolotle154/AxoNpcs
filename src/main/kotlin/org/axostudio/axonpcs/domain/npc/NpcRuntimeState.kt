package org.axostudio.axonpcs.domain.npc

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class NpcRuntimeState(
    val entityId: Int,
    @Volatile var isSleeping: Boolean = false,
    @Volatile var lastActiveMillis: Long = System.currentTimeMillis()
) {
    val viewers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    fun addViewer(uuid: UUID) {
        viewers.add(uuid)
        wake()
    }

    fun removeViewer(uuid: UUID) {
        viewers.remove(uuid)
        if (viewers.isEmpty()) {
            lastActiveMillis = System.currentTimeMillis()
        }
    }

    fun isViewer(uuid: UUID): Boolean = viewers.contains(uuid)

    fun viewerCount(): Int = viewers.size

    fun wake() {
        isSleeping = false
        lastActiveMillis = System.currentTimeMillis()
    }
}
