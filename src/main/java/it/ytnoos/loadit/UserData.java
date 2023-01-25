package it.ytnoos.loadit;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Unfortunately, many servers are cracked and rely more on names
 * than on offline UUIDs, so I need to provide a universal object that
 * can be used those times when a developer wants to upload data only by name or only by UUID
 */
public class UserData {

    @Nullable protected final UUID uuid;
    @Nullable protected final String name;

    public UserData(@NotNull UUID uuid, @NotNull String name) {
        Preconditions.checkNotNull(uuid);
        Preconditions.checkNotNull(name);

        this.uuid = uuid;
        this.name = name;
    }

    public UserData(@NotNull UUID uuid) {
        Preconditions.checkNotNull(uuid);

        this.uuid = uuid;
        name = null;
    }

    public UserData(@NotNull String name) {
        Preconditions.checkNotNull(name);

        this.name = name;
        uuid = null;
    }

    public Optional<UUID> getUuid() {
        return Optional.ofNullable(uuid);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }
}
