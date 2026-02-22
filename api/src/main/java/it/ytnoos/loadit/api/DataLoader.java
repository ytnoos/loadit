package it.ytnoos.loadit.api;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Defines how player data is loaded, created, and converted into sessions.
 * <p>
 * Implement this interface to provide the data source for your plugin.
 * All methods may be called from async threads except
 * {@link #startSession}, which is always called on the main thread.
 *
 * @param <D> the data type, representing a player's persistent/offline data
 * @param <S> the session type, representing the live state of an online player
 * @param <P> the platform player type
 */
public interface DataLoader<D, S, P> {

    /**
     * Loads existing data for the given player, or creates new data if none exists.
     * Called asynchronously during the player pre-login phase.
     *
     * @param uuid the player's unique id
     * @param name the player's name
     * @return the loaded or newly created data, or null if loading failed
     */
    @Nullable
    D getOrCreate(UUID uuid, String name);

    /**
     * Loads existing data for the given player by UUID, without creating new data.
     * Used for on-demand lookups outside the join flow.
     *
     * @param uuid the player's unique id
     * @return the loaded data, or null if not found
     */
    @Nullable
    D load(UUID uuid);

    /**
     * Loads existing data for the given player by name, without creating new data.
     * Used for on-demand lookups outside the join flow.
     *
     * @param name the player's name
     * @return the loaded data, or null if not found
     */
    @Nullable
    D load(String name);

    /**
     * Creates a session for an online player from their loaded data.
     * Called on the main thread during the player login phase, after data has been loaded.
     *
     * @param data   the player's previously loaded data
     * @param player the online player
     * @return the created session, or null if session creation failed (the player will be kicked)
     */
    @Nullable
    S startSession(D data, P player);

}
