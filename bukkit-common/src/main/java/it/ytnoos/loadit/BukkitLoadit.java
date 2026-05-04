package it.ytnoos.loadit;

import it.ytnoos.loadit.api.Loadit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Bukkit-specific extension of {@link Loadit} that binds the platform player type to {@link Player}.
 * <p>
 * Create an instance through {@code BukkitLoaditFactory}, register any listeners, then call {@link #init()}
 * to start handling player joins. Call {@link #stop()} when the plugin is disabled.
 *
 * <pre>{@code
 * BukkitLoadit<MyData, MySession> loadit = BukkitLoaditFactory.create(plugin, myDataLoader);
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
     * Returns the owning plugin.
     */
    Plugin plugin();
}
