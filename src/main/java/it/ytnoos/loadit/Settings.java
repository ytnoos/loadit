package it.ytnoos.loadit;

public class Settings {

    private final long maximumCacheTime;
    private final long cleanerPeriod;
    private final boolean loadOnlines;
    private final int loaderPoolSize;

    public Settings(long maximumCacheTime, long cleanerPeriod, boolean loadOnlines, int loaderPoolSize) {
        this.maximumCacheTime = maximumCacheTime;
        this.cleanerPeriod = cleanerPeriod;
        this.loadOnlines = loadOnlines;
        this.loaderPoolSize = loaderPoolSize;
    }

    public long getMaximumCacheTime() {
        return maximumCacheTime;
    }

    public long getCleanerPeriod() {
        return cleanerPeriod;
    }

    public boolean isLoadOnlines() {
        return loadOnlines;
    }

    public int getLoaderPoolSize() {
        return loaderPoolSize;
    }
}
