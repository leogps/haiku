package org.gps.haiku.utils.ui.fileutils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by leogps on 10/10/15.
 */
public class FileNode extends DefaultMutableTreeNode {

    private static final Logger LOGGER = LogManager.getLogger(FileNode.class);

    private static final String FILE_SEPARATOR_PATTERN = Pattern.quote(File.separator);

    private static final FileFilter NON_HIDDEN_FILE_FILTER = file -> !file.isHidden();

    private final Map<FileNode, Integer> childIndexMap = new HashMap<>();

    private final File file;

    public File getFile() {
        return file;
    }

    public FileNode(File file) {
        this.file = file;
    }

    private List<FileNode> cachedChildren;// TODO: use timestamp and refresh if queried after sometime.


    public TreeNode getChildAt(int i) {
        if(file != null && file.isDirectory()) {
            List<FileNode> children = getChildren();
            if(i < children.size()) {
                return getChildren().get(i);
            }
        }
        return null;
    }

    public int getChildCount() {
        if(file != null && file.isDirectory()) {
            return getChildren().size();
        }
        return 0;
    }


    public TreeNode getParent() {
        if(file != null && !file.getName().equals("__ROOT__")) {
            // FIXME: Cannot blindly check the name. Maybe generate static random name and check for it here.
            return new FileNode(file.getParentFile());
        }
        return null;
    }

    public int getIndex(TreeNode treeNode) {
        if(file != null && file.isDirectory() && treeNode instanceof FileNode) {
            FileNode fileNode = (FileNode) treeNode;
            if (cachedChildren == null) {
                buildChildIndex();
            }
            if (cachedChildren.contains(treeNode)) {
                return childIndexMap.get(fileNode);
            }
        }
        return -1;
    }

    private void buildChildIndex() {
        getChildren();
    }

    public boolean getAllowsChildren() {
        return file.isDirectory() && file.canWrite();
    }


    public boolean isLeaf() {
        return !file.isDirectory();
    }


    public Enumeration<TreeNode> children() {
        if(file == null || !file.isDirectory()) {
            return new Vector<TreeNode>().elements();
        }
        return new Vector<TreeNode>(getChildren()).elements();
    }

    public List<FileNode> getChildren() {
        if(cachedChildren == null) {
            cachedChildren = doRetrieveChildren(true);
        }
        return cachedChildren;
    }

    private List<FileNode> doRetrieveChildren(boolean updateIndexMap) {
        if(updateIndexMap) {
            childIndexMap.clear();
        }
        if(file.isDirectory()) {
            File[] children = file.listFiles(NON_HIDDEN_FILE_FILTER);
            if(children != null) {
                List<FileNode> childNodeList = new ArrayList<>();
                for (int index = 0; index < children.length; index++) {
                    File child = children[index];
                    FileNode childNode = new FileNode(child);
                    childNodeList.add(childNode);
                    if(updateIndexMap) {
                        childIndexMap.put(childNode, index);
                    }
                }
                return childNodeList;
            }
        }
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return file.getName();
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }
        if(!(o instanceof FileNode)) {
            return false;
        }

        FileNode that = (FileNode) o;

        if(that.getFile() == null && this.getFile() == null) {
            return true;
        }
        if(that.getFile() != null && this.getFile() == null) {
            return false;
        }
        if(that.getFile() == null && this.getFile() != null) {
            return false;
        }

        try {
            return this.getFile().getCanonicalPath().equals(that.getFile().getCanonicalPath());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int hashCode() {
        if(file == null) {
            return -1;
        }
        return file.hashCode();
    }

    /**
     * Pass null for @param: pathTo to get the path for the current node.
     *
     */
    public TreePath getPathTo(File pathTo) throws IOException {
        List<FileNode> treePathList = new ArrayList<>();
        treePathList.add(this);

        if(pathTo != null) {
            String[] pathTokenArray = pathTo.getCanonicalPath().split(FILE_SEPARATOR_PATTERN);
            FileNode parentNode = this;
            for (String s : pathTokenArray) {
                boolean found = false;
                List<FileNode> children = parentNode.getChildren();

                for (FileNode child : children) {
                    if (child.getFile().getName().equals(s)) {
                        found = true;
                        parentNode = child;
                        treePathList.add(child);

                        break;
                    }
                }
                if (!found) {
                    break;
                }
            }
        }

        return new TreePath(treePathList.toArray(new FileNode[0]));
    }
}
