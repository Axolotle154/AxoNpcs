package org.axostudio.axonpcs.command.npc

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.axostudio.axonpcs.api.model.NPCActionTrigger
import org.axostudio.axonpcs.api.model.NPCEquipmentSlot
import org.axostudio.axonpcs.config.ConfigManager
import org.axostudio.axonpcs.domain.action.ActionDefinition
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcTransform
import org.axostudio.axonpcs.render.equipment.EquipmentService
import org.axostudio.axonpcs.render.skin.SkinService
import org.axostudio.axonpcs.runtime.service.GroupService
import org.axostudio.axonpcs.runtime.service.NpcService
import org.axostudio.axonpcs.platform.scheduler.SchedulerAdapter
import org.axostudio.axonpcs.tracking.viewer.VisibilityController
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.trim.ArmorTrim
import java.util.Locale

class NpcCommandExecutor(
    private val configManager: ConfigManager,
    private val npcService: NpcService,
    private val skinService: SkinService,
    private val equipmentService: EquipmentService,
    private val groupService: GroupService,
    private val visibilityController: VisibilityController,
    private val scheduler: SchedulerAdapter
) : BasicCommand {

    override fun execute(commandSource: CommandSourceStack, args: Array<out String>) {
        val sender = commandSource.sender
        if (args.isEmpty() || args[0].equals("help", ignoreCase = true)) {
            sendHelp(sender)
            return
        }

        val sub = args[0].lowercase(Locale.ROOT)
        if (!hasSubcommandPermission(sender, sub)) {
            configManager.send(sender, "no-permission")
            return
        }
        when (sub) {
            "create" -> handleCreate(sender, args)
            "copy", "clone" -> handleCopy(sender, args)
            "remove", "delete" -> handleDelete(sender, args)
            "list" -> handleList(sender, args)
            "info" -> handleInfo(sender, args)
            "nearby" -> handleNearby(sender, args)
            "type" -> handleType(sender, args)
            "displayname" -> handleDisplayName(sender, args)
            "skin" -> handleSkin(sender, args)
            "equip", "equipment" -> handleEquip(sender, args)
            "glowing" -> handleGlowing(sender, args)
            "collidable" -> handleCollidable(sender, args)
            "scale" -> handleScale(sender, args)
            "sound" -> handleSound(sender, args)
            "health" -> handleHealth(sender, args)
            "viewdistance" -> handleViewDistance(sender, args)
            "behavior" -> handleBehavior(sender, args)
            "turn_to_player" -> handleTurnToPlayer(sender, args)
            "turn_to_player_distance" -> handleTurnToPlayerDistance(sender, args)
            "movehere" -> handleMoveHere(sender, args)
            "moveto" -> handleMoveTo(sender, args)
            "center" -> handleCenter(sender, args)
            "rotate" -> handleRotate(sender, args)
            "teleport", "tp" -> handleTeleport(sender, args)
            "action" -> handleAction(sender, args)
            "interactioncooldown" -> handleCooldown(sender, args)
            "group" -> handleGroup(sender, args)
            "trim" -> handleTrim(sender, args)
            else -> configManager.send(sender, "unknown-command")
        }
    }

    private fun handleCreate(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            configManager.send(sender, "invalid-usage", mapOf("usage" to "/npc create <id>"))
            return
        }
        val id = args[1]
        val loc = if (sender is Player) sender.location else Location(Bukkit.getWorlds()[0], 0.0, 64.0, 0.0)

        try {
            val npc = npcService.createNPC(id, loc)
            configManager.send(sender, "npc-create-success", mapOf("id" to npc.id))
        } catch (e: Exception) {
            sender.sendMessage(configManager.parse("<red>${e.message}</red>"))
        }
    }

    private fun handleCopy(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            configManager.send(sender, "invalid-usage", mapOf("usage" to "/npc copy <source> <new_id> [here|same]"))
            return
        }
        val sourceId = args[1]
        val newId = args[2]

        val sourceNpc = npcService.getNPC(sourceId).orElse(null) as? NpcDefinition
        if (sourceNpc == null) {
            configManager.send(sender, "npc-not-found", mapOf("id" to sourceId))
            return
        }

        if (npcService.exists(newId)) {
            configManager.send(sender, "npc-exists", mapOf("id" to newId))
            return
        }

        val location = if (args.size > 3 && args[3].equals("same", ignoreCase = true)) {
            sourceNpc.location
        } else if (sender is Player) {
            sender.location
        } else {
            sourceNpc.location
        }

        try {
            val copy = npcService.copyNPC(sourceId, newId, location)
            configManager.send(sender, "npc-copy-success", mapOf("source" to sourceId, "id" to copy.id))
        } catch (e: Exception) {
            sender.sendMessage(configManager.parse("<red>${e.message}</red>"))
        }
    }

    private fun handleDelete(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            configManager.send(sender, "invalid-usage", mapOf("usage" to "/npc remove <id>"))
            return
        }
        val id = args[1]
        if (npcService.deleteNPC(id)) {
            configManager.send(sender, "npc-delete-success", mapOf("id" to id))
        } else {
            configManager.send(sender, "npc-not-found", mapOf("id" to id))
        }
    }

    private fun handleList(sender: CommandSender, args: Array<out String>) {
        val npcs = npcService.allNPCs
        if (npcs.isEmpty()) {
            configManager.send(sender, "npc-list-empty")
            return
        }
        val ids = npcs.joinToString(", ") { it.id }
        configManager.send(sender, "npc-list-header", mapOf("count" to npcs.size.toString(), "npcs" to ids))
    }

    private fun handleInfo(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            configManager.send(sender, "invalid-usage", mapOf("usage" to "/npc info <id>"))
            return
        }
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition
        if (npc == null) {
            configManager.send(sender, "npc-not-found", mapOf("id" to args[1]))
            return
        }
        val viewers = npcService.getViewerCount(npc)
        configManager.send(sender, "npc-info-header", mapOf(
            "id" to npc.id,
            "type" to npc.type,
            "world" to npc.transform.world,
            "x" to "%.1f".format(npc.transform.x),
            "y" to "%.1f".format(npc.transform.y),
            "z" to "%.1f".format(npc.transform.z),
            "viewers" to viewers.toString()
        ))
    }

    private fun handleNearby(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) return
        val radius = if (args.size > 1) args[1].toDoubleOrNull() ?: 16.0 else 16.0
        val nearby = npcService.allNPCs.filter { (it as NpcDefinition).transform.distanceSquared(sender.location) <= radius * radius }
        val ids = nearby.joinToString(", ") { it.id }
        sender.sendMessage(configManager.parse("<gold>NPCs nearby (${nearby.size}): <white>$ids</white></gold>"))
    }

    private fun handleType(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            configManager.send(sender, "invalid-usage", mapOf("usage" to "/npc type <id> <type>"))
            return
        }
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        npc.type = args[2].uppercase(Locale.ROOT)
        npcService.save(npc)
        visibilityController.refreshNpc(npc)
        configManager.send(sender, "npc-type-updated", mapOf("id" to npc.id, "type" to npc.type))
    }

    private fun handleDisplayName(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        if (args.size == 2) {
            sender.sendMessage(configManager.parse("<gold>Display name: ${npc.displayName ?: "none"}</gold>"))
            return
        }
        val name = args.drop(2).joinToString(" ")
        npc.displayName = if (name.equals("none", ignoreCase = true) || name.equals("@none", ignoreCase = true)) null else name
        npcService.save(npc)
        visibilityController.refreshNpc(npc)
        configManager.send(sender, "npc-displayname-updated", mapOf("id" to npc.id, "name" to (npc.displayName ?: "none")))
    }

    private fun handleSkin(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        val skinInput = args[2]

        configManager.send(sender, "npc-skin-loading", mapOf("skin" to skinInput))

        skinService.resolve(skinInput).whenComplete { resolved, error ->
            scheduler.runGlobal {
                if (error != null || resolved == null) {
                    val cause = generateSequence(error) { it.cause }.lastOrNull()
                    val message = cause?.message ?: "Could not resolve the requested skin."
                    val feedback = {
                        configManager.send(sender, "npc-skin-failed", mapOf("skin" to skinInput))
                        sender.sendMessage(configManager.parse("<red>$message</red>"))
                    }
                    if (sender is Player) scheduler.runEntity(sender, feedback) else feedback()
                    return@runGlobal
                }
                npc.skin = resolved
                npcService.save(npc)
                visibilityController.refreshNpc(npc)
                val feedback = {
                    configManager.send(sender, "npc-skin-updated", mapOf("id" to npc.id, "skin" to skinInput))
                }
                if (sender is Player) scheduler.runEntity(sender, feedback) else feedback()
            }
        }
    }

    private fun handleEquip(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return

        if (args[2].equals("auto", ignoreCase = true) && sender is Player) {
            equipmentService.autoEquip(npc, sender)
            npcService.save(npc)
            visibilityController.refreshNpc(npc)
            sender.sendMessage(configManager.parse("<green>Equipment copied from player to ${npc.id}!</green>"))
            return
        }

        val slot = NPCEquipmentSlot.parse(args[2]) ?: return
        if (args.size < 4 || args[3].equals("clear", ignoreCase = true)) {
            equipmentService.equip(npc, slot, null)
        } else if (args[3].equals("hand", ignoreCase = true) && sender is Player) {
            equipmentService.equip(npc, slot, sender.inventory.itemInMainHand)
        } else {
            val mat = Material.matchMaterial(args[3]) ?: return
            equipmentService.equip(npc, slot, ItemStack(mat))
        }
        npcService.save(npc)
        visibilityController.refreshNpc(npc)
        sender.sendMessage(configManager.parse("<green>Equipped slot ${slot.name} on ${npc.id}!</green>"))
    }

    private fun handleTrim(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage(configManager.parse("<yellow>Uso: /npc trim <id> <helmet|chestplate|leggings|boots> [patron] [material|clear]</yellow>"))
            return
        }
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: run {
            configManager.send(sender, "npc-not-found", mapOf("id" to args[1]))
            return
        }
        val slot = NPCEquipmentSlot.parse(args[2]) ?: run {
            sender.sendMessage(configManager.parse("<red>Slot invalido. Usa helmet, chestplate, leggings o boots.</red>"))
            return
        }
        val item = npc.equipment[slot]
        if (item == null || item.type == Material.AIR) {
            sender.sendMessage(configManager.parse("<red>El NPC no tiene armadura en el slot ${slot.name.lowercase(Locale.ROOT)}.</red>"))
            return
        }
        val meta = item.itemMeta as? ArmorMeta ?: run {
            sender.sendMessage(configManager.parse("<red>El objeto en ${slot.name.lowercase(Locale.ROOT)} no soporta armor trims.</red>"))
            return
        }

        if (args.size == 3 || (args.size >= 4 && args[3].equals("clear", ignoreCase = true))) {
            meta.trim = null
            item.itemMeta = meta
            npcService.save(npc)
            visibilityController.refreshNpc(npc)
            sender.sendMessage(configManager.parse("<green>Armor trim removido de ${npc.id} (${slot.name.lowercase(Locale.ROOT)}).</green>"))
            return
        }

        val patternName = args[3].lowercase(Locale.ROOT)
        val materialName = if (args.size > 4) args[4].lowercase(Locale.ROOT) else "quartz"

        val patternKey = if (patternName.contains(":")) NamespacedKey.fromString(patternName) else NamespacedKey.minecraft(patternName)
        val pattern = patternKey?.let { Registry.TRIM_PATTERN.get(it) }
        if (pattern == null) {
            val valid = Registry.TRIM_PATTERN.map { it.key.key }.sorted().joinToString(", ")
            sender.sendMessage(configManager.parse("<red>Patron desconocido '$patternName'. Disponibles: <white>$valid</white></red>"))
            return
        }

        val matKey = if (materialName.contains(":")) NamespacedKey.fromString(materialName) else NamespacedKey.minecraft(materialName)
        val material = matKey?.let { Registry.TRIM_MATERIAL.get(it) }
        if (material == null) {
            val valid = Registry.TRIM_MATERIAL.map { it.key.key }.sorted().joinToString(", ")
            sender.sendMessage(configManager.parse("<red>Material desconocido '$materialName'. Disponibles: <white>$valid</white></red>"))
            return
        }

        meta.trim = ArmorTrim(material, pattern)
        item.itemMeta = meta
        npcService.save(npc)
        visibilityController.refreshNpc(npc)
        sender.sendMessage(configManager.parse("<green>Armor trim aplicado a ${npc.id} (${slot.name.lowercase(Locale.ROOT)}): <white>${pattern.key.key}</white> con <white>${material.key.key}</white>!</green>"))
    }

    private fun handleGlowing(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        npc.glowing = args[2].lowercase(Locale.ROOT)
        npcService.save(npc)
        visibilityController.refreshNpc(npc)
        configManager.send(sender, "npc-glowing-updated", mapOf("id" to npc.id, "color" to npc.glowing))
    }

    private fun handleCollidable(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        npc.isCollidable = args[2].toBoolean()
        npcService.save(npc)
        visibilityController.refreshNpc(npc)
        sender.sendMessage(configManager.parse("<green>Collidable updated for ${npc.id}: ${npc.isCollidable}</green>"))
    }

    private fun handleScale(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        val scale = args[2].toDoubleOrNull() ?: return
        npc.scale = scale
        npcService.save(npc)
        visibilityController.refreshNpc(npc)
        sender.sendMessage(configManager.parse("<green>Scale updated for ${npc.id}: $scale</green>"))
    }

    private fun handleSound(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        npc.isSoundEnabled = args[2].toBoolean()
        npcService.save(npc)
        visibilityController.refreshNpc(npc)
        sender.sendMessage(configManager.parse("<green>Sound enabled updated for ${npc.id}: ${npc.isSoundEnabled}</green>"))
    }

    private fun handleHealth(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        val hp = args[2].toDoubleOrNull() ?: return
        npc.maxHealth = hp
        npcService.save(npc)
        visibilityController.refreshNpc(npc)
        sender.sendMessage(configManager.parse("<green>Max health updated for ${npc.id}: $hp</green>"))
    }

    private fun handleViewDistance(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        val dist = args[2].toDoubleOrNull() ?: return
        npc.viewDistance = dist
        npcService.save(npc)
        visibilityController.refreshNpc(npc)
        sender.sendMessage(configManager.parse("<green>View distance updated for ${npc.id}: $dist</green>"))
    }

    private fun handleBehavior(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        npc.behaviorType = args[2].lowercase(Locale.ROOT)
        if (args.size >= 4) {
            args[3].toDoubleOrNull()?.let { npc.behaviorRange = it }
        }
        npcService.save(npc)
        sender.sendMessage(configManager.parse("<green>Behavior updated for ${npc.id}: ${npc.behaviorType}</green>"))
    }

    private fun handleTurnToPlayer(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        if (args.size >= 3) {
            npc.isTurnToPlayer = args[2].toBoolean()
            npcService.save(npc)
        }
        sender.sendMessage(configManager.parse("<green>Turn to player for ${npc.id}: ${npc.isTurnToPlayer}</green>"))
    }

    private fun handleTurnToPlayerDistance(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        if (args.size >= 3) {
            args[2].toDoubleOrNull()?.let { npc.turnToPlayerDistance = it }
            npcService.save(npc)
        }
        sender.sendMessage(configManager.parse("<green>Turn to player distance for ${npc.id}: ${npc.turnToPlayerDistance}</green>"))
    }

    private fun handleMoveHere(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player || args.size < 2) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        npcService.setLocation(npc, sender.location)
        sender.sendMessage(configManager.parse("<green>Moved ${npc.id} to your location!</green>"))
    }

    private fun handleMoveTo(sender: CommandSender, args: Array<out String>) {
        if (args.size < 5) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        val x = args[2].toDoubleOrNull() ?: return
        val y = args[3].toDoubleOrNull() ?: return
        val z = args[4].toDoubleOrNull() ?: return
        val world = if (args.size >= 6) Bukkit.getWorld(args[5]) else (sender as? Player)?.world ?: Bukkit.getWorlds()[0]
        if (world == null) return

        val loc = Location(world, x, y, z, npc.transform.yaw, npc.transform.pitch)
        npcService.setLocation(npc, loc)
        sender.sendMessage(configManager.parse("<green>Moved ${npc.id} to $x, $y, $z in ${world.name}!</green>"))
    }

    private fun handleCenter(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        val loc = npc.transform.toBukkitLocation() ?: return
        loc.x = Math.floor(loc.x) + 0.5
        loc.z = Math.floor(loc.z) + 0.5
        npcService.setLocation(npc, loc)
        sender.sendMessage(configManager.parse("<green>Centered ${npc.id} on block!</green>"))
    }

    private fun handleRotate(sender: CommandSender, args: Array<out String>) {
        if (args.size < 4) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        val yaw = args[2].toFloatOrNull() ?: return
        val pitch = args[3].toFloatOrNull() ?: return
        npcService.setRotation(npc, yaw, pitch)
        sender.sendMessage(configManager.parse("<green>Rotated ${npc.id} to yaw $yaw, pitch $pitch!</green>"))
    }

    private fun handleTeleport(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player || args.size < 2) return
        val npc = npcService.getNPC(args[1]).orElse(null) ?: return
        val loc = npc.location
        if (loc.world != null) {
            sender.teleport(loc)
            sender.sendMessage(configManager.parse("<green>Teleported to ${npc.id}!</green>"))
        }
    }

    private fun handleAction(sender: CommandSender, args: Array<out String>) {
        if (args.size < 4) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        val trigger = NPCActionTrigger.parse(args[2])
        val subAction = args[3].lowercase(Locale.ROOT)

        when (subAction) {
            "add" -> {
                if (args.size < 6) return
                val type = args[4]
                val value = args.drop(5).joinToString(" ")
                npc.actions.add(ActionDefinition(trigger, type, value))
                npcService.save(npc)
                sender.sendMessage(configManager.parse("<green>Action added to ${npc.id} ($trigger)!</green>"))
            }
            "list" -> {
                val list = npc.actions.filter { it.trigger == trigger }
                if (list.isEmpty()) {
                    sender.sendMessage(configManager.parse("<gold>No actions registered for $trigger</gold>"))
                } else {
                    sender.sendMessage(configManager.parse("<gold>Actions for ${npc.id} ($trigger):</gold>"))
                    list.forEachIndexed { i, a ->
                        sender.sendMessage(configManager.parse("  <white>#$i [${a.type}]: ${a.value}</white>"))
                    }
                }
            }
            "remove" -> {
                if (args.size < 5) return
                val index = args[4].toIntOrNull() ?: return
                val matching = npc.actions.filter { it.trigger == trigger }
                if (index in matching.indices) {
                    val toRemove = matching[index]
                    npc.actions.remove(toRemove)
                    npcService.save(npc)
                    sender.sendMessage(configManager.parse("<green>Action removed from ${npc.id}!</green>"))
                }
            }
        }
    }

    private fun handleCooldown(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        val cd = if (args[2].equals("disabled", ignoreCase = true)) 0.0 else args[2].toDoubleOrNull() ?: 1.5
        npc.interactionCooldownSeconds = cd
        npcService.save(npc)
        sender.sendMessage(configManager.parse("<green>Interaction cooldown for ${npc.id}: $cd seconds</green>"))
    }

    private fun handleGroup(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) return
        val npc = npcService.getNPC(args[1]).orElse(null) as? NpcDefinition ?: return
        val group = if (args[2].equals("none", ignoreCase = true) || args[2].equals("root", ignoreCase = true)) null else args[2]
        try {
            npcService.moveToGroup(npc, group)
            sender.sendMessage(configManager.parse("<green>Group for ${npc.id} set to: ${group ?: "none"}</green>"))
        } catch (e: IllegalArgumentException) {
            sender.sendMessage(configManager.parse("<red>${e.message}</red>"))
        }
    }

    private fun sendHelp(sender: CommandSender) {
        val help = listOf(
            "<gradient:#ff0077:#ff7700><bold>=== AxoNPCs Command Guide ===</bold></gradient>",
            "<yellow>/npc create <id></yellow> - Create an NPC at current location",
            "<yellow>/npc copy <source> <new_id> [here|same]</yellow> - Clone an NPC with all properties",
            "<yellow>/npc remove <id></yellow> - Remove an NPC",
            "<yellow>/npc list</yellow> - List registered NPCs",
            "<yellow>/npc info <id></yellow> - View NPC details",
            "<yellow>/npc type <id> <type></yellow> - Set entity type (PLAYER, VILLAGER, etc)",
            "<yellow>/npc displayname <id> <name></yellow> - Set visible hologram name",
            "<yellow>/npc skin <id> <player|MineSkin URL|mineskin:id|@mirror|@none></yellow> - Set NPC skin",
            "<yellow>/npc equip <id> auto</yellow> - Copy armor & items from player",
            "<yellow>/npc glowing <id> <color|off></yellow> - Enable glowing effect",
            "<yellow>/npc moveto <id> <x y z></yellow> - Teleport NPC to location",
            "<yellow>/npc action <id> <trigger> add <message|command|server> <value></yellow> - Add click action"
        )
        help.forEach { sender.sendMessage(configManager.parse(it)) }
    }

    override fun suggest(commandSource: CommandSourceStack, args: Array<out String>): Collection<String> {
        val sender = commandSource.sender
        if (args.isEmpty()) return permittedSubcommands(sender)

        if (args.size == 1) {
            return matching(args[0], permittedSubcommands(sender))
        }

        val sub = args[0].lowercase(Locale.ROOT)
        if (!hasSubcommandPermission(sender, sub)) return emptyList()

        if (args.size == 2) {
            return when (sub) {
                "create" -> matching(args[1], availableNpcIds())
                "copy", "clone" -> matching(args[1], npcIds())
                "nearby" -> matching(args[1], listOf("8", "16", "32", "64"))
                "list", "help" -> emptyList()
                else -> matching(args[1], npcIds())
            }
        }

        if (args.size == 3) {
            return when (sub) {
                "copy", "clone" -> matching(args[2], listOf("${args[1]}_copy") + availableNpcIds())
                "type" -> matching(args[2], entityTypes())
                "displayname" -> matching(args[2], listOf("@none", "{player}", "{npc}"))
                "skin" -> matching(args[2], skinSources(sender))
                "equip", "equipment" -> matching(args[2], listOf("auto") + equipmentSlots())
                "glowing" -> matching(args[2], GLOW_COLORS)
                "collidable", "sound", "turn_to_player" -> matching(args[2], BOOLEAN_VALUES)
                "scale" -> matching(args[2], listOf("0.5", "0.75", "1", "1.5", "2"))
                "health" -> matching(args[2], listOf("1", "10", "20", "40", "100"))
                "viewdistance" -> matching(args[2], listOf("16", "32", "48", "64", "96"))
                "behavior" -> matching(args[2], listOf("stationary", "walk", "look_at_player", "animation"))
                "turn_to_player_distance" -> matching(args[2], listOf("8", "16", "24", "32"))
                "moveto" -> matching(args[2], coordinateSuggestions(sender, CoordinateAxis.X))
                "rotate" -> matching(args[2], yawSuggestions(sender))
                "action" -> matching(args[2], NPCActionTrigger.entries.map { it.name })
                "interactioncooldown" -> matching(args[2], listOf("disabled", "0", "0.5", "1", "1.5", "3"))
                "group" -> matching(args[2], listOf("none", "root") + groupNames())
                "trim" -> matching(args[2], listOf("helmet", "chestplate", "leggings", "boots"))
                else -> emptyList()
            }
        }

        if (args.size == 4) {
            return when (sub) {
                "copy", "clone" -> matching(args[3], listOf("here", "same"))
                "equip", "equipment" -> if (args[2].equals("auto", true)) emptyList() else itemSuggestions(args[3])
                "behavior" -> matching(args[3], listOf("2", "4", "8", "16"))
                "moveto" -> matching(args[3], coordinateSuggestions(sender, CoordinateAxis.Y))
                "rotate" -> matching(args[3], pitchSuggestions(sender))
                "action" -> matching(args[3], listOf("add", "list", "remove"))
                "trim" -> matching(args[3], listOf("clear") + Registry.TRIM_PATTERN.map { it.key.key })
                else -> emptyList()
            }
        }

        if (args.size == 5) {
            return when (sub) {
                "moveto" -> matching(args[4], coordinateSuggestions(sender, CoordinateAxis.Z))
                "action" -> when (args[3].lowercase(Locale.ROOT)) {
                    "add" -> matching(args[4], ACTION_TYPES)
                    "remove" -> matching(args[4], actionIndices(args[1], args[2]))
                    else -> emptyList()
                }
                "trim" -> matching(args[4], Registry.TRIM_MATERIAL.map { it.key.key })
                else -> emptyList()
            }
        }

        if (args.size == 6 && sub == "moveto") {
            return matching(args[5], Bukkit.getWorlds().map { it.name })
        }

        if (args.size >= 6 && sub == "action" && args[3].equals("add", true)) {
            return matching(args.last(), listOf("{player}", "{npc}", "{world}"))
        }

        return emptyList()
    }

    private fun permittedSubcommands(sender: CommandSender): List<String> = SUBCOMMANDS.filter {
        hasSubcommandPermission(sender, it)
    }

    private fun npcIds(): List<String> = npcService.allNPCs.map { it.id }.sortedBy { it.lowercase(Locale.ROOT) }

    private fun availableNpcIds(): List<String> {
        val used = npcIds().map { it.lowercase(Locale.ROOT) }.toSet()
        return generateSequence(1) { it + 1 }
            .map { if (it == 1) "npc" else "npc_$it" }
            .filter { it.lowercase(Locale.ROOT) !in used }
            .take(3)
            .toList()
    }

    private fun groupNames(): List<String> = (groupService.getGroups().map { it.name } + npcService.allNPCs
        .mapNotNull { (it as? NpcDefinition)?.group })
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .sortedBy { it.lowercase(Locale.ROOT) }

    private fun entityTypes(): List<String> = EntityType.entries
        .filter { it == EntityType.PLAYER || it.isSpawnable }
        .map { it.name }

    private fun equipmentSlots(): List<String> = NPCEquipmentSlot.entries.map { it.name.lowercase(Locale.ROOT) }

    private fun skinSources(sender: CommandSender): List<String> = buildList {
        add("@mirror")
        add("@none")
        add("mineskin:")
        if (sender is Player) add(sender.name)
        addAll(Bukkit.getOnlinePlayers().map { it.name })
    }.distinctBy { it.lowercase(Locale.ROOT) }

    private fun itemSuggestions(input: String): List<String> {
        val quick = listOf("hand", "clear", "diamond_sword", "bow", "shield", "air")
        if (input.isBlank()) return quick
        return matching(input, quick + Material.entries
            .filterNot { it.name.startsWith("LEGACY_") }
            .map { it.name.lowercase(Locale.ROOT) })
    }

    private fun actionIndices(npcId: String, triggerInput: String): List<String> {
        val npc = npcService.getNPC(npcId).orElse(null) as? NpcDefinition ?: return emptyList()
        val trigger = NPCActionTrigger.parse(triggerInput)
        return npc.actions.filter { it.trigger == trigger }.indices.map { it.toString() }
    }

    private fun coordinateSuggestions(sender: CommandSender, axis: CoordinateAxis): List<String> {
        val player = sender as? Player ?: return listOf("0")
        val value = when (axis) {
            CoordinateAxis.X -> player.location.x
            CoordinateAxis.Y -> player.location.y
            CoordinateAxis.Z -> player.location.z
        }
        return listOf(formatNumber(value), formatNumber(kotlin.math.floor(value) + 0.5))
    }

    private fun yawSuggestions(sender: CommandSender): List<String> = buildList {
        if (sender is Player) add(formatNumber(sender.location.yaw.toDouble()))
        addAll(listOf("0", "90", "180", "-90"))
    }.distinct()

    private fun pitchSuggestions(sender: CommandSender): List<String> = buildList {
        if (sender is Player) add(formatNumber(sender.location.pitch.toDouble()))
        addAll(listOf("0", "45", "-45"))
    }.distinct()

    private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        "%.2f".format(Locale.ROOT, value)
    }

    private fun matching(input: String, values: Collection<String>): List<String> = values
        .distinctBy { it.lowercase(Locale.ROOT) }
        .filter { it.startsWith(input, ignoreCase = true) }

    private enum class CoordinateAxis { X, Y, Z }

    companion object {
        private val SUBCOMMANDS = listOf(
            "help", "create", "copy", "clone", "remove", "delete", "list", "info", "nearby", "type", "displayname",
            "skin", "equip", "equipment", "glowing", "collidable", "scale", "sound", "health",
            "viewdistance", "behavior", "turn_to_player", "turn_to_player_distance", "movehere",
            "moveto", "center", "rotate", "teleport", "tp", "action", "interactioncooldown", "group", "trim"
        )
        private val BOOLEAN_VALUES = listOf("true", "false")
        private val GLOW_COLORS = listOf(
            "off", "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold",
            "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"
        )
        private val ACTION_TYPES = listOf(
            "message", "chat", "command", "player_command", "server", "console_command"
        )
    }

    private fun hasSubcommandPermission(sender: CommandSender, subcommand: String): Boolean {
        if (subcommand.equals("help", ignoreCase = true)) return true
        if (sender.hasPermission("axonpcs.admin") ||
            sender.hasPermission("axonpcs.command.*") ||
            sender.hasPermission("axonpcs.command.npc.*")) {
            return true
        }
        val node = when (subcommand.lowercase(Locale.ROOT)) {
            "clone" -> "copy"
            "delete" -> "remove"
            "equipment" -> "equip"
            "movehere", "moveto", "center" -> "move"
            "tp" -> "teleport"
            else -> subcommand.lowercase(Locale.ROOT)
        }
        return sender.hasPermission("axonpcs.command.npc.$node")
    }
}
