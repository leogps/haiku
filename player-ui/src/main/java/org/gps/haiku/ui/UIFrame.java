package org.gps.haiku.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import org.gps.haiku.utils.Constants;
import org.gps.haiku.utils.JavaVersionUtils;
import org.gps.haiku.utils.OSInfo;
import org.gps.haiku.utils.PropertyManager;
import org.gps.haiku.utils.ui.ApplicationExitHandler;
import org.gps.haiku.utils.ui.AsyncTaskListener;
import org.gps.haiku.utils.ui.InterruptableAsyncTask;
import org.gps.haiku.utils.ui.InterruptableProcessDialog;
import org.gps.haiku.utils.ui.fileutils.FileBrowserTree;
import org.gps.haiku.db.ConfigPropertyDao;
import org.gps.haiku.db.DbManagerImpl;
import org.gps.haiku.db.model.ConfigProperty;
import org.gps.haiku.ui.components.TracksContextMenu;
import org.gps.haiku.ui.events.UIFrameEventListener;
import org.gps.haiku.ui.handlers.StatusMessageAppender;
import org.gps.haiku.ui.tablehelpers.models.PlaylistTableModel;
import org.gps.haiku.ui.tablehelpers.models.TracksTableModel;
import org.gps.haiku.ui.theme.UITheme;
import org.gps.haiku.ui.theme.UIThemeMenuButtonModel;
import org.gps.haiku.updater.UpdateResult;
import org.gps.haiku.vlcj.VLCJUtils;
import org.gps.haiku.vlcj.player.PlayerControlPanel;
import org.gps.haiku.vlcj.player.events.PlayerKeyEventListener;
import org.gps.haiku.vlcj.utils.YoutubeDLUtils;
import org.gps.haiku.ytdlp.update.YoutubeDLUpdater;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Created by leogps on 10/11/15.
 */
public class UIFrame extends JFrame {
    private static final Logger LOGGER = LogManager.getLogger(UIFrame.class);
    protected static final String ERROR_OCCURRED_MSG = "Error occurred!!";

    private final UIFrame instance;

    private JPanel wrapperPanel;
    private JPanel contentPanel;
    private JPanel bottomPanel;
    private JPanel topPanel;
    private JPanel bodyPanel;
    private JPanel leftHeaderPanel;
    private JPanel centerHeaderPanel;
    private JPanel rightHeaderPanel;
    private JLabel titleLabel;
    private JLabel versionLabel;
    private JPanel titlePanel;
    private JPanel contactPanel;
    private JLabel emailLabel;
    private JPanel bottomTopPanel;
    private JPanel tasksPanel;

    private PlayerControlPanel playerControlPanel;

    private JTextField searchBox;
    private static final String SEARCH_MSG = "Search selected playlist(s)...";
    boolean disableSearchBoxUpdateEvt = false;

    private JPanel tablesPanel;
    private JTable playlistTable;
    private JTable tracksTable;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JTextArea statusTextArea;
    private JProgressBar progressBar;
    private JScrollPane playlistScrollPane;
    private JScrollPane tracksTableScrollPane;
    private JPanel tracksTablePanel;
    private JPanel playlistTablePanel;
    private JLabel tracksTableHeadingLabel;
    private JPanel tracksTableHeadingPanel;
    private JPanel searchPanel;
    private NowPlayingPanel nowPlayingPanel;
    private JPanel fileBrowserWrapperPanel;
    private JPanel fileBrowserHeaderPanel;
    private JLabel fileBrowserHeaderLabel;
    private FileBrowserTree fileBrowserTree;
    private JLabel refreshLabel;
    private JPanel fileBrowserTreePanel;

    private List<UIFrameEventListener> uiFrameEventListenerList = new ArrayList<UIFrameEventListener>();

    protected static final ResourceBundle RESOURCE_BUNDLE =
            ResourceBundle.getBundle("ui");

    private UIMenuBar uiMenuBar;

    private PlayerKeyEventListener playerKeyEventListener;

