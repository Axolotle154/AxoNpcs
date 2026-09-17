package org.axostudio.axonpcs.migration

import org.axostudio.axonpcs.api.model.NPCSkin
import org.axostudio.axonpcs.api.model.NPCSkinMode
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcTransform
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object CitizensImporter {

    fun importFromFile(file: File): List<NpcDefinition> {
        if (!file.exists()) return emptyList()
        val config = YamlConfiguration.loadConfiguration(file)
        val npcSection = config.getConfigurationSection("npc") ?: return emptyList()

        val imported = mutableListOf<NpcDefinition>()
        for (key in npcSection.getKeys(false)) {
            val sec = npcSection.getConfigurationSection(key) ?: continue
            val name = sec.getString("name", "NPC_$key") ?: "NPC_$key"
            val locSec = sec.getConfigurationSection("traits.location")
            val world = locSec?.getString("world", "world") ?: "world"
            val x = locSec?.getDouble("x", 0.0) ?: 0.0
            val y = locSec?.getDouble("y", 64.0) ?: 64.0
            val z = locSec?.getDouble("z", 0.0) ?: 0.0
            val yaw = locSec?.getDouble("yaw", 0.0)?.toFloat() ?: 0.0f
            val pitch = locSec?.getDouble("pitch", 0.0)?.toFloat() ?: 0.0f

            var skin = NPCSkin.none()
            val skinTrait = sec.getConfigurationSection("traits.skintrait")
            if (skinTrait != null) {
                val skinName = skinTrait.getString("skinName")
                val tex = skinTrait.getString("texture")
                val sig = skinTrait.getString("signature")
                if (!tex.isNullOrBlank()) {
                    skin = NPCSkin(NPCSkinMode.TEXTURE, "citizens_$key", tex, sig ?: "")
                } else if (!skinName.isNullOrBlank()) {
                    skin = NPCSkin(NPCSkinMode.NAME, skinName, "", "")
                }
            }

            val id = "citizens_$key"
            val npc = NpcDefinition(
                id = id,
                type = "PLAYER",
                transform = NpcTransform(world, x, y, z, yaw, pitch),
                displayName = name,
                skin = skin
            )
            imported.add(npc)
        }
        return imported
    }
}
