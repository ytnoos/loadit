package it.ytnoos.loadit;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public interface LoaditLoader<T extends UserData, V extends PlayerData> {

    T loadUserData(UUID uuid, String name);

    V loadPlayerData(T userData, Player player);

    T loadUserData(UUID uuid);

    void savePlayerData(V playerData);

    void batchSavePlayerData(Collection<V> playersData);

    default String getErrorMessage(LoadResult result, UUID uuid, String name) {
        return "An error occurred while trying to load your data.";
    }
}
