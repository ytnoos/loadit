package it.ytnoos.loadit;

import org.bukkit.event.Listener;

import java.util.List;

interface LoaditLifecycleStrategy {

    List<Listener> listeners();

    default boolean usesTimeoutCleanup() {
        return false;
    }

    // whether the strategy reports closed connections to AccessListener#connectionClosed
    default boolean reportsConnectionClose() {
        return false;
    }
}
