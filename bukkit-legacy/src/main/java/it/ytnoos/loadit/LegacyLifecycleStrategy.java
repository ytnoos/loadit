package it.ytnoos.loadit;

import org.bukkit.event.Listener;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

final class LegacyLifecycleStrategy<D, S> implements LoaditLifecycleStrategy {

    // An allowed pre-login reaches PlayerLoginEvent on the next server tick.
    private static final long LOGIN_TICKS = 3L;

    private final LegacyLoginListener<D, S> loginListener;
    private final LoaditLoadCoordinator<D, S> coordinator;
    private final LoaditScheduler scheduler;

    LegacyLifecycleStrategy(LoaditImpl<D, S> loadit, LoaditDataRegistry<D, S> registry, LoaditLoadCoordinator<D, S> coordinator, LoaditScheduler scheduler) {
        this.loginListener = new LegacyLoginListener<>(loadit, registry, coordinator);
        this.coordinator = coordinator;
        this.scheduler = scheduler;
    }

    @Override
    public List<Listener> listeners() {
        return List.of(loginListener);
    }

    @Override
    public boolean usesTimeoutCleanup() {
        return true;
    }

    // There is no close event: if the login has no session after a few ticks, its connection has closed.
    // Checked on the main thread, like PlayerLoginEvent, so it can't race with the session being created.
    @Override
    public void endLogin(UUID uuid, long connectionToken, @Nullable Object connection) {
        scheduler.runAsyncDelayed(() -> scheduler.runOnGlobalRegion(() -> coordinator.endAbandonedLogin(uuid, connectionToken)), LOGIN_TICKS);
    }
}
