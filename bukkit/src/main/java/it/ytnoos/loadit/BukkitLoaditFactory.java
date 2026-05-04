package it.ytnoos.loadit;

import it.ytnoos.loadit.api.DataLoader;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Factory entrypoint for Bukkit loadit instances.
 */
public final class BukkitLoaditFactory {

    private static final String MODERN_CONNECTION_CLOSE_EVENT = "com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent";

    private BukkitLoaditFactory() {
    }

    /**
     * Creates a Bukkit loadit instance with single-threaded async loading.
     *
     * @param plugin the owning plugin
     * @param loader the data loader
     * @param <D>    the persistent data type
     * @param <S>    the live session type
     * @return a configured BukkitLoadit
     */
    public static <D, S> BukkitLoadit<D, S> create(Plugin plugin, DataLoader<D, S, Player> loader) {
        return create(plugin, loader, 1);
    }

    /**
     * Creates a Bukkit loadit instance.
     *
     * @param plugin      the owning plugin
     * @param loader      the data loader
     * @param parallelism the async loader parallelism
     * @param <D>         the persistent data type
     * @param <S>         the live session type
     * @return a configured BukkitLoadit
     */
    public static <D, S> BukkitLoadit<D, S> create(Plugin plugin, DataLoader<D, S, Player> loader, int parallelism) {
        return LoaditBootstrap.create(plugin, loader, parallelism, isModernPaperAvailable() ? new ModernPaperLifecycleStrategyFactory() : new LegacyLifecycleStrategyFactory());
    }

    private static boolean isModernPaperAvailable() {
        try {
            Class.forName(MODERN_CONNECTION_CLOSE_EVENT);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
