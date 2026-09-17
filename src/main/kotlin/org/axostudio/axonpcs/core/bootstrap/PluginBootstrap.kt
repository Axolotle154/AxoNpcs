package org.axostudio.axonpcs.core.bootstrap

import org.axostudio.axonpcs.config.ConfigManager
import org.axostudio.axonpcs.core.lifecycle.PluginLifecycle
import org.axostudio.axonpcs.core.module.ModuleManager
import org.axostudio.axonpcs.platform.scheduler.SchedulerAdapter
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class PluginBootstrap(
    private val plugin: JavaPlugin,
    private val lifecycle: PluginLifecycle,
    private val services: ServiceRegistry,
    private val moduleManager: ModuleManager
) {

    fun init() {
        lifecycle.markBootstrapping()

        // 1. Create data directories if needed
        listOf("lang", "skins", "npcs").forEach {
            val dir = java.io.File(plugin.dataFolder, it)
            if (!dir.exists()) dir.mkdirs()
        }

        // 2. Scheduler
        val scheduler = SchedulerAdapter.create(plugin)
        services.register(SchedulerAdapter::class, scheduler)

        // 3. Config
        val configManager = ConfigManager(plugin)
        configManager.load()
        services.register(ConfigManager::class, configManager)

        // 4. Modules
        val loader = ModuleLoader(plugin, services, moduleManager)
        loader.setupModules()
        moduleManager.enableAll()

        lifecycle.markRunning()
        sendStartupBanner(configManager)
    }

    fun shutdown() {
        lifecycle.markStopping()
        moduleManager.disableAll()
        services.clear()
        lifecycle.markStopped()
    }

    private fun sendStartupBanner(configManager: ConfigManager) {
        val banner = arrayOf(
            "   _____                  _______                        ",
            "  /  _  \\ ___  _______    \\      \\ ______   ____   ______",
            " /  /_\\  \\\\  \\/  /  _ \\   /   |   \\\\____ \\_/ ___\\ /  ___/",
            "/    |    \\>    <  <_> ) /    |    \\  |_> >  \\___ \\___ \\ ",
            "\\____|__  /__/\\_ \\____/  \\____|__  /   __/ \\___  >____  >",
            "        \\/      \\/               \\/|__|        \\/     \\/ "
        )
        for (line in banner) {
            Bukkit.getConsoleSender().sendMessage(configManager.parse("<light_purple>$line</light_purple>"))
        }
        Bukkit.getConsoleSender().sendMessage(configManager.parse(
            "<green>AxoNPCs v${plugin.pluginMeta.version} activated cleanly! (Folia: ${services.get<SchedulerAdapter>().isFolia()})</green>"
        ))
    }
}
