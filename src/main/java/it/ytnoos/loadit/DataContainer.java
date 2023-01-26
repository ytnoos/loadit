package it.ytnoos.loadit;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public interface DataContainer<T extends UserData> extends OfflineDataContainer<T> {

    T getPlayerData(Player player);

    T getPlayerData(UUID uuid);

    Collection<T> getPlayersData();
}
