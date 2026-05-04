package it.ytnoos.loadit;

import org.bukkit.event.Listener;

import java.util.List;

final class ModernPaperLifecycleStrategy<D, S> implements LoaditLifecycleStrategy {

    private final PaperConnectionCloseListener<D, S> connectionCloseListener;
    private final ModernJoinListener<D, S> joinListener;

    ModernPaperLifecycleStrategy(LoaditImpl<D, S> loadit, AccessListener<D, S> accessListener, LoaditDataRegistry<D, S> registry, LoaditLoadCoordinator<D, S> coordinator) {
        this.connectionCloseListener = new PaperConnectionCloseListener<>(loadit.plugin(), accessListener::connectionClosed);
        this.joinListener = new ModernJoinListener<>(loadit, registry, coordinator);
    }

    @Override
    public List<Listener> listeners() {
        return List.of(connectionCloseListener, joinListener);
    }
}
