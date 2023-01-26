package it.ytnoos.loadit;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface OfflineDataContainer<T extends OfflineUserData> {

    CompletableFuture<Optional<T>> getOfflineData(UUID uuid);
}
