package org.gps.haiku.vlcj.utils;

import org.gps.haiku.utils.OSInfo;
import org.gps.haiku.utils.PropertyManager;

import java.util.Objects;

import static org.gps.haiku.utils.PropertyManager.buildFullPathFromRelativePath;

/**
 * Created by leogps on 2/25/17.
 */
public class YoutubeDLUtils {

    public static String fetchYoutubeDLExecutable() {
        String youtubeDLAbsolutePath = PropertyManager.getProperty("youtube-dl-executable-absolute-path");
        if (youtubeDLAbsolutePath != null) {
            return youtubeDLAbsolutePath;
        }
        String youtubeDLExecutablePath = PropertyManager.getProperty("youtube-dl-executable");
        if (youtubeDLExecutablePath == null) {
            String youtubeDLExecutableName = OSInfo.isOSWin() ? "youtubedl.exe" : "youtubedl";
            return buildFullPathFromRelativePath(youtubeDLExecutableName);
        }
        if (PropertyManager.isRelativePath(youtubeDLExecutablePath)) {
            return buildFullPathFromRelativePath(youtubeDLExecutablePath);
        }
        return youtubeDLExecutablePath;
    }

    public static String fetchAdditionalArgs() {
        String youtubeDLAdditionalArgs = PropertyManager.getProperty("youtube-dl.additional-args");
        return Objects.requireNonNullElse(youtubeDLAdditionalArgs, "");
    }

}