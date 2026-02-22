package it.ytnoos.loadit.api;

import java.util.UUID;

/**
 * Listener for player data lifecycle events.
 * <p>
 * Methods that return a boolean can abort the current operation by returning false.
 * Exceptions thrown by listeners are caught and logged without affecting other listeners.
 *
 * @param <D> the data type
 * @param <S> the session type
 */
public interface LoaditLoadListener<D, S> {

    /**
     * Called before data is loaded for a player joining the server.
     * Runs on an async thread.
     *
     * @param uuid the player's unique id
     * @param name the player's name
     * @return true to continue loading, false to abort (the player will be kicked)
     */
    default boolean onPreLoad(UUID uuid, String name) {
        return true;
    }

    /**
     * Called after data has been successfully loaded and cached.
     * Runs on an async thread.
     *
     * @param data the loaded data
     * @return true to continue, false to abort (the player will be kicked)
     */
    default boolean onPostLoad(D data) {
        return true;
    }

    /**
     * Called when a player's data is removed from the registry (on quit or login failure).
     * Exceptions thrown here are logged but do not affect other listeners.
     *
     * @param data the data being unloaded
     */
    default void onUnload(D data) {
    }

    /**
     * Called after a session has been created for an online player.
     * Runs on the main thread.
     *
     * @param data    the player's data
     * @param session the created session
     * @return true to continue, false to abort (the session will be removed and the player kicked)
     */
    default boolean onSessionStart(D data, S session) {
        return true;
    }
}
