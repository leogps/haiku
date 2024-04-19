package org.gps.haiku.vlcj.player.events;

import org.gps.haiku.vlcj.player.playlist.PlaylistItem;

/**
 * Created by leogps on 10/25/17.
 */
public interface NowPlayingListTrackSelectedEventListener {

    void onNowPlayingListTrackSelectedEvent(PlaylistItem playlistItem);

}
