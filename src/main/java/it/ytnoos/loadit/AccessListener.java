package it.ytnoos.loadit;

import it.ytnoos.loadit.api.DataLoader;
import it.ytnoos.loadit.api.LoadResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class AccessListener implements Listener {

    private final BaseLoadit<?> loadit;
    private final DataLoader<?> loader;
    private final LoaditDataContainer<?> container;

    public AccessListener(BaseLoadit<?> loadit, DataLoader<?> loader, LoaditDataContainer<?> container) {
        this.loadit = loadit;
        this.loader = loader;
        this.container = container;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void firstAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();

        //We don't need to load the player since something has disallowed the connection.
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        loadit.debug("Loading data for " + uuid + " (" + name + ")");
        LoadResult result = container.loadData(uuid, name);

        if (result != LoadResult.LOADED) {
            loadit.debug("Error while loading data for " + uuid + " (" + name + ")");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, loader.getErrorMessage(result, uuid, name));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void highAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();

        //It means someone disallowed firstAsync (so we didn't load anything) and then allowed the login again
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED && !container.hasData(uuid)) {
            loadit.debug(uuid + " (" + event.getName() + ") has been re-allowed in AsyncLogin but data is not loaded!");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, loader.getErrorMessage(LoadResult.PRE_LOGIN_REALLOWED, uuid, event.getName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void lastAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        //Player won't join the server, we clear his offline data
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            loadit.debug("Removing data for " + event.getUniqueId() + " (" + event.getName() + ") since he won't join the server during AsyncLogin");
            container.removeData(event.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void firstLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            loadit.debug(uuid + " (" + player.getName() + ") has been disallowed from joining the server, removing his data...");
            container.removeData(uuid);
            return;
        }

        loadit.debug("Associating data for " + uuid + " (" + player.getName() + ")");
        LoadResult result = container.setupPlayer(player);

        if (result != LoadResult.LOADED) {
            loadit.debug("Error while associating data for " + uuid + " (" + player.getName() + ")");
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, loader.getErrorMessage(result, uuid, player.getName()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void highLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED && !container.hasData(uuid)) {
            loadit.debug(uuid + " (" + player.getName() + ") has been re-allowed in Login but data is not loaded!");
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, loader.getErrorMessage(LoadResult.LOGIN_REALLOWED, uuid, player.getName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void lastLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            loadit.debug("Removing data for " + event.getPlayer().getUniqueId() + " (" + event.getPlayer().getName() + ") since he won't join the server during Login");
            container.removeData(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void quit(PlayerQuitEvent event) {
        loadit.debug("Removing data for " + event.getPlayer().getUniqueId() + " (" + event.getPlayer().getName() + ") since he quit the server");
        container.removeData(event.getPlayer().getUniqueId());
    }
}
