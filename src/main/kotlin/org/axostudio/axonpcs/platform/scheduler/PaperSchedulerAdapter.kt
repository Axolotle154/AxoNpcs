package org.axostudio.axonpcs.platform.scheduler

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class PaperSchedulerAdapter(private val plugin: Plugin) : SchedulerAdapter {

    override fun isFolia(): Boolean = false

    override fun runGlobal(action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val task = Bukkit.getScheduler().runTask(plugin, Runnable { action() })
        return TaskHandle { task.cancel() }
    }

    override fun runGlobalDelayed(delayTicks: Long, action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val safeDelay = maxOf(0L, delayTicks)
        val task = Bukkit.getScheduler().runTaskLater(plugin, Runnable { action() }, safeDelay)
        return TaskHandle { task.cancel() }
    }

    override fun runGlobalRepeating(initialDelayTicks: Long, intervalTicks: Long, action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val safeInitial = maxOf(1L, initialDelayTicks)
        val safeInterval = maxOf(1L, intervalTicks)
        val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { action() }, safeInitial, safeInterval)
        return TaskHandle { task.cancel() }
    }

    override fun runAsync(action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val task = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable { action() })
        return TaskHandle { task.cancel() }
    }

    override fun runAsyncDelayed(delayTicks: Long, action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val safeDelay = maxOf(0L, delayTicks)
        val task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable { action() }, safeDelay)
        return TaskHandle { task.cancel() }
    }

    override fun runAsyncRepeating(initialDelayTicks: Long, intervalTicks: Long, action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val safeInitial = maxOf(1L, initialDelayTicks)
        val safeInterval = maxOf(1L, intervalTicks)
        val task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable { action() }, safeInitial, safeInterval)
        return TaskHandle { task.cancel() }
    }

    override fun runEntity(player: Player, action: () -> Unit): TaskHandle {
        return runGlobal(action)
    }

    override fun runEntityDelayed(player: Player, delayTicks: Long, action: () -> Unit): TaskHandle {
        return runGlobalDelayed(delayTicks, action)
    }

    override fun runRegion(location: Location, action: () -> Unit): TaskHandle {
        return runGlobal(action)
    }
}
