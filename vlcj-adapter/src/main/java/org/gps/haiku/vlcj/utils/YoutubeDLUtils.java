package org.gps.haiku.vlcj.utils;

import org.gps.haiku.utils.PropertyManager;

import java.io.File;

/**
 * Created by leogps on 2/25/17.
 */
public class YoutubeDLUtils {

    public static String fetchYoutubeDLExecutable() {
        String youtubeDLAbsolutePath = PropertyManager.getProperty("youtube-dl-executable-absolute-path");
        if(youtubeDLAbsolutePath != null) {
            return youtubeDLAbsolutePath;
        }
        return new File("").getAbsolutePath() + PropertyManager.getProperty("youtube-dl-executable");
    }

    public static String fetchAdditionalArgs() {
        String youtubeDLAdditionalArgs = PropertyManager.getProperty("youtube-dl.additional-args");
        if (youtubeDLAdditionalArgs != null) {
            return youtubeDLAdditionalArgs;
        }
        return "";
    }

}