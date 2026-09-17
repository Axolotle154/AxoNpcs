package org.axostudio.axonpcs.runtime.service

import org.axostudio.axonpcs.api.event.npc.AxoNPCInteractEvent
import org.axostudio.axonpcs.api.model.NPCActionTrigger
import org.axostudio.axonpcs.config.ConfigManager
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.integration.placeholder.PlaceholderService
import org.axostudio.axonpcs.platform.scheduler.SchedulerAdapter
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NpcActionHandler(
    private val configManager: ConfigManager,
    private val placeholderService: PlaceholderService,
    private val scheduler: SchedulerAdapter
) {
    private val lastInteractTimes = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()

    /** Called from Netty. All Bukkit player access is deferred to the entity scheduler. */
    fun handleInteract(player: Player, npc: NpcDefinition, trigger: NPCActionTrigger) {
        scheduler.runEntity(player) {
            handleInteractOnPlayerThread(player, npc, trigger)
        }
    }

    private fun handleInteractOnPlayerThread(player: Player, npc: NpcDefinition, trigger: NPCActionTrigger) {
        if (!player.isOnline) return

        // Cooldown check per player per NPC
        val now = System.currentTimeMillis()
        val playerCooldowns = lastInteractTimes.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }
        val lastTime = playerCooldowns.getOrDefault(npc.id, 0L)
        val cooldownMs = (npc.interactionCooldownSeconds * 1000.0).toLong()

        if (now - lastTime < cooldownMs) {
            return
        }
        playerCooldowns[npc.id] = now

        val event = AxoNPCInteractEvent(player, npc, trigger)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return

        val actions = npc.getActions(trigger)
        for (action in actions) {
            var value = action.value()
                .replace("{player}", player.name)
                .replace("{npc}", npc.id)
                .replace("{world}", npc.transform.world)

            value = placeholderService.setPlaceholders(player, value)

            when (action.type().lowercase()) {
                "message" -> player.sendMessage(configManager.parse(value))
                "chat" -> player.chat(value)
                "command", "player_command" -> player.performCommand(value.removePrefix("/"))
                "console_command", "server" -> scheduler.runGlobal {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value.removePrefix("/"))
                }
            }
        }
    }

    fun clear(player: Player) {
        lastInteractTimes.remove(player.uniqueId)
    }
}
