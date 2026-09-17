package org.axostudio.axonpcs.migration

import com.google.gson.JsonParser
import org.axostudio.axonpcs.api.model.NPCSkin
import org.axostudio.axonpcs.api.model.NPCSkinMode
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcTransform
import java.io.File
import java.io.FileReader

object ZnpcsImporter {

    fun importFromFile(file: File): List<NpcDefinition> {
        if (!file.exists()) return emptyList()
        val imported = mutableListOf<NpcDefinition>()

        try {
            FileReader(file).use { reader ->
                val element = JsonParser.parseReader(reader)
                val list = if (element.isJsonArray) {
                    element.asJsonArray.toList()
                } else if (element.isJsonObject) {
                    element.asJsonObject.entrySet().map { it.value }
                } else {
                    return emptyList()
                }

                for ((index, item) in list.withIndex()) {
                    if (!item.isJsonObject) continue
                    val obj = item.asJsonObject
                    val idNum = if (obj.has("id")) obj.get("id").asString else index.toString()
                    val id = "znpc_$idNum"
                    val npcType = if (obj.has("npcType")) obj.get("npcType").asString else "PLAYER"

                    var world = "world"
                    var x = 0.0
                    var y = 64.0
                    var z = 0.0
                    var yaw = 0.0f
                    var pitch = 0.0f

                    if (obj.has("location") && obj.get("location").isJsonObject) {
                        val loc = obj.getAsJsonObject("location")
                        world = if (loc.has("world")) loc.get("world").asString else "world"
                        x = if (loc.has("x")) loc.get("x").asDouble else 0.0
                        y = if (loc.has("y")) loc.get("y").asDouble else 64.0
                        z = if (loc.has("z")) loc.get("z").asDouble else 0.0
                        yaw = if (loc.has("yaw")) loc.get("yaw").asFloat else 0.0f
                        pitch = if (loc.has("pitch")) loc.get("pitch").asFloat else 0.0f
                    }

                    var displayName: String? = null
                    if (obj.has("hologramLines") && obj.get("hologramLines").isJsonArray) {
                        val lines = obj.getAsJsonArray("hologramLines")
                        if (lines.size() > 0) {
                            displayName = lines[0].asString
                        }
                    }

                    var skin = NPCSkin.none()
                    if (obj.has("skin") && obj.get("skin").isJsonObject) {
                        val skinObj = obj.getAsJsonObject("skin")
                        val tex = if (skinObj.has("value")) skinObj.get("value").asString else ""
                        val sig = if (skinObj.has("signature")) skinObj.get("signature").asString else ""
                        if (tex.isNotBlank()) {
                            skin = NPCSkin(NPCSkinMode.TEXTURE, id, tex, sig)
                        }
                    } else if (obj.has("skin") && obj.get("skin").isJsonPrimitive) {
                        val skinName = obj.get("skin").asString
                        if (skinName.isNotBlank()) {
                            skin = NPCSkin(NPCSkinMode.NAME, skinName, "", "")
                        }
                    }

                    val npc = NpcDefinition(
                        id = id,
                        type = npcType,
                        transform = NpcTransform(world, x, y, z, yaw, pitch),
                        displayName = displayName,
                        skin = skin
                    )
                    imported.add(npc)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return imported
    }
}
