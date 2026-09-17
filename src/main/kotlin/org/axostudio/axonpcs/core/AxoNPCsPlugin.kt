package org.axostudio.axonpcs.core

import org.axostudio.axonpcs.core.bootstrap.PluginBootstrap
import org.axostudio.axonpcs.core.bootstrap.ServiceRegistry
import org.axostudio.axonpcs.core.lifecycle.PluginLifecycle
import org.axostudio.axonpcs.core.module.ModuleManager
import org.bukkit.plugin.java.JavaPlugin

class AxoNPCsPlugin : JavaPlugin() {
    val lifecycle = PluginLifecycle()
    val services = ServiceRegistry()
    val moduleManager = ModuleManager(this)
    private lateinit var bootstrap: PluginBootstrap

    override fun onEnable() {
        bootstrap = PluginBootstrap(this, lifecycle, services, moduleManager)
        bootstrap.init()
    }

    override fun onDisable() {
        if (::bootstrap.isInitialized) {
            bootstrap.shutdown()
        }
    }
}
