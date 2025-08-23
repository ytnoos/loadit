package it.ytnoos.loadit.api;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@NullMarked
public interface DataRegistry<D, S> {

    ExecutorService getExecutor();

    @Nullable
    D getCached(UUID uuid);

    @Nullable
    S getSession(Player player);

    S getSessionOrThrow(Player player);

    void acceptIfCached(UUID uuid, Consumer<D> consumer);

    void acceptIfCached(Player player, Consumer<D> consumer);

    CompletableFuture<D> load(UUID uuid);

    CompletableFuture<D> load(String name);

    void forEach(Consumer<D> consumer);

    void forEachSession(Consumer<S> consumer);

    <E extends Exception> void forEachThrowable(ThrowableConsumer<D, E> consumer) throws E;
}
