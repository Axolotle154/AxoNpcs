package org.axostudio.axonpcs.tracking.spatial

import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcTransform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpatialChunkGridTest {

    private fun createNpc(id: String, worldName: String, x: Double, z: Double): NpcDefinition {
        return NpcDefinition(
            id = id,
            transform = NpcTransform(world = worldName, x = x, y = 64.0, z = z)
        )
    }

    @Test
    fun testInsertAndFindInRadius() {
        val grid = SpatialChunkGrid()

        val npc1 = createNpc("npc_1", "world", 100.0, 100.0)
        val npc2 = createNpc("npc_2", "world", 105.0, 105.0)
        val npc3 = createNpc("npc_3", "world", 500.0, 500.0)
        val npc4 = createNpc("npc_4", "nether", 100.0, 100.0)

        grid.index(npc1)
        grid.index(npc2)
        grid.index(npc3)
        grid.index(npc4)

        // Query around (100, 100) in "world" with radius 32
        val nearby = grid.findNearbyNpcIds("world", 100.0, 100.0, 32.0)

        assertEquals(2, nearby.size)
        assertTrue(nearby.contains("npc_1"))
        assertTrue(nearby.contains("npc_2"))
    }

    @Test
    fun testUpdateAndRemove() {
        val grid = SpatialChunkGrid()
        val npc = createNpc("npc_1", "world", 10.0, 10.0)
        grid.index(npc)

        // Move to 200, 200
        val oldTransform = npc.transform
        npc.transform = NpcTransform("world", 200.0, 64.0, 200.0)
        grid.updatePosition(npc, oldTransform)

        val oldQuery = grid.findNearbyNpcIds("world", 10.0, 10.0, 16.0)
        assertTrue(oldQuery.isEmpty())

        val newQuery = grid.findNearbyNpcIds("world", 200.0, 200.0, 16.0)
        assertEquals(1, newQuery.size)
        assertTrue(newQuery.contains("npc_1"))

        // Unindex
        grid.unindex(npc)
        val afterUnindex = grid.findNearbyNpcIds("world", 200.0, 200.0, 16.0)
        assertTrue(afterUnindex.isEmpty())
    }
}
