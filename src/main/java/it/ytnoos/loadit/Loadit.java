package it.ytnoos.loadit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class Loadit<T extends UserData, V extends PlayerData> {

    private final Plugin plugin;
    private final LoaditLoader<T, V> loader;
    private final DataContainer<T, V> container;

    public Loadit(Plugin plugin, LoaditLoader<T, V> loader) {
        this(plugin, loader, 1);
    }

    public Loadit(Plugin plugin, LoaditLoader<T, V> loader, int poolSize) {
        this.plugin = plugin;
        this.loader = loader;

        container = new DataContainer<>(this, loader, poolSize);
    }

    public void init() {
        init(true);
    }

    public void init(boolean loadOnlines) {
        plugin.getServer().getPluginManager().registerEvents(new AccessListener(loader, container), plugin);

        if (!loadOnlines) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            LoadResult result = container.insertData(player.getUniqueId(), player.getName());
            if (result == LoadResult.LOADED) {
                result = container.insertPlayerData(player);
                if (result == LoadResult.LOADED) continue;
            }

            player.kickPlayer(loader.getErrorMessage(result, player.getUniqueId(), player.getName()));
        }
    }

    public void stop() {
        container.stop();
    }

    public void log(String message) {
        plugin.getLogger().info(() -> "[Loadit] " + message);
    }

    public void logError(Throwable t, String message) {
        plugin.getLogger().log(Level.SEVERE, t, () -> "[Loadit] " + message);
    }

    public V getPlayerData(Player player) {
        return container.getPlayerData(player);
    }

    public Optional<V> getPlayerData(UUID uuid) {
        return container.getPlayerData(uuid);
    }

    public Collection<V> getPlayersData() {
        return container.getPlayersData();
    }
}
