package it.ytnoos.loadit;

import org.bukkit.event.Listener;

final class ModernPaperLifecycleStrategy<D, S> implements LoaditLifecycleStrategy {

    private final PaperConnectionCloseListener<D, S> connectionCloseListener;
    private final ModernJoinListener<D, S> joinListener;

    ModernPaperLifecycleStrategy(LoaditImpl<D, S> loadit, AccessListener<D, S> accessListener, LoaditDataRegistry<D, S> registry) {
        this.connectionCloseListener = new PaperConnectionCloseListener<>(loadit.plugin(), accessListener::connectionClosed);
        this.joinListener = new ModernJoinListener<>(loadit, registry);
    }

    @Override
    public Listener primaryListener() {
        return connectionCloseListener;
    }

    @Override
    public Listener secondaryListener() {
        return joinListener;
    }
}
