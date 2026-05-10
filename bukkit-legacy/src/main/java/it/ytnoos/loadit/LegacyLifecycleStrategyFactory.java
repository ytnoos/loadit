package it.ytnoos.loadit;

/**
 * Factory for the legacy Bukkit/Spigot lifecycle strategy.
 */
final class LegacyLifecycleStrategyFactory implements LoaditLifecycleStrategyFactory {

    @Override
    public <D, S> LoaditLifecycleStrategy create(LoaditImpl<D, S> loadit,
                                                 AccessListener<D, S> accessListener,
                                                 LoaditDataRegistry<D, S> registry,
                                                 LoaditLoadCoordinator<D, S> coordinator,
                                                 LoaditScheduler scheduler) {
        return new LegacyLifecycleStrategy<>(loadit, registry, coordinator);
    }
}
