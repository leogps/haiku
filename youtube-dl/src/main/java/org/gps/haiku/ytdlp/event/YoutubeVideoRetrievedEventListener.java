package org.gps.haiku.ytdlp.event;

import org.gps.haiku.ytdlp.YoutubeVideo;

/**
 * @author leogps
 * Created on 4/19/24
 */
public interface YoutubeVideoRetrievedEventListener {

    void onYoutubeVideoRetrieved(YoutubeVideo video);
}
