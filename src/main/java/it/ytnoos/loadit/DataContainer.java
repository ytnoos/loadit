package it.ytnoos.loadit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;

public class DataContainer<T extends UserData, V extends PlayerData> {

    private static final long OLD_DATA_TIME = TimeUnit.MINUTES.toMillis(15);
    private static final long OLD_DATA_PERIOD = 1;

    private final Loadit<T, V> loadit;
    private final LoaditLoader<T, V> loader;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("loadit-executor").build());

    private final Map<UUID, T> loading = new ConcurrentHashMap<>();
    private final Map<UUID, V> data = new ConcurrentHashMap<>();
    private final Set<UUID> saving = Collections.newSetFromMap(new ConcurrentHashMap<>()); //ahahahah

    public DataContainer(Loadit<T, V> loadit, LoaditLoader<T, V> loader) {
        this.loadit = loadit;
        this.loader = loader;

        executor.scheduleWithFixedDelay(() -> loading.values().removeIf(data -> System.currentTimeMillis() - data.getLoadTime() > OLD_DATA_TIME),
                OLD_DATA_PERIOD,
                OLD_DATA_PERIOD,
                TimeUnit.MINUTES);
    }

    public void stop() {
        executor.shutdown();
        saveAll();

        loading.clear();
        data.clear();
        saving.clear();
    }

    protected boolean isLoading(UUID uuid) {
        return loading.containsKey(uuid);
    }

    protected void removeLoading(UUID uuid) {
        loading.remove(uuid);
    }

    protected boolean isLoaded(Player player) {
        return data.containsKey(player.getUniqueId());
    }

    protected void removeLoaded(UUID uuid) {
        data.remove(uuid);
    }

    protected CompletableFuture<LoadResult> insertData(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            if (saving.contains(uuid)) return LoadResult.SAVING_USER;
            if (data.containsKey(uuid)) return LoadResult.ALREADY_LOADED_PLAYER_PRE;

            T userData = loading.get(uuid);

            if (userData == null) {
                try {
                    userData = loader.loadUserData(uuid, name);
                } catch (Exception e) {
                    return LoadResult.ERROR_LOAD_USER;
                }

                if (userData == null) return LoadResult.ERROR_LOAD_USER;

                loading.put(uuid, userData);
            }

            return LoadResult.LOADED;
        }, executor);
    }

    protected CompletableFuture<LoadResult> insertPlayerData(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID uuid = player.getUniqueId();
            T userData = loading.remove(uuid);

            if (userData == null) return LoadResult.NOT_LOADED;
            if (saving.contains(uuid)) return LoadResult.SAVING_PLAYER;
            if (data.containsKey(uuid)) return LoadResult.ALREADY_LOADED_PLAYER;

            V playerData;

            try {
                playerData = loader.loadPlayerData(userData, player);
            } catch (Exception e) {
                return LoadResult.ERROR_LOAD_PLAYER;
            }

            if (playerData == null) return LoadResult.ERROR_LOAD_PLAYER;

            data.put(uuid, playerData);
            return LoadResult.LOADED;
        }, executor);
    }

    protected void quit(Player player) {
        CompletableFuture.runAsync(() -> {
            String name = player.getName();
            UUID uuid = player.getUniqueId();

            if (loading.remove(uuid) != null) loadit.log(name + " had its user data loaded even if it was online!");

            V playerData = data.remove(uuid);

            if (playerData == null) {
                loadit.log("Tried to save " + name + " player data which is not loaded!");
                return;
            }

            saving.add(uuid);

            try {
                loader.savePlayerData(playerData);
            } catch (Exception e) {
                loadit.logError(e, "Unable to save " + name + " data");
            }

            saving.remove(uuid);
        }, executor);
    }

    protected void saveAll() {
        CompletableFuture.runAsync(() -> {
            Collection<V> playersData = data.values();

            for (V playerData : playersData) {
                saving.add(playerData.getPlayer().getUniqueId());
            }

            try {
                loader.batchSavePlayerData(playersData);
            } catch (Exception e) {
                loadit.logError(e, "Unable to batch save players");
            }

            for (V playerData : playersData) {
                saving.remove(playerData.getPlayer().getUniqueId());
            }
        }, executor).join();
    }

    public V getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public V getPlayerData(UUID uuid) {
        return data.get(uuid);
    }

    public Collection<V> getPlayersData() {
        return Collections.unmodifiableCollection(data.values());
    }

    public CompletableFuture<Optional<T>> getOfflineData(UUID uuid) {
        //online
        V playerData = getPlayerData(uuid);
        if (playerData != null) return CompletableFuture.completedFuture(Optional.of((T) playerData.getUserData()));

        //cached
        T userData = loading.get(uuid);
        if (userData != null) return CompletableFuture.completedFuture(Optional.of(userData));

        //load + put on cache
        return CompletableFuture
                .supplyAsync(() -> {
                    T loadedData = loader.loadUserData(uuid);
                    if (loadedData != null) loading.put(uuid, loadedData); //Put in cache

                    return Optional.ofNullable(loadedData);
                }, executor)
                .exceptionally(t -> {
                    loadit.logError(t, "Unable to get offline data for " + uuid);
                    return Optional.empty();
                });
    }
}
