package it.ytnoos.loadit;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
                ForkJoinPool.defaultForkJoinWorkerThreadFactory, (t, e) -> e.printStackTrace(), false);
    }

    public void stop() {
        loaderExecutor.shutdown();

        data.values().forEach(userData -> userData.setPlayer(null));
        data.clear();
    }

    public boolean hasData(UUID uuid) {
        return data.containsKey(uuid);
    }

    public void removeData(UUID uuid) {
        data.remove(uuid);
    }

    protected LoadResult loadData(UUID uuid, String name) {
        if (data.containsKey(uuid)) return LoadResult.ALREADY_LOADED;

        T userData = CompletableFuture.supplyAsync(() -> {
            try {
                return loader.getOrCreate(uuid, name);
            } catch (Exception e) {
                loadit.logError(e, "Unable to get or create " + uuid + " " + name + " data");
                return null;
            }
        }, loaderExecutor).join();

        if (userData == null) return LoadResult.ERROR_LOAD_USER;

        return data.putIfAbsent(uuid, userData) == null ? LoadResult.LOADED : LoadResult.ALREADY_LOADED;
    }

    protected LoadResult setupPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        T userData = data.get(uuid);

        if (userData == null) return LoadResult.NOT_LOADED;

        userData.setPlayer(player);

        return LoadResult.LOADED;
    }

    protected void quit(Player player) {
        data.remove(player.getUniqueId()).setPlayer(null);
    }

    @Override
    public T get(Player player) {
        if (!player.isOnline()) throw new NullPointerException(player.getName() + " is not online!");
        T userData = data.get(player.getUniqueId());

        if (userData == null || !userData.getPlayer().isPresent())
            throw new NullPointerException(player.getUniqueId() + " " + player.getName() + " is not stored");

        return userData;
    }

    @Override
    public CompletableFuture<Optional<T>> get(UUID uuid) {
        T userData = data.get(uuid);

        if (userData != null) return CompletableFuture.completedFuture(Optional.of(userData));

        return CompletableFuture.supplyAsync(() -> loader.load(uuid), loaderExecutor);
    }

    @Override
    public CompletableFuture<Optional<T>> get(String name) {
        for (T userData : data.values()) {
            if (userData.getName().equals(name)) return CompletableFuture.completedFuture(Optional.of(userData));
        }

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
