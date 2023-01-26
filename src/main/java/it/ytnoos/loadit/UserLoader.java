package it.ytnoos.loadit;

import java.util.Collection;
import java.util.UUID;

public interface UserLoader<T extends UserData> extends OfflineUserLoader<T> {

    void savePlayerData(T playerData);

    void batchSavePlayerData(Collection<T> playersData);

    default String getErrorMessage(LoadResult result, UUID uuid, String name) {
        return "An error occurred while trying to load your data.";
    }
}
