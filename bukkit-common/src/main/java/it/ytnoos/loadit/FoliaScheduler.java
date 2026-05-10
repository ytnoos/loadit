package it.ytnoos.loadit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class FoliaScheduler implements LoaditScheduler {

    private static final String FOLIA_MARKER = "io.papermc.paper.threadedregions.RegionizedServer";
    private static final long MILLIS_PER_TICK = 50L;

    private final Plugin plugin;
    private final Object globalRegionScheduler;
    private final Object asyncScheduler;
    private final Method globalRunMethod;
    private final Method asyncRunDelayedMethod;
    private final Method scheduledTaskCancelMethod;

    FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
        try {
            this.globalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            this.asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);

            Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");
            this.globalRunMethod = globalRegionScheduler.getClass().getMethod("run", pluginClass, Consumer.class);
            this.asyncRunDelayedMethod = asyncScheduler.getClass().getMethod("runDelayed", pluginClass, Consumer.class, long.class, TimeUnit.class);

            Class<?> scheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            this.scheduledTaskCancelMethod = scheduledTaskClass.getMethod("cancel");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Folia scheduler API not available", e);
        }
    }

    static boolean isAvailable() {
        try {
            Class.forName(FOLIA_MARKER);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override
    public void runOnGlobalRegion(Runnable task) {
        Consumer<Object> wrapped = ignored -> task.run();
        try {
            globalRunMethod.invoke(globalRegionScheduler, plugin, wrapped);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to schedule task on Folia global region scheduler", e);
        }
    }

    @Override
    public LoaditScheduledTask runAsyncDelayed(Runnable task, long delayTicks) {
        Consumer<Object> wrapped = ignored -> task.run();
        long delayMillis = Math.max(1L, delayTicks * MILLIS_PER_TICK);
        try {
            Object scheduledTask = asyncRunDelayedMethod.invoke(asyncScheduler, plugin, wrapped, delayMillis, TimeUnit.MILLISECONDS);
            return () -> {
                try {
                    scheduledTaskCancelMethod.invoke(scheduledTask);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Failed to cancel Folia scheduled task", e);
                }
            };
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to schedule async delayed task on Folia", e);
        }
    }
}
