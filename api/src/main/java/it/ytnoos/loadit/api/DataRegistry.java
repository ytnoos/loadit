package it.ytnoos.loadit.api;

import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Provides access to cached player data and sessions.
 * <p>
 * Data is loaded automatically during the player join flow and removed on quit.
 * Use this registry to query cached data, look up sessions, or perform
 * asynchronous on-demand loads.
 *
 * @param <D> the data type, representing a player's persistent/offline data
 * @param <S> the session type, representing the live state of an online player
 * @param <P> the platform player type
 */
public interface DataRegistry<D, S, P> {

    /**
     * Returns the executor used for asynchronous data loading operations.
     *
     * @return the executor service
     */
    ExecutorService executor();

    /**
     * Returns the cached data for the given player, or null if not loaded.
     *
     * @param uuid the player's unique id
     * @return the cached data, or null
     */
    @Nullable
    D data(UUID uuid);

    /**
     * Returns the session for the given online player, or null if the player
     * has no active session.
     *
     * @param player the online player
     * @return the session, or null
     */
    @Nullable
    S session(P player);

    /**
     * Returns the session for the given online player, throwing if no session exists.
     *
     * @param player the online player
     * @return the session, never null
     * @throws NullPointerException if the player has no active session
     */
    S requireSession(P player);

    /**
     * Applies the consumer to the player's cached data if it is present.
     *
     * @param uuid     the player's unique id
     * @param consumer the action to perform on the data
     */
    void ifPresent(UUID uuid, Consumer<D> consumer);

    /**
     * Applies the consumer to the player's cached data if it is present.
     *
     * @param player   the player
     * @param consumer the action to perform on the data
     */
    void ifPresent(P player, Consumer<D> consumer);

    /**
     * Asynchronously loads data for the given player by UUID.
     * This does not cache the result in the registry.
     *
     * @param uuid the player's unique id
     * @return a future containing the loaded data, or null if not found
     */
    CompletableFuture<D> load(UUID uuid);

    /**
     * Asynchronously loads data for the given player by name.
     * This does not cache the result in the registry.
     *
     * @param name the player's name
     * @return a future containing the loaded data, or null if not found
     */
    CompletableFuture<D> load(String name);

    /**
     * Iterates over all cached data entries.
     *
     * @param consumer the action to perform on each data entry
     */
    void forEach(Consumer<D> consumer);

    /**
     * Iterates over all active sessions.
     *
     * @param consumer the action to perform on each session
     */
    void forEachSession(Consumer<S> consumer);

    /**
     * Iterates over all cached data entries with a consumer that may throw a checked exception.
     * The exception is propagated to the caller.
     *
     * @param consumer the action to perform on each data entry
     * @param <E>      the checked exception type
     * @throws E if the consumer throws
     */
    <E extends Exception> void forEachThrowing(ThrowableConsumer<D, E> consumer) throws E;

    /**
     * Iterates over all active sessions with a consumer that may throw a checked exception.
     * The exception is propagated to the caller.
     *
     * @param consumer the action to perform on each session
     * @param <E>      the checked exception type
     * @throws E if the consumer throws
     */
    <E extends Exception> void forEachSessionThrowing(ThrowableConsumer<S, E> consumer) throws E;
}
