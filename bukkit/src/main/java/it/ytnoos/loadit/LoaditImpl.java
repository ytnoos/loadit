package it.ytnoos.loadit;

import it.ytnoos.loadit.api.DataLoader;
import it.ytnoos.loadit.api.DataRegistry;
import it.ytnoos.loadit.api.LoadResult;
import it.ytnoos.loadit.api.LoaditLoadListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

@NullMarked
public class LoaditImpl<D, S> implements Loadit<D, S> {

    private final Plugin plugin;
    private final DataLoader<D, S> loader;
    private final LoaditDataRegistry<D, S> container;
    private final Collection<LoaditLoadListener<D, S>> listeners = new ArrayList<>();
    private boolean debug = false;

    protected LoaditImpl(Plugin plugin, DataLoader<D, S> loader, int parallelism) {
        this.plugin = plugin;
        this.loader = loader;

        container = new LoaditDataRegistry<>(this, loader, parallelism);
    }

    @Override
    public void init() {
        plugin.getServer().getPluginManager().registerEvents(new AccessListener(this, loader, container), plugin);

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
    public void addListener(LoaditLoadListener<D, S> listener) {
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
    public DataRegistry<D, S> getContainer() {
        return container;
    }

    @Override
    public Collection<LoaditLoadListener<D, S>> getListeners() {
        return listeners;
    }

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void debug(String message) {
        if (debug) plugin.getLogger().info(message);
    }
}
