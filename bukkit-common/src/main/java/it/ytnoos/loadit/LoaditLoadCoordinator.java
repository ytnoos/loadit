package it.ytnoos.loadit;

import it.ytnoos.loadit.api.DataLoader;
import it.ytnoos.loadit.api.LoadFailureException;
import it.ytnoos.loadit.api.LoadResult;
import it.ytnoos.loadit.api.LoaditLoadListener;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

final class LoaditLoadCoordinator<D, S> {

    private static final long CLEANUP_TIMEOUT_SECONDS = 30L;
    private static final long NO_CONNECTION_TOKEN = 0L;

    private final BukkitLoadit<D, S> loadit;
    private final DataLoader<D, S, Player> loader;
    private final LoaditDataRegistry<D, S> registry;
    private final ExecutorService loaderExecutor;
    // Guards loader.getOrCreate from running twice for the same UUID.
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();
    // Identifies the pre-login data owner for stale cleanup tasks.
    private final ConcurrentMap<UUID, Long> connectionTokens = new ConcurrentHashMap<>();
    private final AtomicLong connectionTokenGenerator = new AtomicLong();
    private volatile boolean stopping;
    private @Nullable TimeoutCleanup timeoutCleanup;

    LoaditLoadCoordinator(BukkitLoadit<D, S> loadit, DataLoader<D, S, Player> loader, LoaditDataRegistry<D, S> registry, ExecutorService loaderExecutor) {
        this.loadit = loadit;
        this.loader = loader;
        this.registry = registry;
        this.loaderExecutor = loaderExecutor;
    }

    static <D, S> ExecutorService createExecutor(BukkitLoadit<D, S> loadit, int parallelism) {
        return new ForkJoinPool(
                parallelism,
                pool -> {
                    ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                    worker.setDaemon(true);
                    worker.setName("loadit-executor-" + worker.getPoolIndex());
                    return worker;
                }, (t, e) -> loadit.log(Level.SEVERE, e, "An exception occurred in loadit executor"), false);
    }

    void stop() {
        stopping = true;
        loaderExecutor.shutdown();
        TimeoutCleanup timeoutCleanup = this.timeoutCleanup;
        try {
            if (!loaderExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                loaderExecutor.shutdownNow();
                loadit.log(Level.WARNING, "Loadit executor did not stop within 30 seconds");
            }
        } catch (InterruptedException e) {
            loaderExecutor.shutdownNow();
            loadit.log(Level.SEVERE, e, "Interrupted await termination");
            Thread.currentThread().interrupt();
        } finally {
            awaitLoginLoads();
            if (timeoutCleanup != null) {
                timeoutCleanup.cancelAll();
                this.timeoutCleanup = null;
            }
            connectionTokens.clear();
            loading.clear();
            registry.clear();
        }
    }

