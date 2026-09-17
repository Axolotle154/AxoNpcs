package org.axostudio.axonpcs.storage.repository

import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcTransform
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.lang.reflect.Proxy

class NpcRepositoryTest {

    @Test
    fun `moving an NPC to a group removes the previous file`(@TempDir dataFolder: File) {
        val plugin = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(Plugin::class.java)
        ) { _, method, _ ->
            if (method.name == "getDataFolder") dataFolder else null
        } as Plugin
        val repository = NpcRepository(plugin)
        val npc = NpcDefinition(
            id = "guide",
            transform = NpcTransform("world", 0.0, 64.0, 0.0)
        )

        repository.save(npc)
        repository.moveToGroup(npc, "lobby")

        assertFalse(File(dataFolder, "npcs/guide.yml").exists())
        assertTrue(File(dataFolder, "npcs/lobby/guide.yml").exists())
        assertTrue(repository.loadAll().single().group == "lobby")
    }
}
