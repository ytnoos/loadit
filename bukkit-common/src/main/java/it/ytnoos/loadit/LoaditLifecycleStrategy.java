package it.ytnoos.loadit;

import org.bukkit.event.Listener;

import java.util.List;

interface LoaditLifecycleStrategy {

    List<Listener> listeners();

    default boolean usesTimeoutCleanup() {
        return false;
    }
}
