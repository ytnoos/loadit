package it.ytnoos.loadit;

import it.ytnoos.loadit.api.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoaditImpl<D, S> implements BukkitLoadit<D, S> {

    private final Plugin plugin;
    private final LoaditDataRegistry<D, S> registry;
    private final LoaditLoadCoordinator<D, S> coordinator;
    private final AccessListener<D, S> listener;
    private final LoaditLifecycleStrategy strategy;
    private final List<LoaditLoadListener<D, S>> pluginListeners = new CopyOnWriteArrayList<>();

    private static final String LOG_PREFIX = "[Loadit] ";
    private KickMessageProvider kickMessageProvider = LoaditImpl::defaultKickMessage;
    private boolean debug = false;
    private boolean initialized = false;
    private boolean stopped = false;

    protected LoaditImpl(Plugin plugin, DataLoader<D, S, Player> loader, int parallelism, LoaditLifecycleStrategyFactory lifecycleStrategyFactory) {
        this.plugin = plugin;

        ExecutorService loaderExecutor = LoaditLoadCoordinator.createExecutor(this, parallelism);
        registry = new LoaditDataRegistry<>(loader, loaderExecutor);
        coordinator = new LoaditLoadCoordinator<>(this, loader, registry, loaderExecutor);
        listener = new AccessListener<>(this, registry, coordinator);
        strategy = lifecycleStrategyFactory.create(this, listener, registry, coordinator);
    }

    private static String defaultKickMessage(LoadResult result, UUID uuid, String name) {
        String message = "An error occurred while trying to load your data. (" + result.type().name() + ")";
        if (result.cause() != null) message += "\n" + result.cause().getMessage();
        return message;
    }

    @Override
    public void stop() {
        if (!initialized) return;

        HandlerList.unregisterAll(listener);
        strategy.listeners().forEach(HandlerList::unregisterAll);
        coordinator.stop();

        initialized = false;
        stopped = true;
    }

    @Override
    public void addListener(LoaditLoadListener<D, S> listener) {
        pluginListeners.add(listener);
    }

    @Override
    public void removeListener(LoaditLoadListener<D, S> listener) {
        pluginListeners.remove(listener);
    }

    @Override
    public Logger logger() {
        return plugin.getLogger();
    }

    @Override
    public void log(Level level, String message) {
        logger().log(level, () -> LOG_PREFIX + message);
    }

    @Override
    public void log(Level level, Throwable e, String message) {
        logger().log(level, e, () -> LOG_PREFIX + message);
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    @Override
    public DataRegistry<D, S, Player> registry() {
        return registry;
    }

    @Override
    public List<LoaditLoadListener<D, S>> listeners() {
        return Collections.unmodifiableList(pluginListeners);
    }

    @Override
    public void init() {
        if (initialized) throw new IllegalStateException("Loadit has already been initialized");
        if (stopped) throw new IllegalStateException("Loadit cannot be restarted after stop");

        initialized = true;
        coordinator.timeoutCleanup(strategy.usesTimeoutCleanup());
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        strategy.listeners().forEach(listener -> plugin.getServer().getPluginManager().registerEvents(listener, plugin));

        for (Player player : Bukkit.getOnlinePlayers()) {
            LoadResult result = coordinator.loadData(player.getUniqueId(), player.getName());
            if (result.isLoaded()) {
                result = coordinator.setupPlayer(player);
                if (result.isLoaded()) continue;
            }

            player.kickPlayer(kickMessageProvider.getKickMessage(result, player.getUniqueId(), player.getName()));
        }
    }

    @Override
    public void debug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void setKickMessage(KickMessageProvider kickMessageProvider) {
        this.kickMessageProvider = kickMessageProvider;
    }

    void debug(String message) {
        if (debug) log(Level.INFO, message);
    }

    String kickMessage(LoadResult result, UUID uuid, String name) {
        return kickMessageProvider.getKickMessage(result, uuid, name);
    }

}
