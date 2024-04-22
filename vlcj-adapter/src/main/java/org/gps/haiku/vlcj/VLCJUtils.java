package org.gps.haiku.vlcj;

import lombok.Getter;
import org.gps.haiku.utils.PropertyManager;
import org.gps.haiku.vlcj.discovery.NativeDiscoveryStrategyResolver;
import org.gps.haiku.vlcj.discovery.provider.CustomVlcDirectoryProvider;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import uk.co.caprica.vlcj.binding.lib.LibVlc;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

import java.io.File;

public class VLCJUtils {

    @Getter
    private static boolean vlcInitSucceeded = false;

    private static final Logger LOGGER = LogManager.getLogger(VLCJUtils.class);

    static {
        String osArch = System.getProperty("os.arch");
        String vlcPathProperty = String.format("vlc-%s", osArch);
        String vlcPluginsPathProperty = String.format("vlc-%s-plugins", osArch);

        LOGGER.info("VlcPathProperty: {}", vlcPathProperty);
        LOGGER.info("VlcPluginsPathProperty: {}", vlcPluginsPathProperty);

        String currentFolderPath = new File("").getAbsolutePath();
        String vlcPath = PropertyManager.getProperty(vlcPathProperty);
        if (PropertyManager.isRelativePath(vlcPath)) {
            vlcPath = String.join(File.separator, currentFolderPath, vlcPath);
        }
        LOGGER.info("VlcPath: {}", vlcPath);

        String vlcPluginsPath = PropertyManager.getProperty(vlcPluginsPathProperty);
        if (PropertyManager.isRelativePath(vlcPluginsPath)) {
            vlcPluginsPath = String.join(File.separator, currentFolderPath, vlcPluginsPath);
        }
        LOGGER.info("VlcPluginsPath: {}", vlcPluginsPath);

        try {
            System.setProperty("jna.debug_load", "true");

            CustomVlcDirectoryProvider.addToCache(vlcPath, vlcPluginsPath);
            vlcInitSucceeded = new NativeDiscovery(NativeDiscoveryStrategyResolver.resolve())
                    .discover();

            LOGGER.info("vlc native library path set to:" + vlcPath);

            if (LibVlc.libvlc_get_version() != null) {
                vlcInitSucceeded = true;
            }
        } catch (UnsatisfiedLinkError | Exception ex) {
            LOGGER.warn("Failed to link binaries.", ex);
            triggerNativeDiscovery(ex);
        } finally {
            if(vlcInitSucceeded) {
                LOGGER.info(String.format("VLC Engine version %s", getVlcVersion()));
                LOGGER.info(String.format("VLCJ version %s", getVlcJVersion()));
            }
        }

    }

    private static void triggerNativeDiscovery(Throwable ex) {
        LOGGER.warn("Failed to load VLC from provided path.", ex);
        LOGGER.info("Switching to Automatic Discovery.");
        vlcInitSucceeded = new NativeDiscovery().discover();
        if(vlcInitSucceeded) {
            LOGGER.info("Auto discovery of VLC libraries succeeded.");
        }
    }

    public static String getVlcVersion() {
        return LibVlc.libvlc_get_version();
    }

    public static String getVlcJVersion() {
        return "4.7.1";
    }
}
