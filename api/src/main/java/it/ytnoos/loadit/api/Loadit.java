package it.ytnoos.loadit.api;

import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the Loadit library.
 * <p>
 * Create a platform-specific instance (e.g. {@code BukkitLoadit}), register any listeners,
 * then call {@link #init()} to start handling player joins. Call {@link #stop()} when the plugin is disabled.
 *
 * @param <D> the data type, representing a player's persistent/offline data
 * @param <S> the session type, representing the live state of an online player
 * @param <P> the platform player type
 */
public interface Loadit<D, S, P> {

    /**
     * Initializes Loadit by registering event listeners and loading data for any
     * already online players. Must be called once after creation.
     *
     * @throws IllegalStateException if already initialized
     */
    void init();

    /**
     * Shuts down Loadit by unregistering event listeners and stopping the loader executor.
     * Waits up to 30 seconds for pending tasks to complete.
     */
    void stop();

    /**
     * Registers a listener for player data lifecycle events.
     *
     * @param listener the listener to register
     */
    void addListener(LoaditLoadListener<D, S> listener);

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    void removeListener(LoaditLoadListener<D, S> listener);

    /**
     * Returns the logger used by this Loadit instance.
     *
     * @return the logger
     */
    Logger logger();

    void log(Level level, String message);

    void log(Level level, Throwable e, String message);


    /**
     * Returns the data registry for accessing cached data and sessions.
     */
    DataRegistry<D, S, P> registry();

    /**
     * Returns an unmodifiable view of the registered listeners.
     */
    List<LoaditLoadListener<D, S>> listeners();

    /**
     * Sets a custom function to generate kick messages when a load or setup operation fails.
     * The default message includes the result type and cause message if available.
     *
     * @param kickMessageProvider a function that takes a {@link LoadResult} and returns the kick message
     */
    void setKickMessage(Function<LoadResult, String> kickMessageProvider);

    /**
     * Enables or disables debug logging.
     *
     * @param debug true to enable debug logging
     */
    void debug(boolean debug);
}
