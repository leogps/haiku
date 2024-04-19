package org.gps.haiku.vlcj.discovery.provider;

import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider;
import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryProviderPriority;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class CustomVlcDirectoryProvider implements DiscoveryDirectoryProvider {

    private static final Logger LOGGER = Logger.getLogger(CustomVlcDirectoryProvider.class.getName());

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
        LOGGER.info("Adding vlc paths to dir.cache... " + Arrays.toString(directories));
        if (directories == null) {
            return;
        }
        Collections.addAll(DIR_CACHE, directories);
    }
}
