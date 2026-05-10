package it.ytnoos.loadit;

interface LoaditLifecycleStrategyFactory {

    <D, S> LoaditLifecycleStrategy create(LoaditImpl<D, S> loadit,
                                          AccessListener<D, S> accessListener,
                                          LoaditDataRegistry<D, S> registry,
                                          LoaditLoadCoordinator<D, S> coordinator,
                                          LoaditScheduler scheduler);
}
