package org.gps.haiku.macos.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.desktop.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leogps on 7/29/17.
 */
public class MacOsUtils {

    private static final Logger LOGGER = LogManager.getLogger(MacOsUtils.class);

    static {
        new NativeAppUtils();
    }

    public static class NativeAppUtils implements OpenFilesHandler, AboutHandler, PreferencesHandler,
            OpenURIHandler, QuitHandler {

        private NativeAppUtils(){
            try {
                Object instance =
                        Class.forName("com.apple.eawt.Application").getMethod("getApplication").invoke(null);
                Class.forName("com.apple.eawt.Application").getMethod("setAboutHandler", AboutHandler.class).invoke(instance, this);
                Class.forName("com.apple.eawt.Application").getMethod("setOpenFileHandler", OpenFilesHandler.class).invoke(instance, this);
                Class.forName("com.apple.eawt.Application").getMethod("setOpenURIHandler", OpenURIHandler.class).invoke(instance, this);
                Class.forName("com.apple.eawt.Application").getMethod("setPreferencesHandler", PreferencesHandler.class).invoke(instance, this);
                Class.forName("com.apple.eawt.Application").getMethod("setQuitHandler", QuitHandler.class).invoke(instance, this);

            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }

        @Override
        public void handleAbout(AboutEvent aboutEvent) {
            LOGGER.debug("TODO: About Event...");
        }

        @Override
        public void openFiles(OpenFilesEvent openFilesEvent) {
            System.out.println("Open File event triggered...");
            LOGGER.debug("Open File event triggered...");
            LOGGER.debug(openFilesEvent.getSource());
            LOGGER.debug(openFilesEvent.getSearchTerm());
            LOGGER.debug(openFilesEvent.getFiles());
            LOGGER.debug(openFilesEvent.getSource().getClass());

            playFiles(openFilesEvent.getFiles());
        }

        @Override
        public void openURI(OpenURIEvent openURIEvent) {
            System.out.println("Open File event triggered...");
            LOGGER.debug("Open File event triggered...");
            LOGGER.debug(openURIEvent.getSource());
            LOGGER.debug(openURIEvent.getSource().getClass());

            File uriFile = new File(openURIEvent.getURI());
            List<File> fileList = new ArrayList<>();
            fileList.add(uriFile);
            playFiles(fileList);
        }

        private void playFiles(List<File> fileList) {
            try {
                Class<?> clazz = Class.forName("org.gps.haiku.ui.Main");
                Method getPlayerMethod = clazz.getMethod("getPlayer");
                Object player = getPlayerMethod.invoke(null);

                Method playFilesMethod = player.getClass().getMethod("playFiles", List.class);
                playFilesMethod.invoke(player, fileList);
            } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                     IllegalAccessException e) {
                JOptionPane.showMessageDialog(null, "That doesn't work here.");
            }
        }

        @Override
        public void handlePreferences(PreferencesEvent preferencesEvent) {
            LOGGER.debug("Handle Preferences...");
        }

        @Override
        public void handleQuitRequestWith(QuitEvent e, QuitResponse response) {
            LOGGER.debug("Handle Quit...");
        }
    }
}
