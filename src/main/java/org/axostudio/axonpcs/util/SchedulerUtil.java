package org.axostudio.axonpcs.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class SchedulerUtil {
    private static final TaskHandle NOOP_TASK = () -> {
    };

    private final Plugin plugin;
    private final boolean foliaLike;

    public SchedulerUtil(Plugin plugin) {
        this.plugin = plugin;
        this.foliaLike = hasMethod(Bukkit.class, "getGlobalRegionScheduler");
    }

    public boolean isFoliaLike() {
        return foliaLike;
    }

    public TaskHandle runGlobal(Runnable runnable) {
        if (!plugin.isEnabled()) {
            return NOOP_TASK;
        }
        if (foliaLike) {
            Object scheduler = invokeStatic(Bukkit.class, "getGlobalRegionScheduler");
            Object task = invoke(scheduler, "run", new Class<?>[]{Plugin.class, Consumer.class}, plugin, guardedConsumer(runnable));
            return new ReflectionTaskHandle(task);
        }
        BukkitTask task = Bukkit.getScheduler().runTask(plugin, guardedRunnable(runnable));
        return task::cancel;
    }

    public TaskHandle runAsync(Runnable runnable) {
        if (!plugin.isEnabled()) {
            return NOOP_TASK;
        }
        if (foliaLike) {
            Object scheduler = invokeStatic(Bukkit.class, "getAsyncScheduler");
            Object task = invoke(scheduler, "runNow", new Class<?>[]{Plugin.class, Consumer.class}, plugin, guardedConsumer(runnable));
            return new ReflectionTaskHandle(task);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, guardedRunnable(runnable));
        return task::cancel;
    }

    public TaskHandle runEntity(Player player, Runnable runnable) {
        if (!plugin.isEnabled()) {
            return NOOP_TASK;
        }
        if (foliaLike) {
            Object scheduler = invoke(player, "getScheduler", new Class<?>[]{});
            Object task = invoke(scheduler, "run", new Class<?>[]{Plugin.class, Consumer.class, Runnable.class}, plugin, guardedConsumer(runnable), null);
            return new ReflectionTaskHandle(task);
        }
        BukkitTask task = Bukkit.getScheduler().runTask(plugin, guardedRunnable(runnable));
        return task::cancel;
    }

    public TaskHandle runEntityDelayed(Player player, Runnable runnable, long delayTicks) {
        if (!plugin.isEnabled()) {
            return NOOP_TASK;
        }
        if (delayTicks <= 0L) {
            return runEntity(player, runnable);
        }
        if (foliaLike) {
            Object scheduler = invoke(player, "getScheduler", new Class<?>[]{});
            Object task = invoke(scheduler, "runDelayed", new Class<?>[]{Plugin.class, Consumer.class, Runnable.class, long.class},
                    plugin, guardedConsumer(runnable), null, delayTicks);
            return new ReflectionTaskHandle(task);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, guardedRunnable(runnable), delayTicks);
        return task::cancel;
    }

    public TaskHandle runEntityTimer(Player player, Runnable runnable, long initialDelayTicks, long periodTicks) {
        if (!plugin.isEnabled()) {
            return NOOP_TASK;
        }
        long safeInitialDelayTicks = Math.max(1L, initialDelayTicks);
        long safePeriodTicks = Math.max(1L, periodTicks);
        if (foliaLike) {
            Object scheduler = invoke(player, "getScheduler", new Class<?>[]{});
            Object task = invoke(scheduler, "runAtFixedRate",
                    new Class<?>[]{Plugin.class, Consumer.class, Runnable.class, long.class, long.class},
                    plugin, guardedConsumer(runnable), null, safeInitialDelayTicks, safePeriodTicks);
            return new ReflectionTaskHandle(task);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, guardedRunnable(runnable), safeInitialDelayTicks, safePeriodTicks);
        return task::cancel;
    }

    public TaskHandle runRegion(Location location, Runnable runnable) {
        if (!plugin.isEnabled()) {
            return NOOP_TASK;
        }
        if (foliaLike && location.getWorld() != null) {
            Object scheduler = invokeStatic(Bukkit.class, "getRegionScheduler");
            try {
                Object task = invoke(scheduler, "run", new Class<?>[]{Plugin.class, Location.class, Consumer.class}, plugin, location, guardedConsumer(runnable));
                return new ReflectionTaskHandle(task);
            } catch (IllegalStateException ignored) {
                int chunkX = location.getBlockX() >> 4;
                int chunkZ = location.getBlockZ() >> 4;
                Object task = invoke(scheduler, "run",
                        new Class<?>[]{Plugin.class, org.bukkit.World.class, int.class, int.class, Consumer.class},
                        plugin, location.getWorld(), chunkX, chunkZ, guardedConsumer(runnable));
                return new ReflectionTaskHandle(task);
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTask(plugin, guardedRunnable(runnable));
        return task::cancel;
    }

    public TaskHandle runAsyncDelayed(Runnable runnable, long delayTicks) {
        if (!plugin.isEnabled()) {
            return NOOP_TASK;
        }
        if (delayTicks <= 0L) {
            return runAsync(runnable);
        }
        if (foliaLike) {
            Object scheduler = invokeStatic(Bukkit.class, "getAsyncScheduler");
            Object task = invoke(scheduler, "runDelayed", new Class<?>[]{Plugin.class, Consumer.class, long.class, TimeUnit.class},
                    plugin, guardedConsumer(runnable), delayTicks * 50L, TimeUnit.MILLISECONDS);
            return new ReflectionTaskHandle(task);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, guardedRunnable(runnable), delayTicks);
        return task::cancel;
    }

    private Runnable guardedRunnable(Runnable runnable) {
        return () -> {
            if (!plugin.isEnabled()) {
                return;
            }
            runnable.run();
        };
    }

    private Consumer<Object> guardedConsumer(Runnable runnable) {
        return ignored -> {
            if (!plugin.isEnabled()) {
                return;
            }
            runnable.run();
        };
    }

    private static boolean hasMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static Object invokeStatic(Class<?> type, String methodName) {
        try {
            Method method = type.getMethod(methodName);
            return method.invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to call " + methodName, exception);
        }
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Missing scheduler method " + methodName, exception);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access scheduler method " + methodName, exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Scheduler method failed " + methodName, cause);
        }
    }

    @FunctionalInterface
    public interface TaskHandle {
        void cancel();
    }

    private static final class ReflectionTaskHandle implements TaskHandle {
        private final Object task;

        private ReflectionTaskHandle(Object task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            if (task == null) {
                return;
            }
            try {
                task.getClass().getMethod("cancel").invoke(task);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
