package it.ytnoos.loadit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataContainer<T extends UserData, V extends PlayerData> {

    private final Loadit<T, V> loadit;
    private final LoaditLoader<T, V> loader;
    private final ExecutorService saveExecutor;

    private final Map<UUID, T> loading = new HashMap<>();
    private final Map<UUID, V> data = new HashMap<>();
    private final Set<UUID> saving = Collections.newSetFromMap(new ConcurrentHashMap<>()); //ahahahah

    public DataContainer(Loadit<T, V> loadit, LoaditLoader<T, V> loader, int poolSize) {
        this.loadit = loadit;
        this.loader = loader;
        this.saveExecutor = Executors.newFixedThreadPool(poolSize, new ThreadFactoryBuilder().setNameFormat("loadit-saver").build());
    }

    public void stop() {
        saveExecutor.shutdown();
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

    protected LoadResult insertData(UUID uuid, String name) {
        if (saving.contains(uuid)) return LoadResult.SAVING_USER;
        if (loading.containsKey(uuid)) return LoadResult.ALREADY_LOADING_PRE;
        if (data.containsKey(uuid)) return LoadResult.ALREADY_LOADED_PLAYER_PRE;

        T userData;

        try {
            userData = loader.loadUserData(uuid, name);
        } catch (Exception e) {
            return LoadResult.ERROR_LOAD_USER;
        }

        if (userData == null) return LoadResult.ERROR_LOAD_USER;

        loading.put(uuid, userData);
        return LoadResult.LOADED;
    }

    protected LoadResult insertPlayerData(Player player) {
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
    }

    protected void quit(Player player) {
        String name = player.getName();
        UUID uuid = player.getUniqueId();

        if (loading.remove(uuid) != null) loadit.log(name + " had its user data loaded even if it was online!");

        V playerData = data.remove(uuid);

        if (playerData == null) {
            loadit.log("Tried to save " + name + " player data which is not loaded!");
            return;
        }

        saving.add(uuid);

        saveExecutor.execute(() -> {
            try {
                loader.savePlayerData(playerData);
            } catch (Exception e) {
                loadit.logError(e, "Unable to save " + name + " data");
            }

            saving.remove(uuid);
        });
    }

    protected void saveAll() {
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
    }

    public V getPlayerData(Player player) {
        return data.get(player.getUniqueId());
    }

    public Optional<V> getPlayerData(UUID uuid) {
        return Optional.ofNullable(data.get(uuid));
    }

    public Collection<V> getPlayersData() {
        return Collections.unmodifiableCollection(data.values());
    }

    public T getUserData(UUID uuid) {
        return loader.loadUserData(uuid);
    }

    public T loadUserData(String name) {
        return loader.loadUserData(name);
    }

}
