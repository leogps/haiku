package org.gps.haiku.ytdlp;

import lombok.Data;

/**
 * @author leogps
 * Created on 4/19/24
 */
@Data
public class YoutubeVideo {
    private final String url;
    private final YoutubeDLResult youtubeDLResult;
    private final boolean playlist;
}
