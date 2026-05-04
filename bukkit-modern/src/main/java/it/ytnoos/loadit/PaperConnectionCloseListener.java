package it.ytnoos.loadit;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

final class PaperConnectionCloseListener<D, S> implements Listener {

    private final Plugin plugin;
    private final ConnectionCloseCallback callback;

    PaperConnectionCloseListener(Plugin plugin, ConnectionCloseCallback callback) {
        this.plugin = plugin;
        this.callback = callback;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConnectionClose(PlayerConnectionCloseEvent event) {
        Runnable cleanup = () -> callback.cleanup(event.getPlayerUniqueId(), event.getPlayerName());

        if (event.isAsynchronous()) {
            Bukkit.getScheduler().runTask(plugin, cleanup);
        } else {
            cleanup.run();
        }
    }

    interface ConnectionCloseCallback {
        void cleanup(UUID uuid, String name);
    }
}
