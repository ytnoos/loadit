package it.ytnoos.loadit;

import it.ytnoos.loadit.api.DataLoader;
import it.ytnoos.loadit.api.DataRegistry;
import it.ytnoos.loadit.api.LoaditLoadListener;
import org.bukkit.plugin.Plugin;

import java.util.List;

public interface Loadit<D, S> {
    static <D, S> Loadit<D, S> createInstance(Plugin plugin, DataLoader<D, S> loader) {
        return createInstance(plugin, loader, 1);
    }

    static <D, S>  Loadit<D, S> createInstance(Plugin plugin, DataLoader<D, S> loader, int parallelism) {
        if (parallelism < 1) throw new IllegalArgumentException("parallelism must be at least 1");

        return new LoaditImpl<>(plugin, loader, parallelism);
    }

    void init();

    void stop();

    void addListener(LoaditLoadListener<D, S> listener);

    void removeListener(LoaditLoadListener<D, S> listener);

    void logError(Throwable t, String message);

    Plugin plugin();

    DataRegistry<D, S> registry();

    List<LoaditLoadListener<D, S>> listeners();

    void debug(boolean debug);
}
