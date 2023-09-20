package org.gps.haiku.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatLightLaf;
import org.gps.haiku.db.ConfigPropertyDao;
import org.gps.haiku.db.DbManager;
import org.gps.haiku.db.DbManagerImpl;
import org.gps.haiku.db.model.ConfigProperty;
import org.gps.haiku.ui.config.AppConfiguration;
import org.gps.haiku.ui.controller.Controller;
import org.gps.haiku.ui.theme.UITheme;
import org.gps.haiku.utils.PropertyManager;
import org.gps.haiku.vlcj.player.events.UIDropTarget;
import org.gps.haiku.ui.events.UIFrameEventListener;
import org.gps.haiku.ui.exceptions.TaskExecutionException;
import org.gps.haiku.ui.splash.SplashAnimator;
import org.gps.haiku.vlcj.VLCJUtils;
import org.gps.haiku.vlcj.player.HaikuPlayer;
import org.gps.haiku.vlcj.player.events.MediaPlayerEventListener;
import org.gps.haiku.vlcj.player.impl.HaikuPlayerImpl;
import org.gps.haiku.vlcj.player.NowPlayingListData;
import org.gps.haiku.utils.OSInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by leogps on 10/4/14.
 */
public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("ui");
    private static final java.util.logging.Logger BASIC_LOGGER
            = java.util.logging.Logger.getLogger(Main.class.getName());

    private static boolean vlcjInitSucceeded = false;

    public static boolean isVlcjInitSucceeded() {
        return vlcjInitSucceeded;
    }

    private static HaikuPlayer haikuPlayer;
    private static final AppConfiguration appConfiguration = new AppConfiguration();

    private static DbManager dbManager = DbManagerImpl.getInstance();

    public static void main(String[] args) {
        if(args != null && args.length > 0) {
            LOGGER.debug("Arguments passed...");
            for(String arg : args) {
                LOGGER.debug(arg);
            }
            LOGGER.debug("End of args.");
        }
        appConfiguration.configure();

        try {
            dbManager.initialize();
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize DB.", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Shut down initiated...");
                try {
                    dbManager.shutdown();
                } catch (SQLException e) {
                    BASIC_LOGGER.log(Level.INFO, e.getMessage());
                }

                LOGGER.info("Shutting down the application... GoodBye!");
                BASIC_LOGGER.log(Level.INFO, "Shutting down the application... GoodBye!");
            }
        }));

        if(OSInfo.isOSMac()) {
            // take the menu bar off the jframe
            System.setProperty("apple.laf.useScreenMenuBar", "true");

            String name = RESOURCE_BUNDLE.getString("name");
            // set the name of the application menu item
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", name);
            ImageIcon imageIcon = new ImageIcon(Objects.requireNonNull(Main.class.getClassLoader().getResource("images/haiku.png")));
            try {
                Object instance = Class.forName("com.apple.eawt.Application").getMethod("getApplication").invoke(null);
                Class.forName("com.apple.eawt.Application").getMethod("setDockIconImage", Image.class).invoke(instance, imageIcon.getImage());
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
            try {
                Class.forName("org.gps.haiku.macos.utils.MacOsUtils");
            } catch (ClassNotFoundException e) {
                LOGGER.debug(e.getMessage(), e);
            }
        }
        enableTheming();
        final SplashAnimator splashAnimator = new SplashAnimator();

        /*
         * Create and display the form
         */
        try {
            splashAnimator.renderSplashFrame(10, "Checking VLC Engine...");
            vlcjInitSucceeded = VLCJUtils.isVlcInitSucceeded();
            splashAnimator.renderSplashFrame(15, "VLC Engine loaded successfully.");
        } catch(Exception | UnsatisfiedLinkError ex) {
            LOGGER.error("VLCJ initialization failed!!", ex);
            splashAnimator.renderSplashFrame(15, "VLC initialization failed.");
        }

        SwingUtilities.invokeLater(() -> {
            try {

                splashAnimator.renderSplashFrame(15, "Reading properties...");
                Map<String, String> configurationMap = PropertyManager.getConfigurationMap();

                splashAnimator.renderSplashFrame(20, "Logging properties...");
                LOGGER.debug("Dumping properties read: ");
                for(Object keyObj : configurationMap.keySet()) {
                    String key = (String) keyObj;
                    LOGGER.debug(key + ": " + configurationMap.get(key));
                }

                splashAnimator.renderSplashFrame(20, "Initializing UI Frames...");
                final UIFrame uiFrame = new UIFrame();
                if(OSInfo.isOSMac()) {
                    try {

                        Object instance = Class.forName("com.apple.eawt.Application").getMethod("getApplication").invoke(null);
                        Class.forName("com.apple.eawt.Application").getMethod("requestUserAttention", boolean.class).invoke(instance, true);
                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                }
                uiFrame.setDropTarget(new UIDropTarget() {
                    @Override
                    public void onFilesDroppedEvent(List<File> fileList, DropTargetDropEvent dropTargetDropEvent) {
                        getPlayer().playFiles(fileList);
                    }

                    @Override
                    public void onException(Exception e, DropTargetDropEvent dropTargetDropEvent) {
                        JOptionPane.showMessageDialog(uiFrame, "Action could not be performed at the moment, please see logs for more info.");
                    }
                });
                uiFrame.setState(Frame.MAXIMIZED_BOTH);
//                    if(OSInfo.isOSMac()) {
//                        try {
//                            Class.forName("com.apple.eawt.FullScreenUtilities").getMethod("setWindowCanFullScreen", Window.class, boolean.class)
//                                    .invoke(null, uiFrame, true);
//                        } catch (Exception ex) {
//                            LOG.error(ex.getMessage(), ex);
//                        }
//                    }

                splashAnimator.renderSplashFrame(25, "Initializing Controller...");
                final Controller controller = new Controller(uiFrame);

                // Initializing audio player on the UIFrame.
                if(isVlcjInitSucceeded()) {

                    splashAnimator.renderSplashFrame(35, "Initializing Media Player...");
                    try {
                        haikuPlayer = new HaikuPlayerImpl(uiFrame.getPlayerControlPanel());

                        splashAnimator.renderSplashFrame(40, "Registering Media Player Event Listeners...");
                        controller.registerPlayerEventListener();

                        haikuPlayer.addMediaPlayerListener(new MediaPlayerEventListener() {
                            @Override
                            public void playing(HaikuPlayer player, final NowPlayingListData currentTrack) {
                                uiFrame.getNowPlayingPanel().getTrackNameLabel().setText(currentTrack.getName());
                                uiFrame.getNowPlayingPanel().getTrackNameLabel().setToolTipText(currentTrack.getName());

                                uiFrame.getNowPlayingPanel().getTrackAlbumNameLabel().setText(currentTrack.getAlbum());
                                uiFrame.getNowPlayingPanel().getTrackAlbumNameLabel().setToolTipText(currentTrack.getAlbum());

                                uiFrame.getNowPlayingPanel().getTrackArtistNameLabel().setText(currentTrack.getArtist());
                                uiFrame.getNowPlayingPanel().getTrackArtistNameLabel().setToolTipText(currentTrack.getArtist());

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        uiFrame.getFileBrowserTree().attemptToShowInTree(currentTrack.getLocation());
                                    }
                                }).start();
                            }

                            @Override
                            public void paused(HaikuPlayer player, String location) {

                            }

                            @Override
                            public void stopped(HaikuPlayer player, String location) {

                            }

                            @Override
                            public void finished(HaikuPlayer player, String location) {
                                LOGGER.debug("Play finished");

                            }

                            @Override
                            public void onPlayProgressed() {

                            }
                        });

                    } catch(Exception ex) {
                        vlcjInitSucceeded = false;
                        LOGGER.error("MediaPlayer Initialization failed.", ex);
                    } catch(Error ex) {
                        vlcjInitSucceeded = false;
                        LOGGER.error("MediaPlayer Initialization failed.", ex);
                    }

                }

                splashAnimator.renderSplashFrame(50, "Registering UI Event Listeners...");
                uiFrame.addUIFrameEventListener(new UIFrameEventListener() {

                    @Override
                    public void onSearch(String searchQuery, UIFrame uiFrame) {
                        try {
                            controller.search(searchQuery);
                        } catch (TaskExecutionException ex) {
                            LOGGER.error(ex);
                            LOGGER.info("Error occurred when searching for Tracks.");
                        }
                    }

                    @Override
                    public void onFileOpenRequested() {
                        controller.handleFileOpenRequest();
                    }

                    @Override
                    public void onNetworkFileOpenRequested() {
                        controller.handleNetworkFileOpenRequest();
                    }
                });

                splashAnimator.renderSplashFrame(60, "Parsing assets...");
                controller.takeControl();

                splashAnimator.renderSplashFrame(95, "Showing UI...");
                if(!isVlcjInitSucceeded()) {
                    uiFrame.getPlayerControlPanel().setVisible(false);
                } else {
                    uiFrame.getPlayerControlPanel().setVisible(true);
                }

                splashAnimator.renderSplashFrame(100, "Done.");
                splashAnimator.close();

                uiFrame.setVisible(true);
                uiFrame.toFront();

            } catch (final TaskExecutionException tee) {
                LOGGER.error(tee);
                LOGGER.info(UIFrame.ERROR_OCCURRED_MSG);
            } catch (IOException e) {
                LOGGER.error(e);
            }
        });
    }

    private static void enableTheming() {
        UITheme savedTheme = null;
        try {
            savedTheme = retrieveSavedUIThemeProperty();
        } catch (Exception ex) {
            LOGGER.warn("Could not retrieve saved theme", ex);
        }
        if (savedTheme != null) {
            LOGGER.info("Found saved theme: " + savedTheme.getName());
        } else {
            LOGGER.info("No saved theme found. Using default theme.");
            savedTheme = new UITheme();
            savedTheme.setName("Light Flat");
            savedTheme.setClassName("com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme");
        }
        try {
            UIManager.setLookAndFeel( new FlatLightLaf() );
            Method method = Class.forName(savedTheme.getClassName()).getDeclaredMethod("setup");
            method.invoke(null);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            try {
                LOGGER.info("Falling back to system default look and feel...");
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
    }

    private static UITheme retrieveSavedUIThemeProperty() throws SQLException, IOException {
        ConfigPropertyDao configPropertyDao = fetchConfigPropertyDao();
        if(configPropertyDao == null) {
            return null;
        }

        ConfigProperty configProperty = configPropertyDao.findByKey("ui_theme");
        if (configProperty == null) {
            return null;
        }
        return new ObjectMapper().readValue(configProperty.getValue(), UITheme.class);
    }

    private static ConfigPropertyDao fetchConfigPropertyDao() {
        if(!DbManagerImpl.getInstance().isInitiated()) {
            return null;
        }
        return new ConfigPropertyDao(DbManagerImpl.getInstance().getConnection());
    }

    private static Integer retrieveFontSize() {
        if(!dbManager.isInitiated()) {
            return null;
        }
        try {
            ConfigPropertyDao configPropertyDao = new ConfigPropertyDao(dbManager.getConnection());
            ConfigProperty configProperty = configPropertyDao.findByKey("font_size");
            if(configProperty == null) {
                LOGGER.debug("Font size property not found.");
                return null;
            }

            try {
                return Integer.parseInt(configProperty.getValue());
            } catch (NumberFormatException e) {
                LOGGER.error("font_size is not valid.", e);
            }
        } catch (Exception ex) {
            LOGGER.debug("Could not retrieve font_size", ex);
        }
        return null;
    }

    public static void setDefaultSize(int size) {
        Set<Object> keySet = UIManager.getLookAndFeelDefaults().keySet();
        Object[] keys = keySet.toArray(new Object[keySet.size()]);

        for (Object key : keys) {

            if (key != null && key.toString().toLowerCase().contains("font")) {

                LOGGER.info("Updating font: " + key);
                Font font = UIManager.getDefaults().getFont(key);
                if (font != null) {
                    font = font.deriveFont((float)size);
                    UIManager.put(key, font);
                }

            }

        }

    }

    public static HaikuPlayer getPlayer() {
        return haikuPlayer;
    }
}
