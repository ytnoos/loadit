package it.ytnoos.loadit;

import org.bukkit.event.Listener;

import java.util.List;

public interface LoaditLifecycleStrategy {

    List<Listener> listeners();

    default boolean usesTimeoutCleanup() {
        return false;
    }
}
