package it.ytnoos.loadit;

import org.bukkit.event.Listener;

final class LegacyLifecycleStrategy<D, S> implements LoaditLifecycleStrategy {

    private final LegacyLoginListener<D, S> loginListener;

    LegacyLifecycleStrategy(LoaditImpl<D, S> loadit, LoaditDataRegistry<D, S> registry) {
        this.loginListener = new LegacyLoginListener<>(loadit, registry);
    }

    @Override
    public Listener primaryListener() {
        return loginListener;
    }

    @Override
    public boolean usesTimeoutCleanup() {
        return true;
    }
}
