package it.ytnoos.loadit.api;

import java.util.UUID;

public interface LoaditLoadListener<D, S> {

    /**
     * @return true to continue, false to abort the load process
     */
    default boolean onPreLoad(UUID uuid, String name) {
        return true;
    }

    /**
     * @return true to continue, false to abort the load process
     */
    default boolean onPostLoad(D userData) {
        return true;
    }

    default void onUnload(D userData) {
    }

    /**
     * @return true to continue, false to abort the setup process
     */
    default boolean onSessionStart(D userData, S session) {
        return true;
    }
}
