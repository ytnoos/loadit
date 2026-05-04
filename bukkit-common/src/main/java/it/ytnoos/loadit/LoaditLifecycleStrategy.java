package it.ytnoos.loadit;

import org.bukkit.event.Listener;

public interface LoaditLifecycleStrategy {

    Listener primaryListener();

    default Listener secondaryListener() {
        return null;
    }

    default boolean usesTimeoutCleanup() {
        return false;
    }
}
