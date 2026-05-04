package it.ytnoos.loadit;

import it.ytnoos.loadit.api.LoadResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;

final class LegacyLoginListener<D, S> implements Listener {

    private final LoaditImpl<D, S> loadit;
    private final LoaditDataRegistry<D, S> registry;

    LegacyLoginListener(LoaditImpl<D, S> loadit, LoaditDataRegistry<D, S> registry) {
        this.loadit = loadit;
        this.registry = registry;
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
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, loadit.kickMessage(result, uuid, player.getName()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void highLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED && !registry.hasData(uuid)) {
            loadit.debug(uuid + " (" + player.getName() + ") has been re-allowed in Login but data is not loaded!");
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, loadit.kickMessage(LoadResult.LOGIN_REALLOWED, uuid, player.getName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void lastLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            loadit.debug("Removing data for " + event.getPlayer().getUniqueId() + " (" + event.getPlayer().getName() + ") since he won't join the server during Login");
            registry.removeData(event.getPlayer().getUniqueId(), true);
        }
    }
}
