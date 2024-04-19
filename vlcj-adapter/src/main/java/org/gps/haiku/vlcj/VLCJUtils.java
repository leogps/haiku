package org.gps.haiku.vlcj;

import org.gps.haiku.utils.PropertyManager;
import org.gps.haiku.vlcj.discovery.NativeDiscoveryStrategyResolver;
import org.gps.haiku.vlcj.discovery.provider.CustomVlcDirectoryProvider;

import java.util.logging.Level;
import java.util.logging.Logger;
import uk.co.caprica.vlcj.binding.lib.LibVlc;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

import java.io.File;

public class VLCJUtils {

    private static boolean vlcInitSucceeded = false;

    private static final Logger LOGGER = Logger.getLogger(VLCJUtils.class.getName());

    static {
        String osArch = System.getProperty("os.arch");
        String vlcPath = String.format("vlc-%s", osArch);
        String vlcPluginsPath = String.format("vlc-%s-plugins", osArch);

        LOGGER.info("VlcPath: " + vlcPath);
        LOGGER.info("VlcPluginsPath: " + vlcPluginsPath);

        String path = new File("").getAbsolutePath()
                + PropertyManager.getProperty(vlcPath);

        String pluginsPath = new File("").getAbsolutePath()
                + PropertyManager.getProperty(vlcPluginsPath);

        try {
            System.setProperty("jna.debug_load", "true");

            CustomVlcDirectoryProvider.addToCache(path, pluginsPath);
            vlcInitSucceeded = new NativeDiscovery(NativeDiscoveryStrategyResolver.resolve())
                    .discover();

            LOGGER.info("vlc native library path set to:" + path);

            if (LibVlc.libvlc_get_version() != null) {
                vlcInitSucceeded = true;
            }
        } catch (UnsatisfiedLinkError | Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to link binaries.", ex);
            triggerNativeDiscovery(ex);
        } finally {
            if(vlcInitSucceeded) {
                LOGGER.info(String.format("VLC Engine version %s", getVlcVersion()));
                LOGGER.info(String.format("VLCJ version %s", getVlcJVersion()));
            }
        }

    }

    private static void triggerNativeDiscovery(Throwable ex) {
        LOGGER.log(Level.WARNING, "Failed to load VLC from provided path.", ex);
        LOGGER.info("Switching to Automatic Discovery.");
        vlcInitSucceeded = new NativeDiscovery().discover();
        if(vlcInitSucceeded) {
            LOGGER.info("Auto discovery of VLC libraries succeeded.");
        }
    }

    public static boolean isVlcInitSucceeded() {
        return vlcInitSucceeded;
    }

    public static String getVlcVersion() {
        return LibVlc.libvlc_get_version();
    }

    public static String getVlcJVersion() {
        return "4.7.1";
    }
}
