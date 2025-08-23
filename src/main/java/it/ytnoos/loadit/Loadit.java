package it.ytnoos.loadit;

import it.ytnoos.loadit.api.*;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

@NullMarked
public interface Loadit<D, S> {
    static <D, S> Loadit<D, S> createInstance(Plugin plugin, DataLoader<D, S> loader) {
        return createInstance(plugin, loader, 1);
    }

    static <D, S>  Loadit<D, S> createInstance(Plugin plugin, DataLoader<D, S> loader, int parallelism) {
        return new BaseLoadit<>(plugin, loader, parallelism);
    }

    void init();

    void stop();

    void addListener(LoaditLoadListener<D, S> listener);

    void logError(Throwable t, String message);

    Plugin getPlugin();

    DataRegistry<D, S> getContainer();

    Collection<LoaditLoadListener<D, S>> getListeners();

    void setDebug(boolean debug);
}
