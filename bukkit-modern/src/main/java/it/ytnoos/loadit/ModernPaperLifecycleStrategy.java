package it.ytnoos.loadit;

import io.papermc.paper.connection.PlayerConnection;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class ModernPaperLifecycleStrategy<D, S> implements LoaditLifecycleStrategy {

    // Login connections are exposed by AsyncPlayerPreLoginEvent and PlayerConnectionValidateLoginEvent since Paper 1.21.7.
    private static final boolean LOGIN_CONNECTIONS = hasLoginConnections();

    private final PaperConnectionCloseListener<D, S> connectionCloseListener;
    private final ModernJoinListener<D, S> joinListener;
    private final PaperLoginValidateListener<D, S> validateListener;

    ModernPaperLifecycleStrategy(LoaditImpl<D, S> loadit,
                                 AccessListener<D, S> accessListener,
                                 LoaditDataRegistry<D, S> registry,
                                 LoaditLoadCoordinator<D, S> coordinator,
                                 LoaditScheduler scheduler) {
        this.connectionCloseListener = new PaperConnectionCloseListener<>(scheduler, accessListener::connectionClosed);
        this.joinListener = new ModernJoinListener<>(loadit, registry, coordinator);
        this.validateListener = new PaperLoginValidateListener<>(coordinator);
    }

    private static boolean hasLoginConnections() {
        try {
            AsyncPlayerPreLoginEvent.class.getMethod("getConnection");
            Class.forName("io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent");
            return true;
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public List<Listener> listeners() {
        List<Listener> listeners = new ArrayList<>(List.of(connectionCloseListener, joinListener));
        if (LOGIN_CONNECTIONS) listeners.add(validateListener);
        return listeners;
    }

    @Override
    public boolean endsLogins() {
        return LOGIN_CONNECTIONS;
    }

    @Override
    public @Nullable Object loginConnection(AsyncPlayerPreLoginEvent event) {
        return LOGIN_CONNECTIONS ? event.getConnection() : null;
    }

    // Like a vanilla duplicate login: the older connection is closed and its close event removes its data.
    @Override
    public void endLogin(Object connection) {
        if (connection instanceof PlayerConnection playerConnection && playerConnection.isConnected()) {
            playerConnection.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
        }
    }
}
