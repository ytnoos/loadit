package it.ytnoos.loadit;

import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class OfflineUserData {

    protected final UUID uuid;
    protected final long loadTime = System.currentTimeMillis();
    private boolean cache;

    protected OfflineUserData(@NotNull UUID uuid) {
        Preconditions.checkNotNull(uuid);

        this.uuid = uuid;
    }

    @Nullable
    public abstract Player getPlayer();

    public boolean isCache() {
        return cache;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }

    public UUID getUUID() {
        return uuid;
    }

    public long getLoadTime() {
        return loadTime;
    }

}
