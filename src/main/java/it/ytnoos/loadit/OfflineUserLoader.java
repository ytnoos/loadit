package it.ytnoos.loadit;

import java.util.UUID;

public interface OfflineUserLoader<T extends OfflineUserData> {

    T loadOfflineData(UUID uuid, String name);

    T loadOfflineData(UUID uuid);
}
