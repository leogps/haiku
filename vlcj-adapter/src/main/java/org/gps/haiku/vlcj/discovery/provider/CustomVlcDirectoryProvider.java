package org.gps.haiku.vlcj.discovery.provider;

import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider;
import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryProviderPriority;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class CustomVlcDirectoryProvider implements DiscoveryDirectoryProvider {

    private static final Logger LOGGER = LogManager.getLogger(CustomVlcDirectoryProvider.class);

    private static final Set<String> DIR_CACHE = new HashSet<String>();
    @Override
    public int priority() {
        // highest priority
        return DiscoveryProviderPriority.CONFIG_FILE + 1;
    }

    @Override
    public String[] directories() {
        return DIR_CACHE.toArray(new String[0]);
    }

    @Override
    public boolean supported() {
        // Supported for all flavors.
        return true;
    }

    public static void addToCache(String... directories) {
        if (!isValid(directories)) {
            LOGGER.warn("Invalid vlc config. Not providing vlc directories, skipping...");
            return;
        }
        LOGGER.info("Adding vlc paths to dir.cache... " + Arrays.toString(directories));
        if (directories == null) {
            return;
        }
        Collections.addAll(DIR_CACHE, directories);
    }

    private static boolean isValid(String[] directories) {
        if (directories == null || directories.length < 1) {
            return false;
        }
        for (String directory: directories) {
            if (directory == null) {
                return false;
            }
        }
        return true;
    }
}
