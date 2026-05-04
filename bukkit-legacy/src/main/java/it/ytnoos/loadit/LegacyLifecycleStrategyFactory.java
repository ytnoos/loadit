package it.ytnoos.loadit;

/**
 * Factory for the legacy Bukkit/Spigot lifecycle strategy.
 */
public final class LegacyLifecycleStrategyFactory implements LoaditLifecycleStrategyFactory {

    @Override
    public <D, S> LoaditLifecycleStrategy create(LoaditImpl<D, S> loadit, AccessListener<D, S> accessListener, LoaditDataRegistry<D, S> registry) {
        return new LegacyLifecycleStrategy<>(loadit, registry);
    }
}
