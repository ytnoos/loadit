package it.ytnoos.loadit;

import it.ytnoos.loadit.api.LoadResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

final class ModernJoinListener<D, S> implements Listener {

    private final LoaditImpl<D, S> loadit;
    private final LoaditDataRegistry<D, S> registry;
    private final LoaditLoadCoordinator<D, S> coordinator;

    ModernJoinListener(LoaditImpl<D, S> loadit, LoaditDataRegistry<D, S> registry, LoaditLoadCoordinator<D, S> coordinator) {
        this.loadit = loadit;
        this.registry = registry;
        this.coordinator = coordinator;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void firstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (registry.hasSession(uuid)) return;

        loadit.debug("Associating data for " + uuid + " (" + player.getName() + ") on join");
        LoadResult result = coordinator.setupPlayer(player);
        if (result.isLoaded()) return;

        loadit.debug("Cannot associate data for " + uuid + " (" + player.getName() + ") on join (" + result + ")");
        player.kickPlayer(loadit.kickMessage(result, uuid, player.getName()));
    }
}
