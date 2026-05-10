package it.ytnoos.loadit;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

final class PaperConnectionCloseListener<D, S> implements Listener {

    private final LoaditScheduler scheduler;
    private final ConnectionCloseCallback callback;

    PaperConnectionCloseListener(LoaditScheduler scheduler, ConnectionCloseCallback callback) {
        this.scheduler = scheduler;
        this.callback = callback;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConnectionClose(PlayerConnectionCloseEvent event) {
        UUID uuid = event.getPlayerUniqueId();
        String name = event.getPlayerName();
        Runnable cleanup = () -> callback.cleanup(uuid, name);

        if (event.isAsynchronous()) {
            scheduler.runOnGlobalRegion(cleanup);
        } else {
            cleanup.run();
        }
    }

    interface ConnectionCloseCallback {
        void cleanup(UUID uuid, String name);
    }
}
