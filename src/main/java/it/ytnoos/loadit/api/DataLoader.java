package it.ytnoos.loadit.api;

import java.util.Optional;
import java.util.UUID;

public interface DataLoader<T extends UserData> {

    Optional<T> getOrCreate(UUID uuid, String name);

    Optional<T> load(UUID uuid);

    Optional<T> load(String name);

    default String getErrorMessage(LoadResult result, UUID uuid, String name) {
        return "An error occurred while trying to load your data. (" + result.name() + ")";
    }
}
