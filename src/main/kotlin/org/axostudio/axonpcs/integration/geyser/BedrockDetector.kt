package org.axostudio.axonpcs.integration.geyser

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class BedrockDetector {
    private val isFloodgateAvailable: Boolean = Bukkit.getPluginManager().isPluginEnabled("floodgate")

    fun isBedrockPlayer(player: Player): Boolean {
        if (!isFloodgateAvailable) return false
        return try {
            val apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi")
            val getInstance = apiClass.getMethod("getInstance")
            val instance = getInstance.invoke(null)
            val isFloodgatePlayer = apiClass.getMethod("isFloodgatePlayer", UUID::class.java)
            isFloodgatePlayer.invoke(instance, player.uniqueId) as Boolean
        } catch (ignored: Throwable) {
            false
        }
    }
}
