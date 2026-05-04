package it.ytnoos.loadit;

import it.ytnoos.loadit.api.LoadResult;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

class AccessListener<D, S> implements Listener {

    private final LoaditImpl<D, S> loadit;
    private final LoaditDataRegistry<D, S> registry;
    private final LoaditLoadCoordinator<D, S> coordinator;
    // Bukkit calls every priority handler for the same AsyncPlayerPreLoginEvent on the firing thread.
    private final ThreadLocal<@Nullable LoadedPreLogin> loadedPreLogin = new ThreadLocal<>();

    AccessListener(LoaditImpl<D, S> loadit, LoaditDataRegistry<D, S> registry, LoaditLoadCoordinator<D, S> coordinator) {
        this.loadit = loadit;
        this.registry = registry;
        this.coordinator = coordinator;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void firstAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();

        //We don't need to load the player since something has disallowed the connection.
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        loadit.debug("Loading data for " + uuid + " (" + name + ")");
        long connectionToken = coordinator.createConnectionToken();
        LoadResult result = coordinator.loadData(uuid, name, connectionToken);

        if (!result.isLoaded()) {
            loadit.debug("Cannot load data for " + uuid + " (" + name + ") (" + result + ")");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, loadit.kickMessage(result, uuid, name));
            return;
        }

        loadedPreLogin.set(new LoadedPreLogin(uuid, connectionToken));
        coordinator.scheduleCleanup(uuid, name, connectionToken);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void highAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        LoadedPreLogin loaded = loadedPreLogin.get();

        // Data was loaded for the original UUID; reject identity changes after that.
        if (loaded != null && !loaded.uuid().equals(uuid)) {
            loadit.debug(loaded.uuid() + " (" + event.getName() + ") changed UUID during AsyncLogin to " + uuid);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, loadit.kickMessage(LoadResult.ERROR, loaded.uuid(), event.getName()));
            return;
        }

        //It means someone disallowed firstAsync (so we didn't load anything) and then allowed the login again
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED && !registry.hasData(uuid)) {
            loadit.debug(uuid + " (" + event.getName() + ") has been re-allowed in AsyncLogin but data is not loaded!");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, loadit.kickMessage(LoadResult.PRE_LOGIN_REALLOWED, uuid, event.getName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void lastAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        LoadedPreLogin loaded = loadedPreLogin.get();
        loadedPreLogin.remove();

        //Player won't join the server, we clear his offline data
        if (loaded != null && event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            loadit.debug("Removing data for " + loaded.uuid() + " (" + event.getName() + ") since he won't join the server during AsyncLogin");
            coordinator.removeConnectionData(loaded.uuid(), loaded.connectionToken(), false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void quit(PlayerQuitEvent event) {
        connectionClosed(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    void connectionClosed(UUID uuid, String name) {
        loadit.debug("Removing data for " + uuid + " (" + name + ") since the connection was closed");
        coordinator.removeData(uuid, true);
    }

    private record LoadedPreLogin(UUID uuid, long connectionToken) {
    }
}
