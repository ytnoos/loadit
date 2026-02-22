package it.ytnoos.loadit.api;

import java.util.UUID;

public interface LoaditLoadListener<D, S> {

    default void onPreLoad(UUID uuid, String name) {
    }

    default void onPostLoad(D userData) {
    }

    default void onUnload(D userData) {
    }

    default void onSessionStart(D userData, S session) {
    }
}
