package org.gps.haiku.ui.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gps.haiku.utils.PropertyManager;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.gps.haiku.utils.PropertyManager.OS_SPECIFIC_PROPERTIES_FILE_POINTER;

/**
 * Created by leogps on 12/26/16.
 */
public class AppConfiguration {

    private static final Logger LOGGER = LogManager.getLogger(AppConfiguration.class);

    public void configure() {
        LOGGER.debug("Configuring...");
        initConfig();
    }

    private void initConfig() {
        String propertyFileLocationFromEnv = checkEnvironment();
        if (propertyFileLocationFromEnv != null) {
            PropertyManager.setOverridableProperty(OS_SPECIFIC_PROPERTIES_FILE_POINTER, propertyFileLocationFromEnv);
        } else {
            String propertyFileLocationFromInstallation = checkInstallation();
            if (propertyFileLocationFromInstallation != null) {
                PropertyManager.setOverridableProperty(OS_SPECIFIC_PROPERTIES_FILE_POINTER, propertyFileLocationFromInstallation);
            }
        }
        PropertyManager.loadProperties();
    }

    private String checkInstallation() {
        Properties props = new Properties();
        try {
            props.load(AppConfiguration.class.getResourceAsStream("/application.properties"));
        } catch (IOException e) {
            LOGGER.error("Failed to load application properties.", e);
            return null;
        }
        String osBasedPropConfigFileLocation;
        if((osBasedPropConfigFileLocation = props.getProperty(PropertyManager.OS_SPECIFIC_PROPERTIES_FILE_POINTER)) == null) {
            return null;
        }
        File propertyFile = new File(osBasedPropConfigFileLocation);
        if (!propertyFile.exists() || !propertyFile.isFile()) {
            return null;
        }
        return propertyFile.getAbsolutePath();
    }

    private String checkEnvironment() {
        if (!System.getenv().containsKey(PropertyManager.OS_SPECIFIC_PROPERTIES_FILE_POINTER)) {
            return null;
        }
        String propertyFileLocation = System.getenv().get(PropertyManager.OS_SPECIFIC_PROPERTIES_FILE_POINTER);
        File propertyFile = new File(propertyFileLocation);
        if (!propertyFile.exists() || !propertyFile.isFile()) {
            return null;
        }
        return propertyFile.getAbsolutePath();
    }
}
