package it.ytnoos.loadit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class Loadit<T extends UserData> {

    private final Plugin plugin;
    private final Settings settings;
    private final UserLoader<T> loader;
    private final LoaditDataContainer<T> container;

    public Loadit(Plugin plugin, UserLoader<T> loader) {
        this(plugin, loader, new SettingsBuilder());
    }

    public Loadit(Plugin plugin, UserLoader<T> loader, SettingsBuilder builder) {
        this.plugin = plugin;
        this.loader = loader;
        this.settings = builder.createSettings();

        container = new LoaditDataContainer<>(this, loader);
    }

    public void init() {
        plugin.getServer().getPluginManager().registerEvents(new AccessListener(loader, container), plugin);

        if (!settings.isLoadOnlines()) return;

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

    public void warn(String message) {
        plugin.getLogger().info(() -> "[Loadit] " + message);
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
}
