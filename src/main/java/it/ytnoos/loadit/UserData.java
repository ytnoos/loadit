package it.ytnoos.loadit;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class UserData {

    protected final UUID uuid;
    protected final long loadTime = System.currentTimeMillis();

    public UserData(@NotNull UUID uuid) {
        Preconditions.checkNotNull(uuid);

        this.uuid = uuid;
    }

    public UUID getUUID() {
        return uuid;
    }

    public long getLoadTime() {
        return loadTime;
    }
}