    private void awaitLoginLoads() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);

        while (!loading.isEmpty()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                loadit.log(Level.WARNING, "Timed out waiting for active Loadit login loads to finish");
                return;
            }

            try {
                TimeUnit.MILLISECONDS.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(remaining), 50));
            } catch (InterruptedException e) {
                loadit.log(Level.SEVERE, e, "Interrupted while waiting for active Loadit login loads");
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    void timeoutCleanup(boolean enabled) {
        if (enabled) {
            if (timeoutCleanup == null) timeoutCleanup = new TimeoutCleanup();
        } else if (timeoutCleanup != null) {
            timeoutCleanup.cancelAll();
            timeoutCleanup = null;
        }
    }

    long createConnectionToken() {
        return connectionTokenGenerator.incrementAndGet();
    }

    void scheduleCleanup(UUID uuid, String name, long connectionToken) {
        TimeoutCleanup timeoutCleanup = this.timeoutCleanup;
        if (timeoutCleanup != null) timeoutCleanup.schedule(uuid, name, connectionToken);
    }

    private void cancelCleanup(UUID uuid) {
        TimeoutCleanup timeoutCleanup = this.timeoutCleanup;
        if (timeoutCleanup != null) timeoutCleanup.cancel(uuid);
    }

    private void cancelCleanup(UUID uuid, long connectionToken) {
        TimeoutCleanup timeoutCleanup = this.timeoutCleanup;
        if (timeoutCleanup != null) timeoutCleanup.cancel(uuid, connectionToken);
    }

    void removeData(UUID uuid, boolean session) {
        cancelCleanup(uuid);
        connectionTokens.remove(uuid);
        notifyUnload(registry.removeData(uuid, session));
    }

    void removeConnectionData(UUID uuid, long connectionToken, boolean session) {
        cancelCleanup(uuid, connectionToken);
        // Another connection may have already replaced this UUID's data.
        if (!connectionTokens.remove(uuid, connectionToken)) return;
        notifyUnload(registry.removeData(uuid, session));
    }

    private void notifyUnload(@Nullable D userData) {
        if (userData != null) notifyListeners(listener -> listener.onUnload(userData));
    }

    private boolean isConnectionTokenActive(UUID uuid, long connectionToken) {
        Long activeToken = connectionTokens.get(uuid);
        return activeToken != null && activeToken == connectionToken;
    }

    LoadResult loadData(UUID uuid, String name) {
        return loadData(uuid, name, NO_CONNECTION_TOKEN);
    }

    LoadResult loadData(UUID uuid, String name, long connectionToken) {
        boolean addedLoading = false;

        try {
            if (registry.hasData(uuid)) return LoadResult.ALREADY_LOADED;

            addedLoading = loading.add(uuid);
            if (!addedLoading) return LoadResult.ALREADY_LOADING;
            if (stopping) return LoadResult.ERROR;

            return loadDataAfterLock(uuid, name, connectionToken);
        } catch (LoadFailureException e) {
            return LoadResult.error(e);
        } catch (Exception e) {
            loadit.log(Level.SEVERE, e, "Unable to get or create " + uuid + " " + name + " data");
            return LoadResult.error(e);
        } finally {
            if (addedLoading) loading.remove(uuid);
        }
    }

    private LoadResult loadDataAfterLock(UUID uuid, String name, long connectionToken) {
        if (registry.hasData(uuid)) return LoadResult.ALREADY_LOADED;
        if (!callListeners(listener -> listener.onPreLoad(uuid, name))) return LoadResult.ERROR;

        D userData = loader.getOrCreate(uuid, name);
        if (userData == null) return LoadResult.ERROR;

        storeData(uuid, name, userData, connectionToken);
        if (callListeners(listener -> listener.onPostLoad(userData))) return LoadResult.LOADED;

        removeData(uuid, false);
        return LoadResult.ERROR;
    }

    private void storeData(UUID uuid, String name, D userData, long connectionToken) {
        if (connectionToken != NO_CONNECTION_TOKEN) connectionTokens.put(uuid, connectionToken);

        D previousValue = registry.putData(uuid, userData);
        if (previousValue != null) loadit.log(Level.WARNING, uuid + " " + name + " was already loaded!");
    }

    LoadResult setupPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        D userData = registry.data(uuid);
        if (userData == null) return LoadResult.NOT_LOADED;

        S session;
        try {
            session = loader.startSession(userData, player);
        } catch (LoadFailureException e) {
            return LoadResult.error(e);
        } catch (Exception e) {
            loadit.log(Level.SEVERE, e, "Unable to start " + uuid + " " + player.getName() + " session");
            return LoadResult.error(e);
        }

        if (session == null) return LoadResult.NOT_LOADED;

        registry.putSession(uuid, session);
        cancelCleanup(uuid);

        if (!callListeners(listener -> listener.onSessionStart(userData, session))) {
            registry.removeSession(uuid);
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

    private final class TimeoutCleanup {
        private final ConcurrentMap<UUID, CleanupTask> cleanupTasks = new ConcurrentHashMap<>();
        private final AtomicLong tokens = new AtomicLong();

        void schedule(UUID uuid, String name, long connectionToken) {
            long cleanupToken = tokens.incrementAndGet();
            BukkitTask bukkitTask = loadit.plugin().getServer().getScheduler().runTaskLater(loadit.plugin(), () -> cleanup(uuid, name, cleanupToken), CLEANUP_TIMEOUT_SECONDS * 20L);
            CleanupTask cleanupTask = new CleanupTask(cleanupToken, connectionToken, bukkitTask);

            CleanupTask previous = cleanupTasks.put(uuid, cleanupTask);
            if (previous != null) previous.task().cancel();
        }

        private void cleanup(UUID uuid, String name, long cleanupToken) {
            CleanupTask cleanupTask = cleanupTasks.get(uuid);
            if (cleanupTask == null || cleanupTask.cleanupToken() != cleanupToken) return;

            // Do not let an old timeout remove a newer pre-login attempt.
            if ((cleanupTask.connectionToken() != NO_CONNECTION_TOKEN && !isConnectionTokenActive(uuid, cleanupTask.connectionToken()))
                    || !registry.hasData(uuid)
                    || registry.hasSession(uuid)) {
                cleanupTasks.remove(uuid, cleanupTask);
                return;
            }

            if (!cleanupTasks.remove(uuid, cleanupTask)) return;

            loadit.log(Level.WARNING, "Removing stale pre-login data for " + uuid + " (" + name + ") after " + CLEANUP_TIMEOUT_SECONDS + "s without a session");
            if (cleanupTask.connectionToken() == NO_CONNECTION_TOKEN) {
                removeData(uuid, true);
            } else {
                removeConnectionData(uuid, cleanupTask.connectionToken(), true);
            }
        }

        void cancel(UUID uuid) {
            CleanupTask task = cleanupTasks.remove(uuid);
            cancel(task);
        }

        void cancel(UUID uuid, long connectionToken) {
            CleanupTask task = cleanupTasks.get(uuid);
            if (task == null || task.connectionToken() != connectionToken) return;

            if (cleanupTasks.remove(uuid, task)) cancel(task);
        }

        void cancelAll() {
            cleanupTasks.values().forEach(this::cancel);
            cleanupTasks.clear();
        }

        private void cancel(@Nullable CleanupTask task) {
            if (task != null) task.task().cancel();
        }

        private record CleanupTask(long cleanupToken, long connectionToken, BukkitTask task) {
        }
    }
}
