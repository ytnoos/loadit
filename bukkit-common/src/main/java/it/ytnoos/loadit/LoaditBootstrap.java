package it.ytnoos.loadit;

import it.ytnoos.loadit.api.DataLoader;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class LoaditBootstrap {

    private LoaditBootstrap() {
    }

    static <D, S> BukkitLoadit<D, S> create(Plugin plugin, DataLoader<D, S, Player> loader, int parallelism, LoaditLifecycleStrategyFactory lifecycleStrategyFactory) {
        if (parallelism < 1) throw new IllegalArgumentException("parallelism must be at least 1");

        return new LoaditImpl<>(plugin, loader, parallelism, lifecycleStrategyFactory);
    }
}