    // Refresh Icons
    private static final Icon refreshIconBlue = new ImageIcon(Objects.requireNonNull(UIFrame.class.getClassLoader().getResource("images/refresh-icon-blue.png")));
    private static final Icon refreshIconGreen = new ImageIcon(Objects.requireNonNull(UIFrame.class.getClassLoader().getResource("images/refresh-icon-green.png")));
    private static final Icon refreshIconRed = new ImageIcon(Objects.requireNonNull(UIFrame.class.getClassLoader().getResource("images/refresh-icon-red.png")));

    public UIFrame() {
        super(RESOURCE_BUNDLE.getString("title"));


        pack();
        instance = this;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(new Rectangle(0, 0, 860, 640));
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        if (!OSInfo.isOSMac()) {
            setIconImage(fetchIconImage());
        }
        setMinimumSize(new Dimension(850, 450));
        //setPreferredSize(new Dimension(1024, 660));

        titleLabel.setText(RESOURCE_BUNDLE.getString("name"));

        versionLabel.setText(RESOURCE_BUNDLE.getString("version"));

        nowPlayingPanel.getTrackNameLabel().setText(Constants.EMPTY);
        nowPlayingPanel.getTrackAlbumNameLabel().setText(Constants.EMPTY);
        nowPlayingPanel.getTrackArtistNameLabel().setText(Constants.EMPTY);


        //searchBox.setForeground(new Color(153, 153, 153));
        searchBox.setText(SEARCH_MSG);
        searchBox.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent evt) {
                searchBoxFocussed(evt);
            }

