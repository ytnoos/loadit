package it.ytnoos.loadit;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface DataContainer<T extends UserData> {

    T get(Player player);

    CompletableFuture<Optional<T>> get(UUID uuid);

    CompletableFuture<Optional<T>> get(String name);

    Collection<T> get();

    Map<Player, T> getOnlines();

    default void forEach(Consumer<T> consumer) {
        get().forEach(consumer);
    }

    void forEachOnline(BiConsumer<Player, T> consumer);
}
