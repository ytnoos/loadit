package it.ytnoos.loadit;

import java.util.UUID;

public interface LoaditLoadListener<T extends UserData> {

    default void onPreLoad(UUID uuid, String name) {
    }

    default void onPostLoad(T userData) {
    }

    default void onUnload(T userData) {
    }
}