            public void focusLost(FocusEvent evt) {
                searchBoxBlurred(evt);
            }
        });

        playlistScrollPane.setViewportView(playlistTable);
        if (!JavaVersionUtils.isGreaterThan6()) {
            // In Java 6 for Mac on El Capitan, the scroll event does not repaint the table contents correctly.
            playlistScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent adjustmentEvent) {
                    if (!adjustmentEvent.getValueIsAdjusting()) {
                        playlistTable.repaint();
                    }
                }
            });
        }

        tracksTableScrollPane.setViewportView(tracksTable);
        if (!JavaVersionUtils.isGreaterThan6()) {
            // In Java 6 for Mac on El Capitan, the scroll event does not repaint the table contents correctly.
            tracksTableScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent adjustmentEvent) {
                    if (!adjustmentEvent.getValueIsAdjusting()) {
                        tracksTable.repaint();
                    }
                }
            });
        }

        attachEvents();

        add(wrapperPanel);
        bottomPanel.addKeyListener(new KeyAdapter() {
        });

        this.setJMenuBar(uiMenuBar);
        if (!Main.isVlcjInitSucceeded()) {
            uiMenuBar.getOpenMenuItem().setEnabled(false);
            uiMenuBar.getOpenNetworkFileMenuItem().setEnabled(false);
            uiMenuBar.getIncreaseVolumeMenuItem().setEnabled(false);
            uiMenuBar.getDecreaseVolumeMenuItem().setEnabled(false);
            uiMenuBar.getGoToMenuItem().setEnabled(false);
        }

        /*
        * Adding right-click context menu for JTable.
         */
        final ActionListener tracksContextEventListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JMenuItem source = (JMenuItem) actionEvent.getSource();
                if (source.getText().equals(TracksContextMenu.INFORMATION)) {
                    // TODO: Implement Track Information popup.
                } else if (source.getText().equals(TracksContextMenu.PLAY_TEXT)) {
                    for (UIFrameEventListener uiFrameEventListener : uiFrameEventListenerList) {
                        //TODO
                    }
                }
            }
        };

        tracksTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() && tracksTable.getSelectedRowCount() > 0) {
                    e.consume();

                    TracksContextMenu contextMenu = new TracksContextMenu(tracksTable.getSelectedRowCount());

                    // Add popup selection event listener
                    contextMenu.getInformationMenu().addActionListener(tracksContextEventListener);
                    contextMenu.getPlayMenu().addActionListener(tracksContextEventListener);

                    e.translatePoint(instance.getContentPane().getX(), instance.getContentPane().getY());
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        refreshLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                FileSystemRefreshTask fileSystemRefreshTask = new FileSystemRefreshTask();
                final InterruptableProcessDialog interruptableProcessDialog = new InterruptableProcessDialog(fileSystemRefreshTask, true);

                fileSystemRefreshTask.registerListener(new AsyncTaskListener() {
                    @Override
                    public void onSuccess(InterruptableAsyncTask interruptableAsyncTask) {
                        interruptableProcessDialog.close();
                    }

                    @Override
                    public void onFailure(InterruptableAsyncTask interruptableAsyncTask) {
                        interruptableProcessDialog.close();
                        JOptionPane.showMessageDialog(null, "File System refresh failed.");
                    }
                });

                fileSystemRefreshTask.execute();
                interruptableProcessDialog.showDialog();
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                refreshLabel.setIcon(refreshIconRed);
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                refreshLabel.setIcon(refreshIconGreen);
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                refreshLabel.setIcon(refreshIconBlue);
            }
        });
    }

    protected class FileSystemRefreshTask implements InterruptableAsyncTask<Void, Void> {

        private final InterruptableAsyncTask<Void, Void> _this;

        public FileSystemRefreshTask() {
            _this = this;
        }

        private final Runnable refreshTask = new Runnable() {
            @Override
            public void run() {
                try {

                    final TreePath selectedPath = fileBrowserTree.getCurrentSelectionPath();
                    fileBrowserTree.refresh();

                    if(selectedPath != null) {
                        fileBrowserTree.select(selectedPath);
                        fileBrowserTree.getJFileTree().expandPathAsync(selectedPath, () -> {
                            fileBrowserTree.getJFileTree().scrollPathToVisible(selectedPath);
                            for(AsyncTaskListener asyncTaskListener : asyncTaskListenerList) {
                                asyncTaskListener.onSuccess(_this);
                            }
                        });
                    }

                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                    for(AsyncTaskListener asyncTaskListener : asyncTaskListenerList) {
                        asyncTaskListener.onFailure(_this);
                    }
                }
            }
        };
        private Thread executableThread;
        private final List<AsyncTaskListener> asyncTaskListenerList = new ArrayList<AsyncTaskListener>();

        public Void execute() {
            if(executableThread != null && executableThread.isAlive()) {
                throw new IllegalStateException("A previously submitted task is still getting executed.");
            }
            executableThread = new Thread(refreshTask);
            executableThread.start();
            return null;
        }

        @Override
        public void registerListener(AsyncTaskListener asyncTaskListener) {
            asyncTaskListenerList.add(asyncTaskListener);
        }

        @Override
        public void interrupt() {
            if(executableThread != null & executableThread.isAlive()) {
                executableThread.interrupt();
            }
        }

        @Override
        public boolean isInterrupted() {
            return executableThread.isInterrupted();
        }

        @Override
        public Void getResult() {
            return null;
        }
    }

    private void attachEvents() {

        //PlaylistTableStuff
        this.playlistTable.getSelectionModel().
                addListSelectionListener(lse -> {

                    if (playlistTable.getSelectedRowCount() < 1) {
                        tracksTableHeadingLabel.setText("");
                        return;
                    }

                    clearSearchBox();

                    if (!lse.getValueIsAdjusting()
                            && lse.getSource() == playlistTable.getSelectionModel()) {

                        LOGGER.debug(playlistTable.getSelectedRows().length + " playlists selected.");

                        if (playlistTable.getSelectedRows().length > 1) {

                        } else {
                            //TODO. (PlayListHolder) playlistTable.getValueAt(playlistTable.getSelectedRows()[0], 0)).getPlaylist();


                        }

                        for (UIFrameEventListener uiFrameEventListener : uiFrameEventListenerList) {
                            //uiFrameEventListener.onPlaylistSelectedEvent(instance);
                        }

                    }

                });


        this.playlistTable.setRowSorter(new TableRowSorter(this.playlistTable.getModel()));


        //TracksTableStuff
        ((TracksTableModel) this.tracksTable.getModel()).setTracksTableRowSorter(tracksTable);


        final int doubleClickValue = 2;
        this.tracksTable.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent me) {
            }

            @Override
            public void mousePressed(MouseEvent me) {
                if (!me.isPopupTrigger() && !SwingUtilities.isRightMouseButton(me) && me.getClickCount() == doubleClickValue && !me.isConsumed()) {
                    JTable table = (JTable) me.getSource();
                    Point p = me.getPoint();
                    int row = table.rowAtPoint(p);
                    if (me.getClickCount() == 2 && row >= 0) {

                        //TODO final TrackHolder holder = (TrackHolder) tracksTable.getValueAt(row, TracksTableModel.getHolderIndex());

                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                //
            }

            @Override
            public void mouseEntered(MouseEvent me) {
                //
            }

            @Override
            public void mouseExited(MouseEvent me) {
                //
            }


        });

        /**
         * Tasks Panel Events
         */

        // Search box
        searchBox.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent de) {
                updated(de);
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                updated(de);
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
                //Nothing
            }

            public void updated(final DocumentEvent de) {
                if (!disableSearchBoxUpdateEvt) {
                    Document doc = de.getDocument();
                    try {
                        String searchQuery = doc.getText(0, doc.getLength()).trim().toUpperCase();
                        LOGGER.debug(searchQuery);

                        if (searchQuery.equalsIgnoreCase(SEARCH_MSG)) {
                            searchQuery = null;
                        }

                        for (UIFrameEventListener uiFrameEventListener : uiFrameEventListenerList) {
                            uiFrameEventListener.onSearch(searchQuery, instance);
                        }

                    } catch (BadLocationException ex) {
                        LOGGER.error("Error occurred when reading search query", ex);
                    }
                }
            }

        });

        uiMenuBar = new UIMenuBar();
        if (Main.isVlcjInitSucceeded()) {
            // Setting Menu bar event listener.
            uiMenuBar.getOpenMenuItem().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    // Propagating event as UI Frame Event.
                    for (UIFrameEventListener uiFrameEventListener : uiFrameEventListenerList) {
                        uiFrameEventListener.onFileOpenRequested();
                    }
                }
            });
            uiMenuBar.getOpenMenuItem().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            uiMenuBar.getOpenNetworkFileMenuItem().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    // Propagating event as UI Frame Event.
                    for (UIFrameEventListener uiFrameEventListener : uiFrameEventListenerList) {
                        uiFrameEventListener.onNetworkFileOpenRequested();
                    }
                }
            });
            uiMenuBar.getOpenNetworkFileMenuItem().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        uiMenuBar.getIncreaseVolumeMenuItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.getPlayer().handleVolumeIncreasedEvent(Constants.DEFAULT_VOLUME_CHANGE);
            }
        });
        uiMenuBar.getIncreaseVolumeMenuItem().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK));

        uiMenuBar.getDecreaseVolumeMenuItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.getPlayer().handleVolumeDecreasedEvent(Constants.DEFAULT_VOLUME_CHANGE);
            }
        });
        uiMenuBar.getDecreaseVolumeMenuItem().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK));

        uiMenuBar.getGoToMenuItem().addActionListener(actionEvent -> Main.getPlayer().handleGoToEvent());
        uiMenuBar.getGoToMenuItem().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        uiMenuBar.getVlcMenuItem().addActionListener(actionEvent -> {
            StringBuilder message = new StringBuilder();
            if(VLCJUtils.isVlcInitSucceeded()) {
                String vlcLink = RESOURCE_BUNDLE.getString("vlc.link");
                message.append("VLC initialized successfully.")
                       .append(String.format("<br\\>VLC Version: %s", VLCJUtils.getVlcVersion()))
                       .append("<br\\>")
                       .append("<br\\>")
                       .append(buildLink(vlcLink, vlcLink));
            } else {
                message.append("VLC failed to initialize.");
            }
            JEditorPane editor = buildHtmlEditorPane(message.toString());
            JOptionPane.showMessageDialog(null, editor, "About VLC Engine", JOptionPane.INFORMATION_MESSAGE);
        });

        uiMenuBar.getVlcjMenuItem().addActionListener(e -> {
            StringBuilder message = new StringBuilder();
            if(VLCJUtils.isVlcInitSucceeded()) {
                String vlcjLink = RESOURCE_BUNDLE.getString("vlcj.link");
                message.append(String.format("VLCJ version %s", VLCJUtils.getVlcJVersion()))
                        .append("<br\\>")
                        .append(buildLink(vlcjLink, vlcjLink))
                        .append("<br\\>")
                        .append("<br\\>")
                        .append("VLC initialized successfully.")
                        .append("<br\\>")
                        .append(String.format("VLC Version: %s", VLCJUtils.getVlcVersion()));

            } else {
                message.append("VLC failed to initialize.");
            }
            JEditorPane editor = buildHtmlEditorPane(message.toString());
            JOptionPane.showMessageDialog(null, editor, "About VLCJ", JOptionPane.INFORMATION_MESSAGE);
        });

        //Themes
        loadThemesSubMenu();

        uiMenuBar.getUpdatesMenuItem().addActionListener(e -> {
            YoutubeDLUpdater youtubeDLUpdater = new YoutubeDLUpdater();
            try {
                InterruptableAsyncTask<Void, UpdateResult> asyncProcess = youtubeDLUpdater.update(YoutubeDLUtils.fetchYoutubeDLExecutable(),
                        PropertyManager.getProperty("youtube-dl.repository"),
                        PropertyManager.getProperty("youtube-dl.repository.asset.name"),
                        PropertyManager.getProperty("youtube-dl.repository.supported.checksum.names"));

                if(asyncProcess == null) {
                    JOptionPane.showMessageDialog(null, "Update failed. Error code: " + 1001,
                            "Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                final InterruptableProcessDialog interruptableProcessDialog = new InterruptableProcessDialog(asyncProcess, true);
                asyncProcess.registerListener(new AsyncTaskListener<Void, UpdateResult>() {
                    public void onSuccess(InterruptableAsyncTask<Void, UpdateResult> interruptableAsyncTask) {

                        interruptableProcessDialog.close();
                        UpdateResult updateResult = interruptableAsyncTask.getResult();
                        if(updateResult.getReason() == UpdateResult.Reason.UPDATE_NOT_AVAILABLE) {
                            JOptionPane.showMessageDialog(null, "No new updates available for Youtube-DL component.",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(null, "Youtube-DL component updated successfully.",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }

                    public void onFailure(InterruptableAsyncTask<Void, UpdateResult> interruptableAsyncTask) {
                        interruptableProcessDialog.close();
                        UpdateResult updateResult = interruptableAsyncTask.getResult();
                        JOptionPane.showMessageDialog(null, "Youtube-DL component failed to update: \n"
                                + updateResult.getReason().getReason(), "Failed", JOptionPane.ERROR_MESSAGE);
                    }

                });
                asyncProcess.execute();
                interruptableProcessDialog.showDialog();
            } catch (Exception ex) {
                String message = ex.getMessage();
                JOptionPane.showMessageDialog(null, message, "Failed", JOptionPane.ERROR_MESSAGE);
                LOGGER.error(message, ex);
            }
        });

        uiMenuBar.getAboutIMPMenuItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                StringBuilder message = new StringBuilder();
                String githubLink = RESOURCE_BUNDLE.getString("github");
                message.append(RESOURCE_BUNDLE.getString("name"))
                        .append("&nbsp;")
                        .append(RESOURCE_BUNDLE.getString("version"))

                        .append("<br\\>")
                        .append("<br\\>")

                        .append("Author: ")
                        .append(RESOURCE_BUNDLE.getString("author"))

                        .append("<br\\>")

                        .append("Github: ")
                        .append(buildLink(githubLink, githubLink));
                JEditorPane editor = buildHtmlEditorPane(message.toString());
                String title = RESOURCE_BUNDLE.getString("about.player.title");
                JOptionPane.showMessageDialog(null, editor, title, JOptionPane.INFORMATION_MESSAGE);
            }
        });

        uiMenuBar.getExitMenuItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ApplicationExitHandler.handle(instance);
            }
        });

        this.setExtendedState(MAXIMIZED_BOTH);

        StatusMessageAppender.setStatusLabel(statusTextArea);
    }

    private static JEditorPane buildHtmlEditorPane(String html) {
        JEditorPane editor = new JEditorPane();
        editor.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        editor.setEditable(false);
        editor.setText(html);
        editor.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (IOException | URISyntaxException ex) {
                        LOGGER.error(ex);
                    }
                }
            }
        });
        return editor;
    }

    private static String buildLink(String link, String text) {
        return String.format("<a href=\"%s\">%s</a>", link, text);
    }

    private void loadThemesSubMenu() {
        try {
            final List<JRadioButtonMenuItem> themesSubMenuList = uiMenuBar.getThemesSubMenuList();
            final JMenuItem themesMenuItem = uiMenuBar.getThemesMenuItem();

            ObjectMapper objectMapper = new ObjectMapper();
            URL themesList = UIMenuBar.class.getClassLoader().getResource("themes.json");
            List<UITheme> uiThemes = objectMapper.readValue(themesList, new TypeReference<>() {});
            LOGGER.info(String.format("Loaded %s themes from themes.json", uiThemes.size()));
            for (final UITheme uiTheme : uiThemes) {
                final JRadioButtonMenuItem themeSubMenuItem = buildThemeSubmenuItem(uiTheme,
                    themesSubMenuList);
                themesMenuItem.add(themeSubMenuItem);
                themesSubMenuList.add(themeSubMenuItem);
            }

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    private JRadioButtonMenuItem buildThemeSubmenuItem(UITheme uiTheme,
        List<JRadioButtonMenuItem> themesSubMenuList)
    {
        final JRadioButtonMenuItem themeSubMenuItem = new JRadioButtonMenuItem(uiTheme.getName());
        themeSubMenuItem.setModel(new UIThemeMenuButtonModel(uiTheme));
        themeSubMenuItem.addActionListener(actionEvent -> {
            LOGGER.info("Theme selection made: " + uiTheme.getName());
            for (JRadioButtonMenuItem jMenuItem : themesSubMenuList) {
                jMenuItem.setSelected(false);
            }
            boolean success = setUITheme(uiTheme);
            if (success) {
                themeSubMenuItem.setSelected(true);
                saveUITheme(uiTheme);
            }
        });
        if (uiTheme.getClassName().equals( UIManager.getLookAndFeel().getClass().getName())) {
            themeSubMenuItem.setSelected(true);
        }
        return themeSubMenuItem;
    }

    private void saveUITheme(UITheme uiTheme) {
        ConfigPropertyDao configPropertyDao;
        try {
             configPropertyDao = fetchConfigPropertyDao();
             if (configPropertyDao == null) {
                 return;
             }
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve configPropertyDao", ex);
            return;
        }

        try {
            ConfigProperty configProperty = configPropertyDao.findByKey("ui_theme");
            if (configProperty != null) {
                configPropertyDao.delete(configProperty.getId());
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to clear ui_theme in db", ex);
        }

        try {
            ConfigProperty configProperty = new ConfigProperty();
            configProperty.setProperty("ui_theme");
            String value = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(uiTheme);
            configProperty.setValue(value);
            configPropertyDao.insert(configProperty);
        } catch (Exception ex) {
            LOGGER.error("Failed to save selected theme to db.", ex);
        }
    }

    private ConfigPropertyDao fetchConfigPropertyDao() {
        if(!DbManagerImpl.getInstance().isInitiated()) {
            return null;
        }
        return new ConfigPropertyDao(DbManagerImpl.getInstance().getConnection());
    }

    public boolean setUITheme(UITheme uiTheme) {
        try {
            FlatAnimatedLafChange.showSnapshot();
            Method method = Class.forName(uiTheme.getClassName()).getDeclaredMethod("setup");
            method.invoke(null);
            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
            return true;
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        return false;
    }

    /**
     * Fetches the icon image specified in the ui.properties file.
     *
     * @return
     */
    private static Image fetchIconImage() {
        return new ImageIcon(UIFrame.class.getClassLoader().getResource("images/haiku.png")).getImage();
    }

    private void openThis(final String location) {
        final Desktop desktop;

        try {
            final URI uri = new URI(location);
            desktop = Desktop.getDesktop();

            if (!desktop.isSupported(Desktop.Action.BROWSE)) {

                LOGGER.error("Desktop does not support BROWSE action.");
            } else {
                desktop.browse(uri);
            }
        } catch (URISyntaxException ex) {
            LOGGER.error("Error occurred creating email or track link.", ex);
        } catch (IOException ioe) {
            LOGGER.error("Error occurred browsing to email or track link.", ioe);
        }
    }

    private void searchBoxFocussed(FocusEvent evt) {
        doSearchBoxFocussed();
    }

    private void searchBoxBlurred(FocusEvent evt) {
        doSearchBoxBlurred();
    }

    private void doSearchBoxBlurred() {
        if (searchBox.getText().trim().isEmpty()) {
            disableSearchBoxUpdateEvt(true);
            searchBox.setText(SEARCH_MSG);
            disableSearchBoxUpdateEvt(false);
        }
    }

    private void doSearchBoxFocussed() {
        if (searchBox.getText().equals(SEARCH_MSG)) {
            disableSearchBoxUpdateEvt(true);
            searchBox.setText("");
            disableSearchBoxUpdateEvt(false);
        }
    }

    private void clearSearchBox() {
        disableSearchBoxUpdateEvt(true);
        searchBox.setText("");
        doSearchBoxBlurred();
        disableSearchBoxUpdateEvt(false);
    }

    private void disableSearchBoxUpdateEvt(final boolean b) {
        this.disableSearchBoxUpdateEvt = b;
    }

    public void addUIFrameEventListener(UIFrameEventListener uiFrameEventListener) {
        uiFrameEventListenerList.add(uiFrameEventListener);
    }

    public JPanel getMainUIPanel() {
        return contentPanel;
    }

    public PlayerControlPanel getPlayerControlPanel() {
        return playerControlPanel;
    }

    public JTable getPlaylistTable() {
        return playlistTable;
    }

    public JTable getTracksTable() {
        return tracksTable;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public NowPlayingPanel getNowPlayingPanel() {
        return nowPlayingPanel;
    }

    private void createUIComponents() {

        //Tracks Table
        tracksTable = new JTable() {

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                int modelIndex = convertRowIndexToModel(row);
                Component returnComp = super.prepareRenderer(renderer, row, column);

                if (!returnComp.getBackground().equals(getSelectionBackground())) {
                    Color bg = (modelIndex % 2 == 0 ? Color.WHITE : ALTERNATE_COLOR);
                    returnComp.setBackground(bg);
                }

                JComponent jcomp = (JComponent) returnComp;
                String tooltipText = String.valueOf(getModel().getValueAt(modelIndex, column));
                if (tooltipText.equals("null")) {
                    tooltipText = Constants.EMPTY;
                }
                jcomp.setToolTipText(tooltipText);

                return returnComp;
            }
        };

        tracksTable.setModel(new TracksTableModel());
        tracksTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        tracksTable.setIntercellSpacing(new Dimension(1, 2));
        tracksTable.setRowHeight(21);
        tracksTable.setShowGrid(true);
        tracksTable.getTableHeader().setReorderingAllowed(false);
        tracksTable.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        //Playlist Table
        playlistTable = new JTable();
        playlistTable.setModel(new PlaylistTableModel());
        playlistTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        playlistTable.setColumnSelectionAllowed(true);
        playlistTable.setIntercellSpacing(new Dimension(1, 2));
        playlistTable.setRowHeight(24);
        //playlistTable.getColumnModel().setColumnMargin(18);
        playlistTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        playlistTable.setShowGrid(false);
        playlistTable.getTableHeader().setReorderingAllowed(false);
        playlistTable.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        fileBrowserTree = new FileBrowserTree(System.getProperty("user.home"), TreeSelectionModel.SINGLE_TREE_SELECTION);
        fileBrowserTree.registerFileBrowserTreeEventListener(fileNode -> {
            File file = fileNode.getFile();
            if(!file.isDirectory()) {
                Main.getPlayer().play(file);
            }
        });
    }

    private static final Color ALTERNATE_COLOR = new Color(252, 242, 206);

    public JLabel getTracksTableHeadingLabel() {
        return tracksTableHeadingLabel;
    }

    public UIMenuBar getUiMenuBar() {
        return uiMenuBar;
    }

    public FileBrowserTree getFileBrowserTree() {
        return fileBrowserTree;
    }
}
