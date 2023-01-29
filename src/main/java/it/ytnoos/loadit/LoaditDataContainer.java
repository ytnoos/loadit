package it.ytnoos.loadit;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

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

        if (data.put(uuid, userData) != null) {
            data.remove(uuid);
            return LoadResult.ALREADY_LOADED;
        }

        return LoadResult.LOADED;
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loader.load(uuid);
            } catch (Exception e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }, loaderExecutor);
    }

    @Override
    public CompletableFuture<Optional<T>> get(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loader.load(name);
            } catch (Exception e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }, loaderExecutor);
    }

    @Override
    public Collection<T> get() {
        return data.values();
    }

    @Override
    public Map<Player, T> getOnlines() {
        Map<Player, T> onlineMap = new HashMap<>();

        for (T userData : data.values()) {
            userData.getPlayer().ifPresent(player -> onlineMap.put(player, userData));
        }

        return onlineMap;
    }

    @Override
    public void forEachOnline(BiConsumer<Player, T> consumer) {
        for (T userData : data.values()) {
            userData.getPlayer().ifPresent(player -> consumer.accept(player, userData));
        }
    }
}
