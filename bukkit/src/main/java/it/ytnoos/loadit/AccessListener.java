package it.ytnoos.loadit;

import it.ytnoos.loadit.api.LoadResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

class AccessListener<D, S> implements Listener {

    private final LoaditImpl<D, S> loadit;
    private final LoaditDataRegistry<D, S> registry;

    AccessListener(LoaditImpl<D, S> loadit, LoaditDataRegistry<D, S> registry) {
        this.loadit = loadit;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void firstAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();

        //We don't need to load the player since something has disallowed the connection.
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        loadit.debug("Loading data for " + uuid + " (" + name + ")");
        LoadResult result = registry.loadData(uuid, name);

        if (!result.isLoaded()) {
            loadit.debug("Cannot load data for " + uuid + " (" + name + ") (" + result + ")");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, loadit.kickMessage(result));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void highAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();

        //It means someone disallowed firstAsync (so we didn't load anything) and then allowed the login again
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED && !registry.hasData(uuid)) {
            loadit.debug(uuid + " (" + event.getName() + ") has been re-allowed in AsyncLogin but data is not loaded!");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, loadit.kickMessage(LoadResult.PRE_LOGIN_REALLOWED));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void lastAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        //Player won't join the server, we clear his offline data
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            loadit.debug("Removing data for " + event.getUniqueId() + " (" + event.getName() + ") since he won't join the server during AsyncLogin");
            registry.removeData(event.getUniqueId(), false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void firstLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            loadit.debug(uuid + " (" + player.getName() + ") has been disallowed from joining the server, removing his data...");
            registry.removeData(uuid, true);
            return;
        }

        loadit.debug("Associating data for " + uuid + " (" + player.getName() + ")");
        LoadResult result = registry.setupPlayer(player);

        if (!result.isLoaded()) {
            loadit.debug("Cannot associate data for " + uuid + " (" + player.getName() + ") (" + result + ")");
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, loadit.kickMessage(result));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void highLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED && !registry.hasData(uuid)) {
            loadit.debug(uuid + " (" + player.getName() + ") has been re-allowed in Login but data is not loaded!");
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, loadit.kickMessage(LoadResult.LOGIN_REALLOWED));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void lastLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            loadit.debug("Removing data for " + event.getPlayer().getUniqueId() + " (" + event.getPlayer().getName() + ") since he won't join the server during Login");
            registry.removeData(event.getPlayer().getUniqueId(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void quit(PlayerQuitEvent event) {
        loadit.debug("Removing data for " + event.getPlayer().getUniqueId() + " (" + event.getPlayer().getName() + ") since he quit the server");
        registry.removeData(event.getPlayer().getUniqueId(), true);
    }
}
