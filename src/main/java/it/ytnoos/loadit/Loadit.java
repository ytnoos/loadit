package it.ytnoos.loadit;

import it.ytnoos.loadit.api.DataContainer;
import it.ytnoos.loadit.api.DataLoader;
import it.ytnoos.loadit.api.LoaditLoadListener;
import it.ytnoos.loadit.api.UserData;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

public interface Loadit<T extends UserData> {
    static <T extends UserData> Loadit<T> createInstance(Plugin plugin, DataLoader<T> loader) {
        return createInstance(plugin, loader, 1);
    }

    static <T extends UserData> Loadit<T> createInstance(Plugin plugin, DataLoader<T> loader, int parallelism) {
        return new BaseLoadit<>(plugin, loader, parallelism);
    }

    void init();

    void stop();

    void addListener(LoaditLoadListener<T> listener);

    void logError(Throwable t, String message);

    Plugin getPlugin();

    DataContainer<T> getContainer();

    Collection<LoaditLoadListener<T>> getListeners();

    void setDebug(boolean debug);
}
