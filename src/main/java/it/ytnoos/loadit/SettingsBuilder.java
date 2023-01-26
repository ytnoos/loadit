package it.ytnoos.loadit;

import java.util.concurrent.TimeUnit;

public class SettingsBuilder {

    private long maximumCacheTime = TimeUnit.MINUTES.toMillis(15);
    private long cleanerPeriod = TimeUnit.MINUTES.toMillis(1);
    private boolean loadOnlines = true;
    private int loaderPoolSize = 1;

    public SettingsBuilder setMaximumCacheTime(long maximumCacheTime) {
        this.maximumCacheTime = maximumCacheTime;
        return this;
    }

    public SettingsBuilder setCleanerPeriod(long cleanerPeriod) {
        this.cleanerPeriod = cleanerPeriod;
        return this;
    }

    public SettingsBuilder setLoadOnlines(boolean loadOnlines) {
        this.loadOnlines = loadOnlines;
        return this;
    }

    public SettingsBuilder setLoaderPoolSize(int loaderPoolSize) {
        this.loaderPoolSize = loaderPoolSize;
        return this;
    }

    public Settings createSettings() {
        return new Settings(maximumCacheTime, cleanerPeriod, loadOnlines, loaderPoolSize);
    }
}