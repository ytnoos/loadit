package it.ytnoos.loadit;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface DataContainer<T extends UserData> {

    ExecutorService getExecutor();

    Optional<T> getCached(UUID uuid);

    T getCached(Player player);

    void acceptIfCached(Player player, Consumer<T> consumer);

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

    void forEach(Consumer<T> consumer);

    <E extends Exception> void forEachThrowable(ThrowableConsumer<T, E> consumer) throws E;

    void forEach(BiConsumer<Player, T> consumer);

    <E extends Exception> void forEachThrowable(ThrowableBiConsumer<Player, T, E> consumer) throws E;
}
