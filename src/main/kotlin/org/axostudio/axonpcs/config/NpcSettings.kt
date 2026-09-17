package org.axostudio.axonpcs.config

import org.bukkit.configuration.file.FileConfiguration

data class NpcSettings(
    val defaultViewDistance: Double = 48.0,
    val updateIntervalTicks: Long = 10L,
    val joinDelayTicks: Long = 20L,
    val worldChangeDelayTicks: Long = 8L,
    val bedrockWorldChangeDelayTicks: Long = 20L,
    val defaultCooldownSeconds: Double = 1.5,
    val isOptimizationEnabled: Boolean = true,
    val idleModeEnabled: Boolean = true,
    val idleWakeUpRange: Double = 48.0,
    val idleSleepAfterSeconds: Double = 10.0,
    val rotationEnabled: Boolean = true,
    val rotationIntervalTicks: Long = 10L,
    val rotationMaxDistance: Double = 16.0,
    val visibilityIntervalTicks: Long = 20L,
    val visibilityMaxDistance: Double = 48.0,
    val movementEnabled: Boolean = true,
    val maxMovementRange: Double = 8.0,
    val saveOnChange: Boolean = true
) {
    companion object {
        fun fromConfig(config: FileConfiguration): NpcSettings {
            return NpcSettings(
                defaultViewDistance = config.getDouble("rendering.view-distance", 48.0),
                updateIntervalTicks = config.getLong("rendering.update-interval-ticks", 10L),
                joinDelayTicks = config.getLong("rendering.join-delay-ticks", 20L),
                worldChangeDelayTicks = config.getLong("rendering.world-change-delay-ticks", 8L),
                bedrockWorldChangeDelayTicks = config.getLong("rendering.bedrock-world-change-delay-ticks", 20L),
                defaultCooldownSeconds = config.getDouble("interaction.default-cooldown-seconds", 1.5),
                isOptimizationEnabled = config.getBoolean("optimization.enabled", true),
                idleModeEnabled = config.getBoolean("optimization.idle-mode.enabled", true),
                idleWakeUpRange = config.getDouble("optimization.idle-mode.wake-up-range", 48.0),
                idleSleepAfterSeconds = config.getDouble("optimization.idle-mode.sleep-after-seconds", 10.0),
                rotationEnabled = config.getBoolean("optimization.rotation.enabled", true),
                rotationIntervalTicks = config.getLong("optimization.rotation.update-interval-ticks", 10L),
                rotationMaxDistance = config.getDouble("optimization.rotation.max-distance", 16.0),
                visibilityIntervalTicks = config.getLong("optimization.visibility.update-interval-ticks", 20L),
                visibilityMaxDistance = config.getDouble("optimization.visibility.max-distance", 48.0),
                movementEnabled = config.getBoolean("optimization.behavior.movement-enabled", true),
                maxMovementRange = config.getDouble("optimization.behavior.max-movement-range", 8.0),
                saveOnChange = config.getBoolean("storage.save-on-change", true)
            )
        }
    }
}
