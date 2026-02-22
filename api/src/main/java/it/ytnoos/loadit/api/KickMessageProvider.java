package it.ytnoos.loadit.api;

import java.util.UUID;

public interface KickMessageProvider {

    /**
     * Provides a kick message for a given load result, player uuid and name.
     *
     * @param result the load result
     * @param uuid   the player's uuid
     * @param name   the player's name
     * @return the kick message to show to the player
     */
    String getKickMessage(LoadResult result, UUID uuid, String name);
}
