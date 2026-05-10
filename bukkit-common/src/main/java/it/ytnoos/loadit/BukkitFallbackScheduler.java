package it.ytnoos.loadit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

final class BukkitFallbackScheduler implements LoaditScheduler {

    private final Plugin plugin;

    BukkitFallbackScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runOnGlobalRegion(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public LoaditScheduledTask runAsyncDelayed(Runnable task, long delayTicks) {
        BukkitTask handle = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        return handle::cancel;
    }
}
