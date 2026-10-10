package it.ytnoos.loadit;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

final class PaperLoginValidateListener<D, S> implements Listener {

    private final LoaditLoadCoordinator<D, S> coordinator;

    PaperLoginValidateListener(LoaditLoadCoordinator<D, S> coordinator) {
        this.coordinator = coordinator;
    }

    // Fired once the pre-login has completed: from now on a closed login connection fires PlayerConnectionCloseEvent.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onValidateLogin(PlayerConnectionValidateLoginEvent event) {
        if (!(event.getConnection() instanceof PlayerLoginConnection connection)) return;

        PlayerProfile profile = connection.getAuthenticatedProfile();
        UUID uuid = profile == null ? null : profile.getId();
        if (uuid != null) coordinator.loginValidated(uuid, connection);
    }
}
