package it.ytnoos.loadit.api;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
public interface DataLoader<D, S> {

    @Nullable
    D getOrCreate(UUID uuid, String name);

    @Nullable
    D load(UUID uuid);

    @Nullable
    D load(String name);

    @Nullable
    S startSession(D data, Player player);

    default String getErrorMessage(LoadResult result, UUID uuid, String name) {
        return "An error occurred while trying to load your data. (" + result.name() + ")";
    }
}
