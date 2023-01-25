package it.ytnoos.loadit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class AccessListener implements Listener {

    private final LoaditLoader<?, ?> loader;
    private final DataContainer<?, ?> container;

    public AccessListener(LoaditLoader<?, ?> loader, DataContainer<?, ?> container) {
        this.loader = loader;
        this.container = container;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void firstAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();

        //We don't need to load the player since something has disallowed the connection.
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        LoadResult result = container.insertData(uuid, name);

        if (result != LoadResult.LOADED)
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, loader.getErrorMessage(result, uuid, name));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void highAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();

        //It means someone disallowed firstAsync (so we didn't load anything) and then allowed the login again
        if (!container.isLoading(uuid) && event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED)
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, loader.getErrorMessage(LoadResult.PRE_LOGIN_REALLOWED, uuid, event.getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void lastAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();

        //Player isn't allowed to login anymore so we have to remove his data
        if (container.isLoading(uuid) && event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            container.removeLoading(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void firstLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            container.removeLoading(uuid);
            return;
        }

        LoadResult result = container.insertPlayerData(player);

        if (result != LoadResult.LOADED)
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, loader.getErrorMessage(result, uuid, player.getName()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void highLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!container.isLoaded(player) && event.getResult() == PlayerLoginEvent.Result.ALLOWED)
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, loader.getErrorMessage(LoadResult.LOGIN_REALLOWED, uuid, player.getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void lastLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (container.isLoaded(player) && event.getResult() != PlayerLoginEvent.Result.ALLOWED)
            container.removeLoaded(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void quit(PlayerQuitEvent event) {
        container.quit(event.getPlayer());
    }
}
