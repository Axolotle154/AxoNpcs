package org.axostudio.axonpcs.storage.repository

import org.axostudio.axonpcs.domain.group.NpcGroup
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.Locale

class GroupRepository(private val plugin: Plugin) {
    private val rootNpcDir = File(plugin.dataFolder, "npcs")

    fun listGroups(): List<NpcGroup> {
        if (!rootNpcDir.exists()) return emptyList()
        val dirs = rootNpcDir.listFiles { f -> f.isDirectory } ?: return emptyList()
        return dirs.map { NpcGroup(it.name) }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    fun createGroup(name: String): Boolean {
        val dir = File(rootNpcDir, name.trim())
        return if (!dir.exists()) dir.mkdirs() else true
    }

    fun deleteGroup(name: String): Boolean {
        val dir = File(rootNpcDir, name.trim())
        if (!dir.exists()) return false

        // Move any NPCs inside back to root before deleting the directory
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".yml") }
        if (files != null) {
            for (file in files) {
                val target = File(rootNpcDir, file.name)
                file.renameTo(target)
            }
        }
        return dir.delete()
    }
}
