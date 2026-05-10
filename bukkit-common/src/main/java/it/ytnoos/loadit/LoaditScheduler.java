package it.ytnoos.loadit;

import org.bukkit.plugin.Plugin;

/**
 * Abstraction over the platform scheduler.
 * <p>
 * Two implementations are provided:
 * <ul>
 *     <li>{@link FoliaScheduler} when running on Folia</li>
 *     <li>{@link BukkitFallbackScheduler} on Bukkit/Spigot/Paper classic servers</li>
 * </ul>
 * Folia is detected by reflection so loadit does not need a build-time dependency on it.
 */
interface LoaditScheduler {

    /**
     * Executes the given task on the global region (Folia) or on the main server thread (classic).
     * <p>
     * Used for operations that must run in a single-threaded context relative to global server state
     * but do not target a specific entity or world region.
     */
    void runOnGlobalRegion(Runnable task);

    /**
     * Schedules the task to run asynchronously after the given delay (in ticks, 20 ticks per second).
     * The task does not need access to entities, worlds, or main-thread state.
     */
    LoaditScheduledTask runAsyncDelayed(Runnable task, long delayTicks);

    static LoaditScheduler create(Plugin plugin) {
        if (FoliaScheduler.isAvailable()) return new FoliaScheduler(plugin);
        return new BukkitFallbackScheduler(plugin);
    }
}
