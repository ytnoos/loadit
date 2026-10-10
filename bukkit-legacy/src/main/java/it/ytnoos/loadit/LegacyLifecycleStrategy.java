package it.ytnoos.loadit;

import org.bukkit.event.Listener;

import java.util.List;

final class LegacyLifecycleStrategy<D, S> implements LoaditLifecycleStrategy {

    private final LegacyLoginListener<D, S> loginListener;

    LegacyLifecycleStrategy(LoaditImpl<D, S> loadit, LoaditDataRegistry<D, S> registry, LoaditLoadCoordinator<D, S> coordinator) {
        this.loginListener = new LegacyLoginListener<>(loadit, registry, coordinator);
    }

    @Override
    public List<Listener> listeners() {
        return List.of(loginListener);
    }

    @Override
    public boolean usesTimeoutCleanup() {
        return true;
    }
}
