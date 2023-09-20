/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gps.haiku.vlcj.player.events;

import org.gps.haiku.vlcj.player.HaikuPlayer;
import org.gps.haiku.vlcj.player.NowPlayingListData;

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
    void playing(final HaikuPlayer player, final NowPlayingListData currentTrack);

    void paused(final HaikuPlayer player, final String location);

    void stopped(final HaikuPlayer player, final String location);

    void finished(final HaikuPlayer player, final String location);

    void onPlayProgressed();
}
