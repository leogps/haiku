package org.gps.haiku.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertyManager {

    private static final Logger LOGGER = LogManager.getLogger(PropertyManager.class);

    private static final Map<String, String> OVERRIDABLE_PROPERTIES = new HashMap<>();

    private static final Map<String, String> CONFIGURATION_MAP = new HashMap<>();

    public static final String OS_SPECIFIC_PROPERTIES_FILE_POINTER = String.format("haiku.%s.properties.file", OSInfo.getOsPrefix());

    private static final Pattern WIN_ABS_PATH_PATTERN = Pattern.compile("^[a-zA-Z]:(.*)$");

    public static Map<String, String> getConfigurationMap() {
        return CONFIGURATION_MAP;
    }

    public enum Property {

        MAC_PROPERTIES_FILE_PROPERTY("haiku.mac.properties.file", "config/mac-app.properties"),

        WIN_PROPERTIES_FILE_PROPERTY("haiku.win.properties.file", "config/win-app.properties"),

        NIX_PROPERTIES_FILE_PROPERTY("haiku.nix.properties.file", "config/nix-app.properties");

        private final String key;
        private final String defaultValue;

        Property(String key, String defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public String getKey() {
            return key;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        @Override
        public String toString() {
            return name();
        }
    }


    public static void setOverridableProperty(String key, String value) {
        OVERRIDABLE_PROPERTIES.put(key, value);
    }

    public static void loadProperties() {
        String propertiesFilePath = null;
        if (OVERRIDABLE_PROPERTIES.containsKey(OS_SPECIFIC_PROPERTIES_FILE_POINTER)) {
            propertiesFilePath = OVERRIDABLE_PROPERTIES.get(OS_SPECIFIC_PROPERTIES_FILE_POINTER);
        }
        if(propertiesFilePath == null) {
            propertiesFilePath = getDefaultConfigLocation();
        }

        LOGGER.debug("Checking if the config file location is relative path or absolute path...");
        if(isRelativePath(propertiesFilePath)) {
            propertiesFilePath = new File("").getAbsolutePath() + File.separator + propertiesFilePath;
        }

        if(FileUtils.checkFileExistence(propertiesFilePath)) {
            LOGGER.info("Properties file found: " + propertiesFilePath);

            try {

                Properties propertyFileProperties = new Properties();
                propertyFileProperties.load(Files.newInputStream(Paths.get(propertiesFilePath)));

                for(Object keyObj : propertyFileProperties.keySet()) {
                    String key = (String) keyObj;
                    if(!CONFIGURATION_MAP.containsKey(key)) {
                        CONFIGURATION_MAP.put(key, propertyFileProperties.getProperty(key));
                        LOGGER.info(String.format("Using Property from the properties file: {%s : %s}", key, propertyFileProperties.getProperty(key)));
                    }
                }

            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }

        } else {

            LOGGER.warn("Properties file not found: " + propertiesFilePath);
            LOGGER.warn("Properties file needs to be placed at ${currentDirectory}/config/mac-app.properties for OSX and " +
                    "${currentDirectory}/config/win-app.properties for Windows.");
            LOGGER.warn("${currentDirectory} represents the invocation directory.");

            LOGGER.info("Asserting the information is passed through System.Properties.");
        }
    }

    public static boolean isRelativePath(String propertiesFilePath) {
        if (!OSInfo.isOSWin()) {
            return !propertiesFilePath.startsWith(File.separator);
        }
        final Matcher matcher = WIN_ABS_PATH_PATTERN.matcher(propertiesFilePath);
        return !matcher.matches();
    }

    private static String getDefaultConfigLocation() {
        if (OSInfo.isOSWin()) {
            return Property.WIN_PROPERTIES_FILE_PROPERTY.getDefaultValue();
        }
        if(OSInfo.isOSMac()){
            return Property.MAC_PROPERTIES_FILE_PROPERTY.getDefaultValue();
        }
        return Property.NIX_PROPERTIES_FILE_PROPERTY.getDefaultValue();
    }

    public static String getProperty(String key) {
        return CONFIGURATION_MAP.get(key);
    }

    public static boolean containsProperty(String key) {
        return CONFIGURATION_MAP.containsKey(key);
    }
}
