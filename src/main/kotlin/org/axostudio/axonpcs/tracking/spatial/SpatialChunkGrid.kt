package org.axostudio.axonpcs.tracking.spatial

import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcTransform
import org.bukkit.Location
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class SpatialChunkGrid {
    private val chunkIndex = ConcurrentHashMap<ChunkCoord, MutableSet<String>>()

    fun index(npc: NpcDefinition) {
        val coord = ChunkCoord.fromTransform(npc.transform)
        chunkIndex.computeIfAbsent(coord) { ConcurrentHashMap.newKeySet() }.add(npc.id.lowercase(Locale.ROOT))
    }

    fun unindex(npc: NpcDefinition) {
        val coord = ChunkCoord.fromTransform(npc.transform)
        val set = chunkIndex[coord] ?: return
        set.remove(npc.id.lowercase(Locale.ROOT))
        if (set.isEmpty()) {
            chunkIndex.remove(coord)
        }
    }

    fun updatePosition(npc: NpcDefinition, oldTransform: NpcTransform) {
        val oldCoord = ChunkCoord.fromTransform(oldTransform)
        val newCoord = ChunkCoord.fromTransform(npc.transform)
        if (oldCoord != newCoord) {
            val oldSet = chunkIndex[oldCoord]
            oldSet?.remove(npc.id.lowercase(Locale.ROOT))
            if (oldSet != null && oldSet.isEmpty()) {
                chunkIndex.remove(oldCoord)
            }
            index(npc)
        }
    }

    fun findNearbyNpcIds(loc: Location, radiusBlocks: Double): Set<String> {
        val world = loc.world?.name ?: return emptySet()
        return findNearbyNpcIds(world, loc.x, loc.z, radiusBlocks)
    }

    fun findNearbyNpcIds(worldName: String, x: Double, z: Double, radiusBlocks: Double): Set<String> {
        val centerChunkX = Math.floor(x).toInt() shr 4
        val centerChunkZ = Math.floor(z).toInt() shr 4
        val chunkRadius = maxOf(1, Math.ceil(radiusBlocks / 16.0).toInt() + 1)

        val result = mutableSetOf<String>()
        for (cx in (centerChunkX - chunkRadius)..(centerChunkX + chunkRadius)) {
            for (cz in (centerChunkZ - chunkRadius)..(centerChunkZ + chunkRadius)) {
                val coord = ChunkCoord(worldName.lowercase(Locale.ROOT), cx, cz)
                val ids = chunkIndex[coord]
                if (ids != null) {
                    result.addAll(ids)
                }
            }
        }
        return result
    }

    fun clear() {
        chunkIndex.clear()
    }

    private data class ChunkCoord(val world: String, val chunkX: Int, val chunkZ: Int) {
        companion object {
            fun fromTransform(transform: NpcTransform): ChunkCoord =
                ChunkCoord(
                    world = transform.world.lowercase(Locale.ROOT),
                    chunkX = Math.floor(transform.x).toInt() shr 4,
                    chunkZ = Math.floor(transform.z).toInt() shr 4
                )
        }
    }
}
