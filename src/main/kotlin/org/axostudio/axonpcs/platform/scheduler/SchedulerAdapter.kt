package org.axostudio.axonpcs.platform.scheduler

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

fun interface TaskHandle {
    fun cancel()
}

interface SchedulerAdapter {
    fun isFolia(): Boolean

    fun runGlobal(action: () -> Unit): TaskHandle

    fun runGlobalDelayed(delayTicks: Long, action: () -> Unit): TaskHandle

    fun runGlobalRepeating(initialDelayTicks: Long, intervalTicks: Long, action: () -> Unit): TaskHandle

    fun runAsync(action: () -> Unit): TaskHandle

    fun runAsyncDelayed(delayTicks: Long, action: () -> Unit): TaskHandle

    fun runAsyncRepeating(initialDelayTicks: Long, intervalTicks: Long, action: () -> Unit): TaskHandle

    fun runEntity(player: Player, action: () -> Unit): TaskHandle

    fun runEntityDelayed(player: Player, delayTicks: Long, action: () -> Unit): TaskHandle

    fun runRegion(location: Location, action: () -> Unit): TaskHandle

    companion object {
        fun create(plugin: Plugin): SchedulerAdapter {
            val isFolia = try {
                Bukkit::class.java.getMethod("getGlobalRegionScheduler")
                true
            } catch (e: NoSuchMethodException) {
                false
            }

            return if (isFolia) {
                FoliaSchedulerAdapter(plugin)
            } else {
                PaperSchedulerAdapter(plugin)
            }
        }
    }
}
