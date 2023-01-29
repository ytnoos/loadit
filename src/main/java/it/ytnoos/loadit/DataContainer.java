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

    default void accept(UUID uuid, Consumer<Optional<T>> consumer) {
        get(uuid).thenAccept(consumer);
    }

    default void acceptIfExists(UUID uuid, Consumer<T> consumer) {
        accept(uuid, optional -> optional.ifPresent(consumer));
    }

    CompletableFuture<Optional<T>> get(String name);

    default void accept(String name, Consumer<Optional<T>> consumer) {
        get(name).thenAccept(consumer);
    }

    default void acceptIfExists(String name, Consumer<T> consumer) {
        accept(name, optional -> optional.ifPresent(consumer));
    }

    Collection<T> get();

    Map<Player, T> getOnlines();

    default void forEach(Consumer<T> consumer) {
        get().forEach(consumer);
    }

    void forEachOnline(BiConsumer<Player, T> consumer);
}
