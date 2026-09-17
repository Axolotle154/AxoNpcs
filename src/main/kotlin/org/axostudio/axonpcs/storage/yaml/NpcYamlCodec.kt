package org.axostudio.axonpcs.storage.yaml

import org.axostudio.axonpcs.api.model.NPCActionTrigger
import org.axostudio.axonpcs.api.model.NPCEquipmentSlot
import org.axostudio.axonpcs.api.model.NPCSkin
import org.axostudio.axonpcs.api.model.NPCSkinMode
import org.axostudio.axonpcs.domain.action.ActionDefinition
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcTransform
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.Locale

object NpcYamlCodec {

    fun decode(file: File, id: String, group: String? = null): NpcDefinition? {
        if (!file.exists()) return null
        val config = YamlConfiguration.loadConfiguration(file)

        val enabled = config.getBoolean("enabled", true)
        val npcId = config.getString("id", id) ?: id
        val type = config.getString("type", "PLAYER") ?: "PLAYER"
        val world = config.getString("world", "world") ?: "world"
        val x = config.getDouble("position.x", 0.0)
        val y = config.getDouble("position.y", 64.0)
        val z = config.getDouble("position.z", 0.0)
        val yaw = config.getDouble("position.yaw", 0.0).toFloat()
        val pitch = config.getDouble("position.pitch", 0.0).toFloat()

        val displayName = config.getString("display-name")
        val skinModeStr = config.getString("skin.mode", "NONE")
        val skinMode = try {
            NPCSkinMode.valueOf(skinModeStr?.uppercase(Locale.ROOT) ?: "NONE")
        } catch (e: Exception) {
            NPCSkinMode.NONE
        }
        val skinSource = config.getString("skin.source", "") ?: ""
        val skinValue = config.getString("skin.value", "") ?: ""
        val skinSignature = config.getString("skin.signature", "") ?: ""
        val skin = NPCSkin(skinMode, skinSource, skinValue, skinSignature)

        val glowing = config.getString("glowing", "off") ?: "off"
        val collidable = config.getBoolean("collidable", false)
        val soundEnabled = config.getBoolean("sound-enabled", true)
        val maxHealth = config.getDouble("max-health", 20.0)
        val scale = config.getDouble("scale", 1.0)
        val viewDistance = config.getDouble("view-distance", 48.0)
        val cooldown = config.getDouble("interaction-cooldown", 1.5)

        val behavior = config.getString("behavior", "stationary") ?: "stationary"
        val animation = config.getString("animation", "none") ?: "none"
        val animInterval = config.getLong("animation-interval-ticks", 40L)
        val movementRange = config.getDouble("movement-range", 2.0)
        val turnToPlayer = config.getBoolean("turn-to-player", true)
        val turnDistance = config.getDouble("turn-to-player-distance", 16.0)

        val npc = NpcDefinition(
            id = npcId,
            type = type,
            transform = NpcTransform(world, x, y, z, yaw, pitch),
            displayName = displayName,
            skin = skin,
            glowing = glowing,
            isCollidable = collidable,
            isSoundEnabled = soundEnabled,
            maxHealth = maxHealth,
            scale = scale,
            viewDistance = viewDistance,
            interactionCooldownSeconds = cooldown,
            isTurnToPlayer = turnToPlayer,
            turnToPlayerDistance = turnDistance,
            behaviorType = behavior,
            behaviorRange = movementRange,
            animationType = animation,
            animationIntervalTicks = animInterval,
            group = group,
            isEnabled = enabled
        )

        // Appearance
        config.getConfigurationSection("appearance")?.let { sec ->
            for (key in sec.getKeys(false)) {
                sec.getString(key)?.let { value -> npc.appearance[key] = value }
            }
        }

        // Equipment
        config.getConfigurationSection("equipment")?.let { sec ->
            for (slotName in sec.getKeys(false)) {
                val slot = NPCEquipmentSlot.parse(slotName) ?: continue
                val item: ItemStack? = sec.getItemStack(slotName)
                if (item != null) {
                    npc.equipment[slot] = item
                }
            }
        }

        // Actions
        config.getConfigurationSection("actions")?.let { actionsSec ->
            for (triggerName in actionsSec.getKeys(false)) {
                val trigger = NPCActionTrigger.parse(triggerName)
                val list = actionsSec.getMapList(triggerName)
                for (map in list) {
                    val actionType = map["type"]?.toString() ?: "message"
                    val actionValue = map["value"]?.toString() ?: ""
                    npc.actions.add(ActionDefinition(trigger, actionType, actionValue))
                }
            }
        }

        return npc
    }

    fun encode(npc: NpcDefinition, file: File) {
        AtomicFileWriter.writeAtomically(file) { tmpFile ->
            val config = YamlConfiguration()
            config.set("enabled", npc.isEnabled)
            config.set("id", npc.id)
            config.set("type", npc.type)
            config.set("world", npc.transform.world)
            config.set("position.x", npc.transform.x)
            config.set("position.y", npc.transform.y)
            config.set("position.z", npc.transform.z)
            config.set("position.yaw", npc.transform.yaw)
            config.set("position.pitch", npc.transform.pitch)

            if (npc.displayName != null) {
                config.set("display-name", npc.displayName)
            }
            config.set("skin.mode", npc.skin.mode().name)
            config.set("skin.source", npc.skin.source())
            config.set("skin.value", npc.skin.value())
            config.set("skin.signature", npc.skin.signature())

            config.set("glowing", npc.glowing)
            config.set("collidable", npc.isCollidable)
            config.set("sound-enabled", npc.isSoundEnabled)
            config.set("max-health", npc.maxHealth)
            config.set("scale", npc.scale)
            config.set("view-distance", npc.viewDistance)
            config.set("interaction-cooldown", npc.interactionCooldownSeconds)

            config.set("behavior", npc.behaviorType)
            config.set("animation", npc.animationType)
            config.set("animation-interval-ticks", npc.animationIntervalTicks)
            config.set("movement-range", npc.behaviorRange)
            config.set("turn-to-player", npc.isTurnToPlayer)
            config.set("turn-to-player-distance", npc.turnToPlayerDistance)

            // Appearance
            if (npc.appearance.isNotEmpty()) {
                npc.appearance.forEach { (k, v) -> config.set("appearance.$k", v) }
            } else {
                config.createSection("appearance")
            }

            // Equipment
            if (npc.equipment.isNotEmpty()) {
                npc.equipment.forEach { (slot, item) -> config.set("equipment.${slot.name}", item) }
            } else {
                config.createSection("equipment")
            }

            // Actions
            if (npc.actions.isNotEmpty()) {
                val grouped = npc.actions.groupBy { it.trigger }
                grouped.forEach { (trigger, actionList) ->
                    val mapList = actionList.map { mapOf("type" to it.type, "value" to it.value) }
                    config.set("actions.${trigger.name}", mapList)
                }
            } else {
                config.createSection("actions")
            }

            config.save(tmpFile)
        }
    }
}
