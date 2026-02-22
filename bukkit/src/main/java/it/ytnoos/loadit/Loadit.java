package it.ytnoos.loadit;

import it.ytnoos.loadit.api.DataLoader;
import it.ytnoos.loadit.api.DataRegistry;
import it.ytnoos.loadit.api.LoaditLoadListener;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Main entry point for the Loadit library.
 * <p>
 * Create an instance with {@link #createInstance}, register any listeners, then call {@link #init()}
 * to start handling player joins. Call {@link #stop()} when the plugin is disabled.
 *
 * <pre>{@code
 * Loadit<MyData, MySession> loadit = Loadit.createInstance(plugin, myDataLoader);
 * loadit.init();
 * // ...
 * loadit.stop();
 * }</pre>
 *
 * @param <D> the data type, representing a player's persistent/offline data
 * @param <S> the session type, representing the live state of an online player
 */
public interface Loadit<D, S> {

    /**
     * Creates a new Loadit instance with a single-threaded loader executor.
     *
     * @param plugin the owning plugin
     * @param loader the data loader implementation
     * @return a new Loadit instance
     */
    static <D, S> Loadit<D, S> createInstance(Plugin plugin, DataLoader<D, S> loader) {
        return createInstance(plugin, loader, 1);
    }

    /**
     * Creates a new Loadit instance with the given loader executor parallelism.
     *
     * @param plugin      the owning plugin
     * @param loader      the data loader implementation
     * @param parallelism the number of threads for async data loading
     * @return a new Loadit instance
     * @throws IllegalArgumentException if parallelism is less than 1
     */
    static <D, S> Loadit<D, S> createInstance(Plugin plugin, DataLoader<D, S> loader, int parallelism) {
        if (parallelism < 1) throw new IllegalArgumentException("parallelism must be at least 1");

        return new LoaditImpl<>(plugin, loader, parallelism);
    }

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
     * Logs an error with the plugin's logger.
     *
     * @param t       the throwable
     * @param message the error message
     */
    void logError(Throwable t, String message);

    /**
     * Returns the owning plugin.
     */
    Plugin plugin();

    /**
     * Returns the data registry for accessing cached data and sessions.
     */
    DataRegistry<D, S> registry();

    /**
     * Returns an unmodifiable view of the registered listeners.
     */
    List<LoaditLoadListener<D, S>> listeners();

    /**
     * Enables or disables debug logging.
     *
     * @param debug true to enable debug logging
     */
    void debug(boolean debug);
}
