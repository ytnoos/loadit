package it.ytnoos.loadit;

/**
 * Factory for the modern Paper lifecycle strategy.
 */
final class ModernPaperLifecycleStrategyFactory implements LoaditLifecycleStrategyFactory {

    @Override
    public <D, S> LoaditLifecycleStrategy create(LoaditImpl<D, S> loadit,
                                                 AccessListener<D, S> accessListener,
                                                 LoaditDataRegistry<D, S> registry,
                                                 LoaditLoadCoordinator<D, S> coordinator,
                                                 LoaditScheduler scheduler) {
        return new ModernPaperLifecycleStrategy<>(loadit, accessListener, registry, coordinator, scheduler);
    }
}
