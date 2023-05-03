package it.ytnoos.loadit;

import it.ytnoos.loadit.api.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

public class BaseLoadit<T extends UserData> implements Loadit<T> {

    private final Plugin plugin;
    private final DataLoader<T> loader;
    private final LoaditDataContainer<T> container;
    private final Collection<LoaditLoadListener<T>> listeners = new ArrayList<>();

    protected BaseLoadit(Plugin plugin, DataLoader<T> loader, int parallelism) {
        this.plugin = plugin;
        this.loader = loader;

        container = new LoaditDataContainer<>(this, loader, parallelism);
    }

    @Override
    public void init() {
        plugin.getServer().getPluginManager().registerEvents(new AccessListener(loader, container), plugin);

        for (Player player : Bukkit.getOnlinePlayers()) {
            LoadResult result = container.loadData(player.getUniqueId(), player.getName());
            if (result == LoadResult.LOADED) {
                result = container.setupPlayer(player);
                if (result == LoadResult.LOADED) continue;
            }

            player.kickPlayer(loader.getErrorMessage(result, player.getUniqueId(), player.getName()));
        }
    }

    @Override
    public void stop() {
        container.stop();
    }

    @Override
    public void addListener(LoaditLoadListener<T> listener) {
        listeners.add(listener);
    }

    @Override
    public void logError(Throwable t, String message) {
        plugin.getLogger().log(Level.SEVERE, t, () -> "[Loadit] " + message);
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public DataContainer<T> getContainer() {
        return container;
    }

    @Override
    public Collection<LoaditLoadListener<T>> getListeners() {
        return listeners;
    }
}
