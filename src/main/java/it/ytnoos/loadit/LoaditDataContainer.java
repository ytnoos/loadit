package it.ytnoos.loadit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;

public class LoaditDataContainer<T extends UserData> implements DataContainer<T> {

    private final Loadit<T> loadit;
    private final UserLoader<T> loader;

    private final ConcurrentMap<UUID, T> dataMap = new ConcurrentHashMap<>();
    private final Set<UUID> saving = Collections.newSetFromMap(new ConcurrentHashMap<>()); //ahahahah

    private final ExecutorService loaderExecutor;
    private final ScheduledExecutorService cleaner;

    public LoaditDataContainer(Loadit<T> loadit, UserLoader<T> loader) {
        this.loadit = loadit;
        this.loader = loader;

        Settings settings = loadit.getSettings();

        loaderExecutor = Executors.newFixedThreadPool(
                settings.getLoaderPoolSize(),
                new ThreadFactoryBuilder().setNameFormat("loadit-loader-" + loadit.getPlugin().getName()).build());

        cleaner = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("loadit-cleaner-" + loadit.getPlugin().getName()).build());

        cleaner.scheduleWithFixedDelay(() -> dataMap.values().removeIf(data -> data.isCache() && System.currentTimeMillis() - data.getLoadTime() > settings.getMaximumCacheTime()),
                settings.getCleanerPeriod(),
                settings.getCleanerPeriod(),
                TimeUnit.MILLISECONDS);
    }

    public void stop() {
        loaderExecutor.shutdown();
        cleaner.shutdown();

        saveAll();
    }

    protected boolean hasData(UUID uuid) {
        return dataMap.containsKey(uuid);
    }

    protected void removeData(UUID uuid) {
        dataMap.remove(uuid);
    }

    protected LoadResult loadData(UUID uuid, String name) {
        T offlineData = dataMap.computeIfAbsent(uuid, u -> {
            if (saving.contains(uuid)) return null;

            return CompletableFuture.supplyAsync(() -> {
                try {
                    return loader.loadOfflineData(uuid, name);
                } catch (Exception e) {
                    loadit.logError(e, "Unable to load " + uuid + " " + name);
                    return null;
                }
            }, loaderExecutor).join();
        });

        return offlineData != null ? LoadResult.LOADED : LoadResult.ERROR_LOAD_USER;
    }

    protected LoadResult setupPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        T data = dataMap.get(uuid);

        if (data != null) {
            data.setPlayer(player);
            data.setCache(false);
            return LoadResult.LOADED;
        }

        return LoadResult.NOT_LOADED;
    }

    protected void quit(Player player) {
        UUID uuid = player.getUniqueId();

        if (!saving.add(uuid)) {
            loadit.warn(uuid + " is already being saved!");
            return;
        }

        T data = dataMap.remove(uuid);

        if (data == null) {
            loadit.warn("Tried to save " + player.getName() + " player data which is not loaded!");
            saving.remove(uuid);
            return;
        }

        loaderExecutor.execute(() -> {
            try {
                loader.savePlayerData(data);
            } catch (Exception e) {
                loadit.logError(e, "Unable to save " + player.getName() + " data");
            }

            saving.remove(uuid);
        });
    }

    protected void saveAll() {
        Collection<T> playersData = dataMap.values();

        playersData.forEach(data -> saving.add(data.getUUID()));

        try {
            loader.batchSavePlayerData(playersData);
        } catch (Exception e) {
            loadit.logError(e, "Unable to batch save players");
        }

        playersData.forEach(data -> saving.remove(data.getUUID()));
    }

    public T getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public T getPlayerData(UUID uuid) {
        return dataMap.get(uuid);
    }

    public Collection<T> getPlayersData() {
        return Collections.unmodifiableCollection(dataMap.values());
    }

    @Override
    public CompletableFuture<Optional<T>> getOfflineData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(dataMap.computeIfAbsent(uuid, u -> {
            if (saving.contains(uuid)) return null;

            try {
                T data = loader.loadOfflineData(uuid);
                data.setCache(true);
                return data;
            } catch (Exception e) {
                loadit.logError(e, "Unable to load " + uuid);
                return null;
            }
        })), loaderExecutor);
    }
}
