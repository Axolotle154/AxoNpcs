package org.axostudio.axonpcs.migration

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.axostudio.axonpcs.api.model.NPCSkin
import org.axostudio.axonpcs.api.model.NPCSkinMode
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcTransform
import java.io.File
import java.io.FileReader

object FancyNpcsImporter {

    fun importFromFile(file: File): List<NpcDefinition> {
        if (!file.exists()) return emptyList()
        val imported = mutableListOf<NpcDefinition>()

        try {
            FileReader(file).use { reader ->
                val element = JsonParser.parseReader(reader)
                val array = if (element.isJsonArray) {
                    element.asJsonArray
                } else if (element.isJsonObject && element.asJsonObject.has("npcs")) {
                    element.asJsonObject.getAsJsonArray("npcs")
                } else {
                    return emptyList()
                }

                for (item in array) {
                    if (!item.isJsonObject) continue
                    val obj = item.asJsonObject
                    val name = if (obj.has("name")) obj.get("name").asString else "npc_${imported.size + 1}"
                    val displayName = if (obj.has("displayName")) obj.get("displayName").asString else name
                    val type = if (obj.has("type")) obj.get("type").asString else "PLAYER"

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

                    var skin = NPCSkin.none()
                    if (obj.has("skin") && obj.get("skin").isJsonObject) {
                        val skinObj = obj.getAsJsonObject("skin")
                        val valStr = if (skinObj.has("value")) skinObj.get("value").asString else ""
                        val sigStr = if (skinObj.has("signature")) skinObj.get("signature").asString else ""
                        if (valStr.isNotBlank()) {
                            skin = NPCSkin(NPCSkinMode.TEXTURE, "fn_$name", valStr, sigStr)
                        }
                    }

                    val npc = NpcDefinition(
                        id = "fn_$name",
                        type = type,
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
