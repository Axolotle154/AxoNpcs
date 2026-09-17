package org.axostudio.axonpcs.command.root

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.axostudio.axonpcs.config.ConfigManager
import org.axostudio.axonpcs.migration.MigrationService
import org.axostudio.axonpcs.runtime.service.NpcService
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import java.util.Locale

class AxoNPCCommand(
    private val plugin: Plugin,
    private val configManager: ConfigManager,
    private val npcService: NpcService,
    private val migrationService: MigrationService? = null
) : BasicCommand {

    override fun execute(commandSource: CommandSourceStack, args: Array<out String>) {
        val sender = commandSource.sender
        if (args.isEmpty()) {
            configManager.send(sender, "invalid-usage", mapOf("usage" to "/axonpcs <version|reload|featureflags|migrate>"))
            return
        }

        when (args[0].lowercase(Locale.ROOT)) {
            "version" -> {
                if (!hasPermission(sender, "version")) {
                    configManager.send(sender, "no-permission")
                    return
                }
                configManager.send(sender, "version", mapOf(
                    "version" to plugin.pluginMeta.version,
                    "server" to plugin.server.version
                ))
            }
            "reload" -> {
                if (!hasPermission(sender, "reload")) {
                    configManager.send(sender, "no-permission")
                    return
                }
                configManager.load()
                val count = npcService.reload()
                configManager.send(sender, "reload", mapOf("count" to count.toString()))
            }
            "featureflags" -> {
                if (!hasPermission(sender, "featureflags")) {
                    configManager.send(sender, "no-permission")
                    return
                }
                val flags = "packet-npcs=true, skins=true, mirror-skins=true, glowing=true, equipment=true, scale=true"
                configManager.send(sender, "featureflags", mapOf("flags" to flags))
            }
            "migrate" -> {
                if (!hasPermission(sender, "migrate")) {
                    configManager.send(sender, "no-permission")
                    return
                }
                if (args.size < 2) {
                    configManager.send(sender, "invalid-usage", mapOf("usage" to "/axonpcs migrate <citizens|fancynpcs|znpcs>"))
                    return
                }
                if (migrationService == null) {
                    sender.sendMessage("Migration service is not available.")
                    return
                }
                val source = args[1].lowercase(Locale.ROOT)
                val result = migrationService.migrate(source)
                sender.sendMessage("§a[AxoNPCs] Migration from $source completed: ${result.successCount} imported, ${result.failedCount} failed.")
                if (result.errors.isNotEmpty()) {
                    for (err in result.errors.take(5)) {
                        sender.sendMessage("§c - $err")
                    }
                }
            }
            else -> {
                configManager.send(sender, "unknown-command")
            }
        }
    }

    override fun suggest(
        commandSource: CommandSourceStack,
        args: Array<out String>
    ): Collection<String> {
        val sender = commandSource.sender
        if (args.size == 1) {
            val subs = listOf("version", "reload", "featureflags", "migrate")
            return subs.filter {
                it.startsWith(args[0], ignoreCase = true) && hasPermission(sender, it)
            }
        }
        if (args.size == 2 && args[0].equals("migrate", ignoreCase = true)) {
            if (!hasPermission(sender, "migrate")) return emptyList()
            val sources = listOf("citizens", "fancynpcs", "znpcs")
            return sources.filter { it.startsWith(args[1], ignoreCase = true) }
        }
        return emptyList()
    }

    private fun hasPermission(sender: CommandSender, action: String): Boolean {
        return sender.hasPermission("axonpcs.admin") ||
            sender.hasPermission("axonpcs.command.*") ||
            sender.hasPermission("axonpcs.command.$action")
    }
}
