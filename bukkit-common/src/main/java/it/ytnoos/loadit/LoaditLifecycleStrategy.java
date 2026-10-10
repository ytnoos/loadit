package it.ytnoos.loadit;

import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

interface LoaditLifecycleStrategy {

    List<Listener> listeners();

    default boolean usesTimeoutCleanup() {
        return false;
    }

    // Whether a login that never joined can be ended, through loginConnection and endLogin.
    default boolean endsLogins() {
        return false;
    }

    // The connection of a pre-login.
    default @Nullable Object loginConnection(AsyncPlayerPreLoginEvent event) {
        return null;
    }

    // Whether a connection returned by loginConnection is still open.
    default boolean isConnected(Object connection) {
        return true;
    }

    // Closes a validated login connection, so that its close event removes its data.
    default void endLogin(Object connection) {
    }
}
