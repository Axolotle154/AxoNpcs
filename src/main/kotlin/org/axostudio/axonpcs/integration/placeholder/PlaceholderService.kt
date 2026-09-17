package org.axostudio.axonpcs.integration.placeholder

import org.bukkit.Bukkit
import org.bukkit.entity.Player

class PlaceholderService {
    private val isPapiPresent: Boolean = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")

    fun setPlaceholders(player: Player?, text: String): String {
        if (!isPapiPresent || player == null) return text
        return try {
            val papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
            val setPlaceholders = papiClass.getMethod("setPlaceholders", Player::class.java, String::class.java)
            setPlaceholders.invoke(null, player, text) as String
        } catch (ignored: Throwable) {
            text
        }
    }
}
