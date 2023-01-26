package it.ytnoos.loadit;

import com.google.common.base.Objects;
import org.bukkit.entity.Player;

public abstract class PlayerData {

    protected final UserData userData;
    protected final Player player;

    public PlayerData(UserData userData, Player player) {
        this.userData = userData;
        this.player = player;
    }

    /**
     * Override this and cast with your own UserData object
     *
     * @return
     */
    public abstract UserData getUserData();

    public final Player getPlayer() {
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
