package it.ytnoos.loadit;

import com.google.common.base.Objects;
import org.bukkit.entity.Player;

public class PlayerData {

    private final UserData userData;
    private final Player player;

    public PlayerData(UserData userData, Player player) {
        this.userData = userData;
        this.player = player;
    }

    public UserData getUserData() {
        return userData;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerData that = (PlayerData) o;
        return Objects.equal(userData, that.userData) && Objects.equal(player, that.player);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userData, player);
    }
}
