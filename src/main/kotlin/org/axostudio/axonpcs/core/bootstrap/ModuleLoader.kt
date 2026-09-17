package org.axostudio.axonpcs.core.bootstrap

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.axostudio.axonpcs.api.service.AxoNPCProvider
import org.axostudio.axonpcs.command.npc.NpcCommandExecutor
import org.axostudio.axonpcs.command.root.AxoNPCCommand
import org.axostudio.axonpcs.config.ConfigManager
import org.axostudio.axonpcs.core.module.AxoModule
import org.axostudio.axonpcs.core.module.ModuleManager
import org.axostudio.axonpcs.integration.geyser.BedrockDetector
import org.axostudio.axonpcs.integration.placeholder.PlaceholderService
import org.axostudio.axonpcs.listener.PlayerConnectionListener
import org.axostudio.axonpcs.listener.WorldChangeListener
import org.axostudio.axonpcs.migration.MigrationService
import org.axostudio.axonpcs.platform.scheduler.SchedulerAdapter
import org.axostudio.axonpcs.protocol.adapter.ProtocolAdapter
import org.axostudio.axonpcs.render.equipment.EquipmentService
import org.axostudio.axonpcs.render.skin.SkinService
import org.axostudio.axonpcs.runtime.registry.GroupRegistry
import org.axostudio.axonpcs.runtime.registry.NpcRegistry
import org.axostudio.axonpcs.runtime.service.GroupService
import org.axostudio.axonpcs.runtime.service.NpcActionHandler
import org.axostudio.axonpcs.runtime.service.NpcService
import org.axostudio.axonpcs.storage.repository.GroupRepository
import org.axostudio.axonpcs.storage.repository.NpcRepository
import org.axostudio.axonpcs.tracking.spatial.SpatialChunkGrid
import org.axostudio.axonpcs.tracking.sync.WorldSyncManager
import org.axostudio.axonpcs.tracking.viewer.VisibilityController
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class ModuleLoader(
    private val plugin: JavaPlugin,
    private val services: ServiceRegistry,
    private val moduleManager: ModuleManager
) {

    fun setupModules() {
        val scheduler = services.get<SchedulerAdapter>()
        val configManager = services.get<ConfigManager>()

        // 1. Protocol Module
        val protocolAdapter = ProtocolAdapter()
        services.register(ProtocolAdapter::class, protocolAdapter)

        moduleManager.register(object : AxoModule {
            override val name: String = "Protocol"
            override fun onEnable() {
                // Injected players setup
                for (p in Bukkit.getOnlinePlayers()) {
                    // Inject will be hooked with listener
                }
            }
            override fun onDisable() {
                for (p in Bukkit.getOnlinePlayers()) {
                    protocolAdapter.uninject(p)
                }
            }
        })

        // 2. Integration & Services
        val bedrockDetector = BedrockDetector()
        val placeholderService = PlaceholderService()
        val skinService = SkinService(plugin)
        val equipmentService = EquipmentService()
        services.register(BedrockDetector::class, bedrockDetector)
        services.register(PlaceholderService::class, placeholderService)
        services.register(SkinService::class, skinService)
        services.register(EquipmentService::class, equipmentService)

        // 3. Storage & Registries
        val npcRepository = NpcRepository(plugin)
        val groupRepository = GroupRepository(plugin)
        val npcRegistry = NpcRegistry()
        val groupRegistry = GroupRegistry()
        val spatialGrid = SpatialChunkGrid()
        services.register(NpcRepository::class, npcRepository)
        services.register(GroupRepository::class, groupRepository)
        services.register(NpcRegistry::class, npcRegistry)
        services.register(GroupRegistry::class, groupRegistry)
        services.register(SpatialChunkGrid::class, spatialGrid)

        // 4. Tracking Module
        val visibilityController = VisibilityController(protocolAdapter, configManager, scheduler, npcRegistry, spatialGrid)
        val worldSyncManager = WorldSyncManager(visibilityController, protocolAdapter, configManager, scheduler) { p: Player ->
            bedrockDetector.isBedrockPlayer(p)
        }
        services.register(VisibilityController::class, visibilityController)
        services.register(WorldSyncManager::class, worldSyncManager)

        moduleManager.register(object : AxoModule {
            override val name: String = "Tracking"
            override fun onEnable() {
                visibilityController.start()
            }
            override fun onDisable() {
                visibilityController.stop()
            }
        })

        // 5. Runtime Module
        val npcService = NpcService(npcRegistry, npcRepository, spatialGrid, visibilityController, skinService, scheduler)
        val groupService = GroupService(groupRegistry, groupRepository)
        val actionHandler = NpcActionHandler(configManager, placeholderService, scheduler)
        services.register(NpcService::class, npcService)
        services.register(GroupService::class, groupService)
        services.register(NpcActionHandler::class, actionHandler)

        // Register Public API Providers
        org.axostudio.axonpcs.api.service.AxoNPCProvider.register(npcService)
        org.axostudio.axonpcs.api.AxoNPCsProvider.register(npcService)

        moduleManager.register(object : AxoModule {
            override val name: String = "Runtime"
            override fun onEnable() {
                groupService.loadAll()
                val loadedCount = npcService.reload()
                plugin.logger.info("Loaded $loadedCount virtual NPCs successfully.")
            }
            override fun onDisable() {
                org.axostudio.axonpcs.api.service.AxoNPCProvider.unregister()
                org.axostudio.axonpcs.api.AxoNPCsProvider.unregister()
            }
            override fun onReload() {
                npcService.reload()
            }
        })

        // 6. Listeners Module
        moduleManager.register(object : AxoModule {
            override val name: String = "Listeners"
            override fun onEnable() {
                val connListener = PlayerConnectionListener(worldSyncManager, protocolAdapter, npcRegistry, actionHandler)
                val worldListener = WorldChangeListener(worldSyncManager)
                Bukkit.getPluginManager().registerEvents(connListener, plugin)
                Bukkit.getPluginManager().registerEvents(worldListener, plugin)

                // Inject existing online players
                for (player in Bukkit.getOnlinePlayers()) {
                    protocolAdapter.inject(player) { uuid, interactPacket ->
                        val npc = npcRegistry.getByEntityId(interactPacket.entityId()) ?: return@inject false
                        actionHandler.handleInteract(player, npc, interactPacket.trigger())
                        true
                    }
                    worldSyncManager.handleJoin(player)
                }
            }
            override fun onDisable() {
            }
        })

        // 7. Commands Module
        val migrationService = MigrationService(plugin.dataFolder, npcRepository, npcRegistry, spatialGrid, visibilityController, skinService, scheduler)
        services.register(MigrationService::class, migrationService)

        moduleManager.register(object : AxoModule {
            override val name: String = "Commands"
            override fun onEnable() {
                val axoCommand = AxoNPCCommand(plugin, configManager, npcService, migrationService)
                val npcCommand = NpcCommandExecutor(
                    configManager,
                    npcService,
                    skinService,
                    equipmentService,
                    groupService,
                    visibilityController,
                    scheduler
                )

                plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
                    event.registrar().register(
                        "axonpcs",
                        "Main AxoNPCs administration command.",
                        listOf("axonpc"),
                        axoCommand
                    )
                    event.registrar().register(
                        "npc",
                        "Create and manage AxoNPCs virtual NPCs.",
                        emptyList(),
                        npcCommand
                    )
                }
            }
            override fun onDisable() {
            }
        })
    }
}
