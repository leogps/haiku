package org.gps.haiku.vlcj.utils;

import org.gps.haiku.utils.OSInfo;
import org.gps.haiku.utils.PropertyManager;

import java.io.File;
import java.util.Objects;

/**
 * Created by leogps on 2/25/17.
 */
public class YoutubeDLUtils {

    public static String fetchYoutubeDLExecutable() {
        String youtubeDLAbsolutePath = PropertyManager.getProperty("youtube-dl-executable-absolute-path");
        if (youtubeDLAbsolutePath != null) {
            return youtubeDLAbsolutePath;
        }
        String currentFolderPath = new File("").getAbsolutePath();
        String youtubeDLExecutablePath = PropertyManager.getProperty("youtube-dl-executable");
        if (youtubeDLExecutablePath == null) {
            return String.join(File.separator, currentFolderPath, OSInfo.isOSWin()
                    ? "youtubedl.exe" : "youtubedl");
        }
        if (PropertyManager.isRelativePath(youtubeDLExecutablePath)) {
            return String.join(File.separator, currentFolderPath, youtubeDLExecutablePath);
        }
        return youtubeDLExecutablePath;
    }

    public static String fetchAdditionalArgs() {
        String youtubeDLAdditionalArgs = PropertyManager.getProperty("youtube-dl.additional-args");
        return Objects.requireNonNullElse(youtubeDLAdditionalArgs, "");
    }

}