package it.ytnoos.loadit;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaperConnectionCloseListenerTest {

    private static final UUID PLAYER = UUID.randomUUID();

    private final List<UUID> cleanups = new CopyOnWriteArrayList<>();
    private final PaperConnectionCloseListener<Object, Object> listener = new PaperConnectionCloseListener<>(new ImmediateScheduler(), (uuid, name) -> cleanups.add(uuid));

    // The quit already removed the data, which a newer login may own by the time the close arrives.
    @Test
    void closeAfterQuitDoesNotCleanUp() {
        listener.onQuit(new PlayerQuitEvent(player(), "quit"));
        listener.onConnectionClose(close());

        assertEquals(List.of(), cleanups);
    }

    @Test
    void closeOfLoginThatNeverJoinedCleansUp() {
        listener.onConnectionClose(close());

        assertEquals(List.of(PLAYER), cleanups);
    }

    @Test
    void eachQuitSkipsOnlyItsOwnClose() {
        listener.onQuit(new PlayerQuitEvent(player(), "quit"));
        listener.onConnectionClose(close());
        listener.onConnectionClose(close());

        assertEquals(List.of(PLAYER), cleanups);
    }

    private static PlayerConnectionCloseEvent close() {
        return new PlayerConnectionCloseEvent(PLAYER, "player", InetAddress.getLoopbackAddress(), false);
    }

    private static Player player() {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (self, method, args) -> method.getName().equals("getUniqueId") ? PLAYER : null);
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
