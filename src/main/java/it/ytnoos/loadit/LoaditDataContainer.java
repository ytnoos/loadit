package it.ytnoos.loadit;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

public class LoaditDataContainer<T extends UserData> implements DataContainer<T> {

    private final Loadit<T> loadit;
    private final DataLoader<T> loader;

    private final ConcurrentMap<UUID, T> data = new ConcurrentHashMap<>();
    private final ExecutorService loaderExecutor;

    public LoaditDataContainer(Loadit<T> loadit, DataLoader<T> loader) {
        this.loadit = loadit;
        this.loader = loader;

        loaderExecutor = new ForkJoinPool(
                loadit.getSettings().getParallelism(),
                pool -> {
                    ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                    worker.setDaemon(true);
                    worker.setName("loadit-executor-" + worker.getPoolIndex());
                    return worker;
                }, (t, e) -> e.printStackTrace(), false);
    }

    public void stop() {
        loaderExecutor.shutdown();
        try {
            loaderExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            loadit.getPlugin().getLogger().log(Level.SEVERE, e, () -> "Interrupted await termination");
            Thread.currentThread().interrupt();
        } finally {
            data.values().forEach(userData -> userData.setPlayer(null));
            data.clear();
        }
    }

    public boolean hasData(UUID uuid) {
        return data.containsKey(uuid);
    }

    public void removeData(UUID uuid) {
        T userData = data.remove(uuid);
        if (userData == null) return;

        try {
            for (LoaditLoadListener<T> listener : loadit.getListeners()) {
                listener.onUnload(userData);
            }
        } catch (Exception e) {
            loadit.getPlugin().getLogger().log(Level.SEVERE, e, () -> "Error while calling onUnload listener");
        }

        userData.setPlayer(null);
    }

    protected LoadResult loadData(UUID uuid, String name) {
        return data.computeIfAbsent(uuid, u -> {
            try {
                for (LoaditLoadListener<T> listener : loadit.getListeners()) {
                    listener.onPreLoad(uuid, name);
                }

                T userData = loader.getOrCreate(uuid, name).orElse(null);

                if (userData != null) {
                    for (LoaditLoadListener<T> listener : loadit.getListeners()) {
                        listener.onPostLoad(userData);
                    }
                }

                return userData;
            } catch (Exception e) {
                loadit.logError(e, "Unable to get or create " + uuid + " " + name + " data");
                return null;
            }
        }) != null ? LoadResult.LOADED : LoadResult.ERROR_LOAD_USER;
    }

    protected LoadResult setupPlayer(Player player) {
        T userData = data.get(player.getUniqueId());

        if (userData == null) return LoadResult.NOT_LOADED;

        userData.setPlayer(player);

        return LoadResult.LOADED;
    }

    @Override
    public ExecutorService getExecutor() {
        return loaderExecutor;
    }

    @Override
    public Optional<T> getCached(UUID uuid) {
        return Optional.ofNullable(data.get(uuid));
    }

    @Override
    public T getCached(Player player) {
        if (!player.isOnline()) throw new NullPointerException(player.getName() + " is not online!");
        T userData = data.get(player.getUniqueId());

        if (userData == null || !userData.getPlayer().isPresent())
            throw new NullPointerException(player.getUniqueId() + " " + player.getName() + " is not stored");

        return userData;
    }

    @Override
    public void acceptIfCached(Player player, Consumer<T> consumer) {
        T userData = data.get(player.getUniqueId());
        if (userData != null) consumer.accept(userData);
    }

    @Override
    public CompletableFuture<Optional<T>> get(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> loader.load(uuid), loaderExecutor);
    }

    @Override
    public CompletableFuture<Optional<T>> get(String name) {
        return CompletableFuture.supplyAsync(() -> loader.load(name), loaderExecutor);
    }

    @Override
    public void forEach(Consumer<T> consumer) {
        data.values().forEach(consumer);
    }

    @Override
    public <E extends Exception> void forEachThrowable(ThrowableConsumer<T, E> consumer) throws E {
        for (T userData : data.values()) {
            consumer.accept(userData);
        }
    }

    @Override
    public void forEach(BiConsumer<Player, T> consumer) {
        data.values().forEach(userData -> userData.getPlayer().ifPresent(player -> consumer.accept(player, userData)));
    }

    @Override
    public <E extends Exception> void forEachThrowable(ThrowableBiConsumer<Player, T, E> consumer) throws E {
        Player player;
        for (T userData : data.values()) {
            player = userData.getPlayer().orElse(null);
            if (player != null) consumer.accept(player, userData);
        }
    }
}
