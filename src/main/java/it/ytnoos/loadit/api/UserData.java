package it.ytnoos.loadit.api;


import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public abstract class UserData {

    protected final UUID uuid;
    protected final String name;

    private @Nullable Player player;

    protected UserData(@NotNull UUID uuid, @NotNull String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    public void setPlayer(@Nullable Player player) {
        this.player = player;
    }
}
