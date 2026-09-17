package org.axostudio.axonpcs.platform.scheduler

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.logging.Level

class FoliaSchedulerAdapter(private val plugin: Plugin) : SchedulerAdapter {

    private val globalScheduler: Any by lazy {
        Bukkit::class.java.getMethod("getGlobalRegionScheduler").invoke(null)
    }

    private val asyncScheduler: Any by lazy {
        Bukkit::class.java.getMethod("getAsyncScheduler").invoke(null)
    }

    private val regionScheduler: Any by lazy {
        Bukkit::class.java.getMethod("getRegionScheduler").invoke(null)
    }

    override fun isFolia(): Boolean = true

    override fun runGlobal(action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val consumer = Consumer<Any> { if (plugin.isEnabled) action() }
        val method: Method = globalScheduler.javaClass.getMethod("run", Plugin::class.java, Consumer::class.java)
        val task = method.invoke(globalScheduler, plugin, consumer)
        return wrapTask(task)
    }

    override fun runGlobalDelayed(delayTicks: Long, action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val safeDelay = maxOf(1L, delayTicks)
        val consumer = Consumer<Any> { if (plugin.isEnabled) action() }
        val method: Method = globalScheduler.javaClass.getMethod("runDelayed", Plugin::class.java, Consumer::class.java, Long::class.javaPrimitiveType)
        val task = method.invoke(globalScheduler, plugin, consumer, safeDelay)
        return wrapTask(task)
    }

    override fun runGlobalRepeating(initialDelayTicks: Long, intervalTicks: Long, action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val safeInitial = maxOf(1L, initialDelayTicks)
        val safeInterval = maxOf(1L, intervalTicks)
        val consumer = Consumer<Any> { if (plugin.isEnabled) action() }
        val method: Method = globalScheduler.javaClass.getMethod(
            "runAtFixedRate",
            Plugin::class.java, Consumer::class.java, Long::class.javaPrimitiveType, Long::class.javaPrimitiveType
        )
        val task = method.invoke(globalScheduler, plugin, consumer, safeInitial, safeInterval)
        return wrapTask(task)
    }

    override fun runAsync(action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val consumer = Consumer<Any> { if (plugin.isEnabled) action() }
        val method: Method = asyncScheduler.javaClass.getMethod("runNow", Plugin::class.java, Consumer::class.java)
        val task = method.invoke(asyncScheduler, plugin, consumer)
        return wrapTask(task)
    }

    override fun runAsyncDelayed(delayTicks: Long, action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val safeDelayMs = maxOf(1L, delayTicks * 50L)
        val consumer = Consumer<Any> { if (plugin.isEnabled) action() }
        val method: Method = asyncScheduler.javaClass.getMethod(
            "runDelayed",
            Plugin::class.java, Consumer::class.java, Long::class.javaPrimitiveType, TimeUnit::class.java
        )
        val task = method.invoke(asyncScheduler, plugin, consumer, safeDelayMs, TimeUnit.MILLISECONDS)
        return wrapTask(task)
    }

    override fun runAsyncRepeating(initialDelayTicks: Long, intervalTicks: Long, action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        val safeInitialMs = maxOf(50L, initialDelayTicks * 50L)
        val safeIntervalMs = maxOf(50L, intervalTicks * 50L)
        val consumer = Consumer<Any> { if (plugin.isEnabled) action() }
        val method: Method = asyncScheduler.javaClass.getMethod(
            "runAtFixedRate",
            Plugin::class.java, Consumer::class.java, Long::class.javaPrimitiveType, Long::class.javaPrimitiveType, TimeUnit::class.java
        )
        val task = method.invoke(asyncScheduler, plugin, consumer, safeInitialMs, safeIntervalMs, TimeUnit.MILLISECONDS)
        return wrapTask(task)
    }

    override fun runEntity(player: Player, action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        return try {
            val entityScheduler = player.javaClass.getMethod("getScheduler").invoke(player)
            val consumer = Consumer<Any> { if (plugin.isEnabled && player.isOnline) action() }
            val method: Method = entityScheduler.javaClass.getMethod(
                "run",
                Plugin::class.java, Consumer::class.java, Runnable::class.java
            )
            val task = method.invoke(entityScheduler, plugin, consumer, null)
            wrapTask(task)
        } catch (t: Throwable) {
            plugin.logger.log(Level.SEVERE, "Could not schedule task on an entity scheduler", t)
            TaskHandle {}
        }
    }

    override fun runEntityDelayed(player: Player, delayTicks: Long, action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled) return TaskHandle {}
        if (delayTicks <= 0L) return runEntity(player, action)
        return try {
            val entityScheduler = player.javaClass.getMethod("getScheduler").invoke(player)
            val consumer = Consumer<Any> { if (plugin.isEnabled && player.isOnline) action() }
            val method: Method = entityScheduler.javaClass.getMethod(
                "runDelayed",
                Plugin::class.java, Consumer::class.java, Runnable::class.java, Long::class.javaPrimitiveType
            )
            val task = method.invoke(entityScheduler, plugin, consumer, null, delayTicks)
            wrapTask(task)
        } catch (t: Throwable) {
            plugin.logger.log(Level.SEVERE, "Could not schedule delayed task on an entity scheduler", t)
            TaskHandle {}
        }
    }

    override fun runRegion(location: Location, action: () -> Unit): TaskHandle {
        if (!plugin.isEnabled || location.world == null) return TaskHandle {}
        return try {
            val consumer = Consumer<Any> { if (plugin.isEnabled) action() }
            val method: Method = regionScheduler.javaClass.getMethod(
                "run",
                Plugin::class.java, Location::class.java, Consumer::class.java
            )
            val task = method.invoke(regionScheduler, plugin, location, consumer)
            wrapTask(task)
        } catch (t: Throwable) {
            plugin.logger.log(Level.SEVERE, "Could not schedule task for region at $location", t)
            TaskHandle {}
        }
    }

    private fun wrapTask(task: Any?): TaskHandle {
        if (task == null) return TaskHandle {}
        return TaskHandle {
            try {
                task.javaClass.getMethod("cancel").invoke(task)
            } catch (ignored: Throwable) {
            }
        }
    }
}
