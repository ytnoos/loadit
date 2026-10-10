package it.ytnoos.loadit;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

final class PaperConnectionCloseListener<D, S> implements Listener {

    private final LoaditScheduler scheduler;
    private final ConnectionCloseCallback callback;
    // Quits whose close is still pending: the quit already removed the data, which a newer login may now own.
    private final Multiset<UUID> pendingQuitCloses = ConcurrentHashMultiset.create();

    PaperConnectionCloseListener(LoaditScheduler scheduler, ConnectionCloseCallback callback) {
        this.scheduler = scheduler;
        this.callback = callback;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        pendingQuitCloses.add(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConnectionClose(PlayerConnectionCloseEvent event) {
        UUID uuid = event.getPlayerUniqueId();
        // Paper fires a joined player's close after its quit.
        if (pendingQuitCloses.remove(uuid)) return;

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
