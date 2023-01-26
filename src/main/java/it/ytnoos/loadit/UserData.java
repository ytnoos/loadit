package it.ytnoos.loadit;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class UserData extends OfflineUserData {

    protected Player player;

    protected UserData(@NotNull UUID uuid) {
        super(uuid);
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}
