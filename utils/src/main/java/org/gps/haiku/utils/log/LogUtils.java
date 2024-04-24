package org.gps.haiku.utils.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.gps.haiku.utils.PropertyManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.gps.haiku.utils.PropertyManager.*;

/**
 * @author leogps
 * Created on 4/23/24
 */
public class LogUtils {

    public static void checkAndInitLog4J() throws IOException {
        String log4jConfigLocation = PropertyManager.lookupConfiguredApplicationProperty(LOG4J_CONFIG_LOCATION);
        if (log4jConfigLocation == null) {
            return;
        }
        File configFile;
        if (isRelativePath(log4jConfigLocation)) {
            String path = buildFullPathFromRelativePath(log4jConfigLocation);
            configFile = new File(path);
        } else {
            configFile = new File(log4jConfigLocation);
        }
        if (!configFile.exists() || !configFile.isFile()) {
            return;
        }
        ConfigurationSource source = new ConfigurationSource(new FileInputStream(configFile));
        Configurator.initialize(null, source);
        LogManager.getLogger(LogUtils.class)
                .info("Log4j configuration loaded successfully from {}", configFile.getAbsolutePath());
    }
}
