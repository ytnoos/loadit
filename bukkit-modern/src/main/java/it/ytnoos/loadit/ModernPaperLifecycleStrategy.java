package it.ytnoos.loadit;

import io.papermc.paper.connection.PlayerConnection;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

final class ModernPaperLifecycleStrategy<D, S> implements LoaditLifecycleStrategy {

    // AsyncPlayerPreLoginEvent#getConnection is missing on older Paper versions.
    private static final boolean LOGIN_CONNECTIONS = hasLoginConnections();

    private final PaperConnectionCloseListener<D, S> connectionCloseListener;
    private final ModernJoinListener<D, S> joinListener;

    ModernPaperLifecycleStrategy(LoaditImpl<D, S> loadit,
                                 AccessListener<D, S> accessListener,
                                 LoaditDataRegistry<D, S> registry,
                                 LoaditLoadCoordinator<D, S> coordinator,
                                 LoaditScheduler scheduler) {
        this.connectionCloseListener = new PaperConnectionCloseListener<>(scheduler, accessListener::connectionClosed);
        this.joinListener = new ModernJoinListener<>(loadit, registry, coordinator);
    }

    private static boolean hasLoginConnections() {
        try {
            AsyncPlayerPreLoginEvent.class.getMethod("getConnection");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Override
    public List<Listener> listeners() {
        return List.of(connectionCloseListener, joinListener);
    }

    @Override
    public @Nullable Object loginConnection(AsyncPlayerPreLoginEvent event) {
        return LOGIN_CONNECTIONS ? event.getConnection() : null;
    }

    // Like a vanilla duplicate login: the older connection is closed and its close event removes its data.
    @Override
    public void endLogin(UUID uuid, long connectionToken, @Nullable Object connection) {
        if (connection instanceof PlayerConnection playerConnection && playerConnection.isConnected()) {
            playerConnection.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
        }
    }
}
