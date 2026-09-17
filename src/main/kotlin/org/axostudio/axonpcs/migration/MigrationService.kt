package org.axostudio.axonpcs.migration

import org.axostudio.axonpcs.render.skin.SkinService
import org.axostudio.axonpcs.platform.scheduler.SchedulerAdapter
import org.axostudio.axonpcs.runtime.registry.NpcRegistry
import org.axostudio.axonpcs.storage.repository.NpcRepository
import org.axostudio.axonpcs.tracking.spatial.SpatialChunkGrid
import org.axostudio.axonpcs.tracking.viewer.VisibilityController
import java.io.File
import java.util.Locale

class MigrationService(
    private val dataFolder: File,
    private val npcRepository: NpcRepository,
    private val npcRegistry: NpcRegistry,
    private val spatialGrid: SpatialChunkGrid,
    private val visibilityController: VisibilityController,
    private val skinService: SkinService,
    private val scheduler: SchedulerAdapter
) {

    fun migrate(source: String): MigrationResult {
        val pluginsFolder = dataFolder.parentFile
        val npcs = when (source.lowercase(Locale.ROOT)) {
            "citizens" -> {
                val file = File(pluginsFolder, "Citizens/saves.yml")
                CitizensImporter.importFromFile(file)
            }
            "fancynpcs" -> {
                val file1 = File(pluginsFolder, "FancyNpcs/default_data.json")
                val file2 = File(pluginsFolder, "FancyNpcs/npcs.json")
                if (file1.exists()) FancyNpcsImporter.importFromFile(file1)
                else FancyNpcsImporter.importFromFile(file2)
            }
            "znpcs" -> {
                val file1 = File(pluginsFolder, "ServersNPC/npcs.json")
                val file2 = File(pluginsFolder, "ZNPCsPlus/data/npcs.json")
                if (file1.exists()) ZnpcsImporter.importFromFile(file1)
                else ZnpcsImporter.importFromFile(file2)
            }
            else -> emptyList()
        }

        var success = 0
        var failed = 0
        val errors = mutableListOf<String>()

        for (npc in npcs) {
            try {
                npcRepository.save(npc)
                npcRegistry.register(npc)
                spatialGrid.index(npc)
                if (npc.skin.value().isBlank() && npc.skin.source().isNotBlank()) {
                    skinService.resolve(npc.skin.source()).thenAccept { resolved ->
                        scheduler.runGlobal {
                            npc.skin = resolved
                            npcRepository.save(npc)
                            visibilityController.refreshNpc(npc)
                        }
                    }
                }
                success++
            } catch (e: Exception) {
                failed++
                errors.add("${npc.id}: ${e.message}")
            }
        }

        visibilityController.tickAll()
        return MigrationResult(source, success, failed, errors)
    }
}
