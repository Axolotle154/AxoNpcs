package org.axostudio.axonpcs.storage.repository

import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.storage.yaml.NpcYamlCodec
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.Locale

class NpcRepository(private val plugin: Plugin) {
    private val rootNpcDir = File(plugin.dataFolder, "npcs")

    init {
        if (!rootNpcDir.exists()) {
            rootNpcDir.mkdirs()
        }
    }

    fun loadAll(): List<NpcDefinition> {
        val list = mutableListOf<NpcDefinition>()
        if (!rootNpcDir.exists()) return list

        loadFromDir(rootNpcDir, null, list)
        return list
    }

    private fun loadFromDir(dir: File, group: String?, result: MutableList<NpcDefinition>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                // Subdirectory represents a group!
                loadFromDir(file, file.name, result)
            } else if (file.name.endsWith(".yml") || file.name.endsWith(".yaml")) {
                val id = file.nameWithoutExtension.lowercase(Locale.ROOT)
                val npc = NpcYamlCodec.decode(file, id, group)
                if (npc != null) {
                    result.add(npc)
                }
            }
        }
    }

    fun save(npc: NpcDefinition) {
        val group = npc.group?.takeIf { it.isNotBlank() }
        require(group == null || isValidGroupName(group)) { "Invalid group name: $group" }
        val targetDir = if (group != null) {
            File(rootNpcDir, group)
        } else {
            rootNpcDir
        }
        if (!targetDir.exists()) targetDir.mkdirs()

        val file = File(targetDir, "${npc.id}.yml")
        NpcYamlCodec.encode(npc, file)
    }

    fun delete(npc: NpcDefinition): Boolean {
        val files = findFiles(npc.id)
        return files.isNotEmpty() && files.all { it.delete() }
    }

    fun findFile(id: String): File? {
        return findFiles(id).firstOrNull()
    }

    private fun findFiles(id: String): List<File> {
        val normalizedId = "${id.lowercase(Locale.ROOT)}.yml"
        val result = mutableListOf<File>()
        val rootFile = File(rootNpcDir, normalizedId)
        if (rootFile.exists()) result.add(rootFile)

        val subdirs = rootNpcDir.listFiles { f -> f.isDirectory } ?: return result
        for (dir in subdirs) {
            val file = File(dir, normalizedId)
            if (file.exists()) result.add(file)
        }
        return result
    }

    fun moveToGroup(npc: NpcDefinition, newGroup: String?) {
        val normalizedGroup = newGroup?.takeIf { it.isNotBlank() }
        require(normalizedGroup == null || isValidGroupName(normalizedGroup)) { "Invalid group name: $newGroup" }
        val oldFiles = findFiles(npc.id)
        npc.group = normalizedGroup
        save(npc)
        val targetDir = normalizedGroup?.let { File(rootNpcDir, it) } ?: rootNpcDir
        val targetFile = File(targetDir, "${npc.id}.yml").absoluteFile
        oldFiles.filter { it.absoluteFile != targetFile }.forEach { it.delete() }
    }

    private fun isValidGroupName(name: String): Boolean {
        return name.matches(Regex("^[a-zA-Z0-9_-]+$"))
    }
}
