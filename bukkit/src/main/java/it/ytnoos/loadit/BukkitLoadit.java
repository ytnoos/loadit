package it.ytnoos.loadit;

import it.ytnoos.loadit.api.DataLoader;
import it.ytnoos.loadit.api.Loadit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Bukkit-specific extension of {@link Loadit} that binds the platform player type to {@link Player}.
 * <p>
 * Create an instance with {@link #createInstance}, register any listeners, then call {@link #init()}
 * to start handling player joins. Call {@link #stop()} when the plugin is disabled.
 *
 * <pre>{@code
 * BukkitLoadit<MyData, MySession> loadit = BukkitLoadit.createInstance(plugin, myDataLoader);
 * loadit.init();
 * // ...
 * loadit.stop();
 * }</pre>
 *
 * @param <D> the data type, representing a player's persistent/offline data
 * @param <S> the session type, representing the live state of an online player
 */
public interface BukkitLoadit<D, S> extends Loadit<D, S, Player> {

    /**
     * Creates a new BukkitLoadit instance with a single-threaded loader executor.
     *
     * @param plugin the owning plugin
     * @param loader the data loader implementation
     * @return a new BukkitLoadit instance
     */
    static <D, S> BukkitLoadit<D, S> createInstance(Plugin plugin, DataLoader<D, S, Player> loader) {
        return createInstance(plugin, loader, 1);
    }

    /**
     * Creates a new BukkitLoadit instance with the given loader executor parallelism.
     *
     * @param plugin      the owning plugin
     * @param loader      the data loader implementation
     * @param parallelism the number of threads for async data loading
     * @return a new BukkitLoadit instance
     * @throws IllegalArgumentException if parallelism is less than 1
     */
    static <D, S> BukkitLoadit<D, S> createInstance(Plugin plugin, DataLoader<D, S, Player> loader, int parallelism) {
        if (parallelism < 1) throw new IllegalArgumentException("parallelism must be at least 1");

        return new LoaditImpl<>(plugin, loader, parallelism);
    }

    /**
     * Returns the owning plugin.
     */
    Plugin plugin();
}
