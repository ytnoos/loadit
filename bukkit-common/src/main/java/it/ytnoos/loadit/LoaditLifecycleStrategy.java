package it.ytnoos.loadit;

import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

interface LoaditLifecycleStrategy {

    List<Listener> listeners();

    default boolean usesTimeoutCleanup() {
        return false;
    }

    // The platform handle of an allowed pre-login, passed back to endLogin.
    default @Nullable Object loginConnection(AsyncPlayerPreLoginEvent event) {
        return null;
    }

    // Ends an allowed login that never joined, so that its own close or cleanup removes its data.
    default void endLogin(UUID uuid, long connectionToken, @Nullable Object connection) {
    }
}
