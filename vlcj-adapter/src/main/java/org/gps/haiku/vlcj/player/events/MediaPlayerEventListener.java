/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gps.haiku.vlcj.player.events;

import org.gps.haiku.vlcj.player.HaikuPlayer;
import org.gps.haiku.vlcj.player.playlist.PlaylistItem;

/**
 *
 * @author leogps
 */
public interface MediaPlayerEventListener {

    /**
     * Invoked when media starts playing.
     *
     * @param player
     * @param currentTrack
     */
    void playing(final HaikuPlayer player, final PlaylistItem currentTrack);

    void paused(final HaikuPlayer player, final String location);

    void stopped(final HaikuPlayer player, final String location);

    void finished(final HaikuPlayer player, final String location);

    void onPlayProgressed();
}
