package it.ytnoos.loadit;

import it.ytnoos.loadit.api.DataLoader;
import it.ytnoos.loadit.api.LoadResult;
import it.ytnoos.loadit.api.LoaditLoadListener;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoaditLoadCoordinatorTest {

    private static final UUID PLAYER = UUID.randomUUID();

    private final List<Object> unloaded = new CopyOnWriteArrayList<>();
    private final BlockingQueue<Long> endedLogins = new LinkedBlockingQueue<>();
    private BiConsumer<UUID, Long> onEndLogin = (uuid, token) -> {
    };

    private ExecutorService executor;
    private LoaditDataRegistry<Object, Session> registry;
    private LoaditLoadCoordinator<Object, Session> coordinator;

    private record Session(Object data) {
    }

    @BeforeEach
    void setUp() {
        DataLoader<Object, Session, Player> loader = new DataLoader<>() {
            @Override
            public Object getOrCreate(UUID uuid, String name) {
                return new Object();
            }

            @Override
            public @Nullable Object load(UUID uuid) {
                return null;
            }

            @Override
            public @Nullable Object load(String name) {
                return null;
            }

            @Override
            public Session startSession(Object data, Player player) {
                return new Session(data);
            }
        };

        LoaditLoadListener<Object, Session> unloadRecorder = new LoaditLoadListener<>() {
            @Override
            public void onUnload(Object data) {
                unloaded.add(data);
            }
        };

        executor = Executors.newSingleThreadExecutor();
        registry = new LoaditDataRegistry<>(loader, executor);
        coordinator = new LoaditLoadCoordinator<>(stub(BukkitLoadit.class, "listeners", List.of(unloadRecorder)), loader, registry, executor, new ImmediateScheduler());
        coordinator.strategy(new LoaditLifecycleStrategy() {
            @Override
            public List<Listener> listeners() {
                return List.of();
            }

            @Override
            public void endLogin(UUID uuid, long connectionToken, @Nullable Object connection) {
                endedLogins.add(connectionToken);
                onEndLogin.accept(uuid, connectionToken);
            }
        });
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    // The 1.8 strategy ends a login that has no session a few ticks after it was allowed.
    @Test
    void newLoginEndsLegacyLoginThatNeverJoined() throws Exception {
        long previous = allowedLogin();
        Object previousData = registry.data(PLAYER);
        onEndLogin = coordinator::endAbandonedLogin;

        assertEquals(LoadResult.Type.LOADED, newLogin().get(5, TimeUnit.SECONDS).type());
        assertEquals(previous, endedLogins.poll());
        assertNotSame(previousData, registry.data(PLAYER));
        assertTrue(unloaded.contains(previousData));
    }

    // The modern strategy disconnects the login, whose close event then removes its data.
    @Test
    void newLoginEndsModernLoginThatNeverJoined() throws Exception {
        long previous = allowedLogin();
        onEndLogin = (uuid, token) -> coordinator.removeData(uuid, true);

        assertEquals(LoadResult.Type.LOADED, newLogin().get(5, TimeUnit.SECONDS).type());
        assertEquals(previous, endedLogins.poll());
    }

    // A login that joins while a newer one waits keeps its data and its session is created on that data.
    @Test
    void loginStillJoiningKeepsItsData() throws Exception {
        long previous = allowedLogin();
        Object previousData = registry.data(PLAYER);

        CompletableFuture<LoadResult> newLogin = newLogin();
        assertEquals(previous, endedLogins.poll(5, TimeUnit.SECONDS));

        Player player = stub(Player.class, "getUniqueId", PLAYER);
        assertEquals(LoadResult.Type.LOADED, coordinator.setupPlayer(player).type());
        // the legacy check scheduled by endLogin runs after the join
        coordinator.endAbandonedLogin(PLAYER, previous);

        assertEquals(LoadResult.Type.ALREADY_LOADED, newLogin.get(5, TimeUnit.SECONDS).type());
        assertSame(previousData, registry.data(PLAYER));
        assertSame(previousData, registry.session(player).data());
        assertFalse(unloaded.contains(previousData));
    }

    // A login whose pre-login is still running is never ended: only its own outcome releases the data.
    @Test
    void loginStillInPreLoginIsNotEnded() throws Exception {
        long previous = coordinator.createConnectionToken();
        assertEquals(LoadResult.Type.LOADED, coordinator.loadData(PLAYER, "previous", previous).type());

        CompletableFuture<LoadResult> newLogin = newLogin();
        assertNull(endedLogins.poll(300, TimeUnit.MILLISECONDS));

        // another listener disallows the previous pre-login
        coordinator.removeConnectionData(PLAYER, previous, false);

        assertEquals(LoadResult.Type.LOADED, newLogin.get(5, TimeUnit.SECONDS).type());
        assertTrue(endedLogins.isEmpty());
    }

    @Test
    void loginThatCannotBeEndedStillRefusesNewLogins() throws Exception {
        allowedLogin();
        Object previousData = registry.data(PLAYER);

        assertEquals(LoadResult.Type.ALREADY_LOADED, newLogin().get(10, TimeUnit.SECONDS).type());
        assertSame(previousData, registry.data(PLAYER));
    }

    @Test
    void legacyCheckOfAnOlderLoginKeepsNewerData() {
        long older = allowedLogin();
        coordinator.removeConnectionData(PLAYER, older, false);
        allowedLogin();
        Object newerData = registry.data(PLAYER);

        coordinator.endAbandonedLogin(PLAYER, older);

        assertSame(newerData, registry.data(PLAYER));
    }

    private long allowedLogin() {
        long token = coordinator.createConnectionToken();
        assertEquals(LoadResult.Type.LOADED, coordinator.loadData(PLAYER, "previous", token).type());
        coordinator.preLoginAllowed(PLAYER, token, new AsyncPlayerPreLoginEvent("previous", InetAddress.getLoopbackAddress(), PLAYER));
        return token;
    }

    private CompletableFuture<LoadResult> newLogin() {
        return CompletableFuture.supplyAsync(() -> coordinator.loadData(PLAYER, "new", coordinator.createConnectionToken()));
    }

    // Implements only the given getter; anything else the coordinator calls returns null.
    private static <T> T stub(Class<T> type, String method, Object value) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (self, called, args) -> switch (called.getName()) {
            case "equals" -> self == args[0];
            case "hashCode" -> System.identityHashCode(self);
            default -> called.getName().equals(method) ? value : null;
        });
        return type.cast(proxy);
    }

    private static final class ImmediateScheduler implements LoaditScheduler {
        @Override
        public void runOnGlobalRegion(Runnable task) {
            task.run();
        }

        @Override
        public LoaditScheduledTask runAsyncDelayed(Runnable task, long delayTicks) {
            throw new UnsupportedOperationException();
        }
    }
}
