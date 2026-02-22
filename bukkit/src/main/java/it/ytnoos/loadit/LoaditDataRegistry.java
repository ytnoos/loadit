package it.ytnoos.loadit;

import it.ytnoos.loadit.api.*;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 *
 * @param <D> Type for offline/logging players (data)
 * @param <S> Type for online players (session)
 */
public class LoaditDataRegistry<D, S> implements DataRegistry<D, S, Player> {

    private final Loadit<D, S, Player> loadit;
    private final DataLoader<D, S, Player> loader;

    private final ConcurrentMap<UUID, D> data = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, S> sessions = new ConcurrentHashMap<>(); // write operations are only on main thread

    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();
    private final ExecutorService loaderExecutor;

    LoaditDataRegistry(Loadit<D, S, Player> loadit, DataLoader<D, S, Player> loader, int parallelism) {
        this.loadit = loadit;
        this.loader = loader;

        loaderExecutor = new ForkJoinPool(
                parallelism,
                pool -> {
                    ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                    worker.setDaemon(true);
                    worker.setName("loadit-executor-" + worker.getPoolIndex());
                    return worker;
                }, (t, e) -> loadit.log(Level.SEVERE, e, "An exception occurred in loadit executor"), false);
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

    public void stop() {
        loaderExecutor.shutdown();
        try {
            loaderExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            loadit.log(Level.SEVERE, e, "Interrupted await termination");
            Thread.currentThread().interrupt();
        } finally {
            sessions.clear();
            data.clear();
            loading.clear();
        }
    }

    public boolean hasData(UUID uuid) {
        return data.containsKey(uuid);
    }

    public void removeData(UUID uuid, boolean session) {
        if (session) sessions.remove(uuid);
        D userData = data.remove(uuid);
        if (userData == null) return;

        notifyListeners(listener -> listener.onUnload(userData));
    }

    protected LoadResult loadData(UUID uuid, String name) {
        if (hasData(uuid)) return LoadResult.ALREADY_LOADED;

        if (!loading.add(uuid)) return LoadResult.ALREADY_LOADING;

        try {
            if (hasData(uuid)) return LoadResult.ALREADY_LOADED;

            if (!callListeners(listener -> listener.onPreLoad(uuid, name))) return LoadResult.ERROR;

            D userData = loader.getOrCreate(uuid, name);
            if (userData == null) return LoadResult.ERROR;

            D previousValue = data.put(uuid, userData);

            if (previousValue != null)
                loadit.log(Level.WARNING, uuid + " " + name + " was already loaded!");

            if (!callListeners(listener -> listener.onPostLoad(userData))) return LoadResult.ERROR;

            return LoadResult.LOADED;
        } catch (Exception e) {
            loadit.log(Level.SEVERE, e, "Unable to get or create " + uuid + " " + name + " data");
            return LoadResult.error(e);
        } finally {
            loading.remove(uuid);
        }
    }

    protected LoadResult setupPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        D userData = data.get(uuid);
        if (userData == null) return LoadResult.NOT_LOADED;

        S session = loader.startSession(userData, player);
        if (session == null) return LoadResult.NOT_LOADED;

        sessions.put(uuid, session);

        if (!callListeners(listener -> listener.onSessionStart(userData, session))) {
            sessions.remove(uuid);
            return LoadResult.NOT_LOADED;
        }

        return LoadResult.LOADED;
    }

    private boolean callListeners(Function<LoaditLoadListener<D, S>, Boolean> function) {
        for (LoaditLoadListener<D, S> listener : loadit.listeners()) {
            try {
                if (!function.apply(listener)) return false;
            } catch (Exception e) {
                loadit.log(Level.SEVERE, e, "Error in listener " + listener.getClass().getName());
            }
        }
        return true;
    }

    private void notifyListeners(Consumer<LoaditLoadListener<D, S>> consumer) {
        callListeners(listener -> {
            consumer.accept(listener);
            return true;
        });
    }
}
