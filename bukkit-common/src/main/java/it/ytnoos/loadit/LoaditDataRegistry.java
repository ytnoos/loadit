package it.ytnoos.loadit;

import it.ytnoos.loadit.api.DataLoader;
import it.ytnoos.loadit.api.DataRegistry;
import it.ytnoos.loadit.api.ThrowableConsumer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 *
 * @param <D> Type for offline/logging players (data)
 * @param <S> Type for online players (session)
 */
public class LoaditDataRegistry<D, S> implements DataRegistry<D, S, Player> {

    private final DataLoader<D, S, Player> loader;
    private final ExecutorService loaderExecutor;

    private final ConcurrentMap<UUID, D> data = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, S> sessions = new ConcurrentHashMap<>(); // write operations are only on main thread

    LoaditDataRegistry(DataLoader<D, S, Player> loader, ExecutorService loaderExecutor) {
        this.loader = loader;
        this.loaderExecutor = loaderExecutor;
    }

    @Override
    public ExecutorService executor() {
        return loaderExecutor;
    }

    @Nullable
    @Override
    public D data(UUID uuid) {
        return data.get(uuid);
    }

    @Override
    public @Nullable S session(Player player) {
        return sessions.get(player.getUniqueId());
    }

    @Override
    public S requireSession(Player player) {
        S session = session(player);

        if (session == null)
            throw new NullPointerException(player.getUniqueId() + " " + player.getName() + " does not have a session");

        return session;
    }

    @Override
    public void ifPresent(UUID uuid, Consumer<D> consumer) {
        D userData = data.get(uuid);
        if (userData != null) consumer.accept(userData);
    }

    @Override
    public void ifPresent(Player player, Consumer<D> consumer) {
        ifPresent(player.getUniqueId(), consumer);
    }

    @Override
    public CompletableFuture<@Nullable D> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> loader.load(uuid), loaderExecutor);
    }

    @Override
    public CompletableFuture<@Nullable D> load(String name) {
        return CompletableFuture.supplyAsync(() -> loader.load(name), loaderExecutor);
    }

    @Override
    public void forEach(Consumer<D> consumer) {
        data.values().forEach(consumer);
    }

    @Override
    public void forEachSession(Consumer<S> consumer) {
        sessions.values().forEach(consumer);
    }

    @Override
    public <E extends Exception> void forEachThrowing(ThrowableConsumer<D, E> consumer) throws E {
        for (D userData : data.values()) {
            consumer.accept(userData);
        }
    }

    @Override
    public <E extends Exception> void forEachSessionThrowing(ThrowableConsumer<S, E> consumer) throws E {
        for (S session : sessions.values()) {
            consumer.accept(session);
        }
    }

    void clear() {
        sessions.clear();
        data.clear();
    }

    public boolean hasData(UUID uuid) {
        return data.containsKey(uuid);
    }

    public boolean hasSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    @Nullable D removeData(UUID uuid, boolean session) {
        if (session) sessions.remove(uuid);
        return data.remove(uuid);
    }

    void removeData(UUID uuid, D userData) {
        data.remove(uuid, userData);
    }

    @Nullable D putData(UUID uuid, D userData) {
        return data.put(uuid, userData);
    }

    void putSession(UUID uuid, S session) {
        sessions.put(uuid, session);
    }

    void removeSession(UUID uuid) {
        sessions.remove(uuid);
    }

}
