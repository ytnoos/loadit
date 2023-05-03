package it.ytnoos.loadit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

public class Loadit<T extends UserData> {

    private final Plugin plugin;
    private final Settings settings;
    private final DataLoader<T> loader;
    private final LoaditDataContainer<T> container;
    private final Collection<LoaditLoadListener<T>> listeners = new ArrayList<>();

    public Loadit(Plugin plugin, DataLoader<T> loader) {
        this(plugin, loader, new SettingsBuilder());
    }

    public Loadit(Plugin plugin, DataLoader<T> loader, SettingsBuilder builder) {
        this.plugin = plugin;
        this.loader = loader;
        this.settings = builder.createSettings();

        container = new LoaditDataContainer<>(this, loader);
    }

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

    public void stop() {
        container.stop();
    }

    public void addListener(LoaditLoadListener<T> listener) {
        listeners.add(listener);
    }

    public void logError(Throwable t, String message) {
        plugin.getLogger().log(Level.SEVERE, t, () -> "[Loadit] " + message);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Settings getSettings() {
        return settings;
    }

    public DataContainer<T> getContainer() {
        return container;
    }

    public Collection<LoaditLoadListener<T>> getListeners() {
        return listeners;
    }
}
