package org.gps.haiku.utils.ui.fileutils;

import org.gps.haiku.utils.SingleQueuedThreadExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by leogps on 10/11/15.
 */
public class FileBrowserTree extends JPanel {

    private static final Logger LOGGER = LogManager.getLogger(FileBrowserTree.class);

    private JTree fileTree;
    private JScrollPane scrollPane;
    private JPanel wrapperPanel;
    private JPanel contentPanel;
    private JFileChooser fileChooser = new JFileChooser();
    private String userRequestedFilePath;

    private FileNode rootNode;

    private int selectionMode;

    private final List<FileBrowserTreeEventListener> fileBrowserTreeEventListenerList = new ArrayList<FileBrowserTreeEventListener>();

    public FileBrowserTree(String userRequestedFilePath, int selectionMode) {
        this.userRequestedFilePath = userRequestedFilePath;
        this.selectionMode = selectionMode;

        scrollPane.setViewportView(fileTree);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(adjustmentEvent -> {
            if (!adjustmentEvent.getValueIsAdjusting()) {
                fileTree.repaint();
            }
        });

        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                if (mouseEvent.getClickCount() == 2) {
                    TreePath selectionPath = getCurrentSelectionPath();
                    FileNode fileNode = getFileNodeFromSelectionPath(selectionPath);
                    for (FileBrowserTreeEventListener fileBrowserTreeEventListener : fileBrowserTreeEventListenerList) {
                        fileBrowserTreeEventListener.onNodeDoubleClicked(fileNode);
                        expandAndScrollTo(selectionPath);
                    }

                }
            }
        });
    }

    private FileNode getFileNodeFromSelectionPath(TreePath selectionPath) {
        if(selectionPath != null) {
            return (FileNode) selectionPath.getLastPathComponent();
        }
        return null;
    }

    public TreePath getCurrentSelectionPath() {
        TreePath[] selectedTreePath = getFileTree().getSelectionPaths();
        if (selectedTreePath != null && selectedTreePath.length > 0) {
            TreePath selectionPath = selectedTreePath[selectedTreePath.length - 1];
            return selectionPath;
        }
        return null;
    }

    private FileNode getSelectedFileNode() {
        return getFileNodeFromSelectionPath(getCurrentSelectionPath());
    }

    private void expandAndScrollTo(final TreePath selectionPath) {
        getJFileTree().expandPathAsync(selectionPath, new JFileTreeNodeExpansionProcessor() {

            public void onNodeExpansion() {
                getJFileTree().scrollPathToVisible(selectionPath);
                LOGGER.debug("Node expanded: " + selectionPath);
            }
        });
    }

    public JFileTree getJFileTree() {
        return (JFileTree) getFileTree();
    }

    private void createUIComponents() {
        fileTree = new JFileTree();

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                fileTree.getSelectionModel().setSelectionMode(selectionMode);

                initializeComponents(null);

                fileTree.setCellRenderer(new DefaultTreeCellRenderer() {

                    @Override
                    public Component getTreeCellRendererComponent(JTree tree,
                                                                  Object value, boolean selected, boolean expanded,
                                                                  boolean leaf, int row, boolean hasFocus) {
                        Component _this = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                        FileNode node = (FileNode) value;
                        if (fileChooser == null) {
                            fileChooser = new JFileChooser();
                        }
                        if(node != rootNode) {
                            Icon icon = fileChooser.getUI().getFileView(fileChooser).getIcon(node.getFile());
                            setIcon(icon);
                        }

                        String fileName = FileSystemView.getFileSystemView().getSystemDisplayName(node.getFile());
                        setText(fileName);
                        setToolTipText(fileName);
                        return _this;
                    }
                });

                makeDefaultSelection();
            }
        });
    }

    public Enumeration<TreePath> getExpandedPaths() throws IOException {
        TreePath rootPath = rootNode.getPathTo(null);
        return getFileTree().getExpandedDescendants(rootPath);
    }

    public FileNode getCurrentSelectedNode() {
        TreePath previouslySelectedPath = getCurrentSelectionPath();
        FileNode selectedNode = getFileNodeFromSelectionPath(previouslySelectedPath);
        return selectedNode;
    }

    public class JFileTree extends JTree {

        private SingleQueuedThreadExecutor singleQueuedThreadExecutor = new SingleQueuedThreadExecutor();

        public void expandPathAsync(final TreePath treePath, final JFileTreeNodeExpansionProcessor... nodeExpansionProcessors) {
            final JTree thisTree = this;

            singleQueuedThreadExecutor.terminateExistingAndInvokeLater(new Runnable() {

                public void run() {
                    thisTree.expandPath(treePath);
                    if (nodeExpansionProcessors != null && nodeExpansionProcessors.length > 0) {
                        for (JFileTreeNodeExpansionProcessor nodeExpansionProcessor : nodeExpansionProcessors) {
                            nodeExpansionProcessor.onNodeExpansion();
                        }
                    }
                }
            });
        }
    }

    public interface JFileTreeNodeExpansionProcessor {
        void onNodeExpansion();
    }

    private void initializeComponents(Enumeration<TreePath> expandablePaths) {
        final String ROOT_FILE_NAME = "__ROOT__";
        File virtualRootFile = new VirtualFolder(ROOT_FILE_NAME);
        rootNode = new FileNode(virtualRootFile);
        fileTree.setModel(new DefaultTreeModel(rootNode));
        expandPaths(expandablePaths);
    }

    public void makeDefaultSelection() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                String defaultPath = null;
                if (userRequestedFilePath != null) {
                    File userRequestedFile = new File(userRequestedFilePath);
                    if (userRequestedFile.exists()) {
                        defaultPath = userRequestedFilePath;
                    }
                }

                if (defaultPath == null && System.getProperty("user.home") != null) {
                    defaultPath = System.getProperty("user.home");
                }

                if (defaultPath == null) {
                    defaultPath = File.separator;
                }

                File defaultFile = new File(defaultPath);
                expandAndScrollToFile(defaultFile);
            }
        });
    }

    public void select(TreePath treePath) {
        fileTree.setSelectionPath(treePath);
    }

    public void expandPaths(Enumeration<TreePath> expandablePaths) {
        if(expandablePaths != null) {
            LOGGER.debug("Expanding paths...");
            int count = 0; // FIXME: Make this static final. or move to properties.
            // Only expanding top 10 paths to conserve memory.
            while (expandablePaths.hasMoreElements() && ++count < 10) {
                TreePath previouslyExpandedPath = expandablePaths.nextElement();
                getFileTree().expandPath(previouslyExpandedPath);
                LOGGER.debug("Expanded path: " + previouslyExpandedPath);
            }
        }
    }

    public void refresh() throws IOException {
        LOGGER.debug("Refreshing file system...");

        LOGGER.debug("Re-initializing file system tree...");
        initializeComponents(null);
        LOGGER.debug("Re-initializing file system tree completed.");
    }

    private DefaultTreeModel getTreeModel() {
        return (DefaultTreeModel) fileTree.getModel();
    }

    private void expandAndScrollToFile(File file) {
        if (rootNode != null && file != null && file.exists()) {
            try {
                TreePath path = rootNode.getPathTo(file);
                fileTree.setSelectionPath(path);
                expandAndScrollTo(path);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }

        }
    }

    public JTree getFileTree() {
        return fileTree;
    }

    public void registerFileBrowserTreeEventListener(FileBrowserTreeEventListener fileBrowserTreeEventListener) {
        fileBrowserTreeEventListenerList.add(fileBrowserTreeEventListener);
    }

    public void attemptToShowInTree(String location) {
        if(location == null) {
            return;
        }

        final File file = getFileFromLocation(location);
        if(file != null && file.exists() && !isSelectedFile(file)) {
            expandAndScrollToFile(file);
        }
    }

    private boolean isSelectedFile(File file) {
        FileNode selectedFileNode = getSelectedFileNode();
        if(selectedFileNode != null) {
            return selectedFileNode.getFile().getPath().equals(file.getPath());
        }
        return false;
    }

    private File getFileFromLocation(String location) {
        try {
            URL locationURL = new URL(location);
            if(isFileProtocol(locationURL)) {
                String path = locationURL.getPath();
                String decodedPath = URLDecoder.decode(path, "UTF-8");
                LOGGER.debug("Decoded filepath: " + path);
                return new File(decodedPath);
            }

        } catch (MalformedURLException | UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    private boolean isFileProtocol(URL locationURL) {
        return locationURL != null &&
                locationURL.getProtocol().equals("file");
    }

    /**
     * Represents a Virtual Folder.
     *
     */
    private static class VirtualFolder extends File {

        private final String name;

        public VirtualFolder(String name) {
            super(name);
            this.name = name;
        }

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public File[] listFiles() {
            return File.listRoots();
        }

        @Override
        public File[] listFiles(FilenameFilter filenameFilter) {
            return listFiles();
        }

        @Override
        public File[] listFiles(java.io.FileFilter fileFilter) {
            return listFiles();
        }

        @Override
        public String[] list() {
            List<String> fileStr = new ArrayList<String>();
            for(File file : listFiles()) {
                fileStr.add(file.getName());
            }
            return fileStr.toArray(new String[fileStr.size()]);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getParent() {
            return null;
        }

        @Override
        public File getParentFile() {
            return null;
        }

        @Override
        public String getPath() {
            return name;
        }

        @Override
        public boolean isAbsolute() {
            return true;
        }

        @Override
        public String getAbsolutePath() {
            return name;
        }

        @Override
        public File getAbsoluteFile() {
            return this;
        }

        @Override
        public String getCanonicalPath() throws IOException {
            return name;
        }

        @Override
        public boolean isHidden() {
            return false;
        }
    }

}
