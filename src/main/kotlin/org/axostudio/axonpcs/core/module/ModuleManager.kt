package org.axostudio.axonpcs.core.module

import org.bukkit.plugin.Plugin
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Level

class ModuleManager(private val plugin: Plugin) {
    private val modules = CopyOnWriteArrayList<AxoModule>()

    fun register(module: AxoModule) {
        modules.add(module)
    }

    fun enableAll() {
        for (module in modules) {
            try {
                module.onEnable()
                plugin.logger.info("Enabled module: ${module.name}")
            } catch (t: Throwable) {
                plugin.logger.log(Level.SEVERE, "Failed to enable module: ${module.name}", t)
            }
        }
    }

    fun disableAll() {
        // Disable in reverse order
        for (module in modules.reversed()) {
            try {
                module.onDisable()
            } catch (t: Throwable) {
                plugin.logger.log(Level.WARNING, "Error disabling module: ${module.name}", t)
            }
        }
        modules.clear()
    }

    fun reloadAll() {
        for (module in modules) {
            try {
                module.onReload()
            } catch (t: Throwable) {
                plugin.logger.log(Level.WARNING, "Error reloading module: ${module.name}", t)
            }
        }
    }
}
