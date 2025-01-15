package io.improt.vai.frame.component;

import io.improt.vai.backend.ActiveFileManager;
import io.improt.vai.backend.App;
import io.improt.vai.util.FileUtils;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ProjectPanel extends JPanel implements ActiveFileManager.EnabledFilesChangeListener {
    private final JTree tree;

    public ProjectPanel() {
        setLayout(new BorderLayout());

        // Initialize the tree
        tree = new JTree();
        tree.setModel(null);

        // Set custom renderer to highlight active files
        tree.setCellRenderer(new ActiveFileTreeCellRenderer());

        // Wrap the JTree in a JScrollPane
        JScrollPane scrollPane = new JScrollPane(tree);

        // Add mouse listener for tree interactions
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    File selectedFile = pathToFile(selPath);
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                        if (selectedFile != null && selectedFile.isFile()) {
                            App.getInstance().getActiveFileManager().toggleFile(selectedFile);
                            // Removed redundant tree.repaint() as it will be handled by listener
                        }
                    } else if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1) {
                        if (selectedFile != null) {
                            showContextMenu(e.getX(), e.getY(), selectedFile);
                        }
                    }
                }
            }
        });

        // Add TreeSelectionListener to handle selection changes
        tree.addTreeSelectionListener(e -> {
            // Notify listeners if needed
        });

        // Add TreeExpansionListener to auto-expand single directory nodes and handle state saving
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                TreePath path = event.getPath();
                expandSingleChildNodes(path);
                saveExpandedPaths();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                saveExpandedPaths();
                // Optional: Implement behavior on collapse if needed
            }
        });

        // Add the scroll pane containing the tree to the panel
        add(scrollPane, BorderLayout.CENTER);

    }

    public void init(App backend) {
        // Register as a listener to App for enabledFiles changes
        backend.getActiveFileManager().addEnabledFilesChangeListener(this);
    }

    /**
     * Refreshes the tree view while retaining its expanded state.
     *
     * @param root The root directory to display in the tree.
     */
    public void refreshTree(File root) {
        if (root != null && root.isDirectory()) {
            // Collect expanded paths before refreshing
            List<String> expandedPaths = new ArrayList<>();

            // Load tree nodes
            DefaultMutableTreeNode rootNode = createTreeNodes(root);
            tree.setModel(new DefaultTreeModel(rootNode));

            // Expand nodes with a single child directory
            TreePath rootPath = new TreePath(rootNode);
            expandSingleChildNodes(rootPath);

            // Load and expand saved paths
            List<String> savedExpandedPaths = FileUtils.loadTreeConfig(root);
            expandSavedPaths(savedExpandedPaths);
        }
    }

    /**
     * Retrieves the list of currently expanded paths as absolute file paths.
     *
     * @return A list of expanded file paths.
     */
    private List<String> getExpandedPaths() {
        List<String> paths = new ArrayList<>();
        if (tree == null || tree.getModel() == null) {
            return paths;
        }

        Enumeration<TreePath> enumeration = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                TreePath path = enumeration.nextElement();
                File file = pathToFile(path);
                if (file != null) {
                    paths.add(file.getAbsolutePath());
                }
            }
        }
        return paths;
    }

    /**
     * Saves the currently expanded paths to the tree configuration file.
     */
    private void saveExpandedPaths() {
        if (expandingFlag) {
            return;
        }

        File workspace = App.getInstance().getCurrentWorkspace();
        if (workspace == null) {
            return;
        }
        List<String> expandedPaths = getExpandedPaths();
        FileUtils.saveTreeConfig(expandedPaths, workspace);
    }

    // We don't need to save these paths if the tree is being expanded via expandSavedPaths()
    private boolean expandingFlag = false;

    /**
     * Expands the tree paths based on the provided list of file paths.
     *
     * @param paths The list of file paths to expand in the tree.
     */
    private void expandSavedPaths(List<String> paths) {
        expandingFlag = true;
        for (String path : paths) {
            File file = new File(path);
            if (file.exists() && file.isDirectory()) {
                TreePath treePath = fileToTreePath(file);
                if (treePath != null) {
                    tree.expandPath(treePath);
                }
            }
        }
        expandingFlag = false;
    }

    /**
     * Converts a File object to its corresponding TreePath in the JTree.
     *
     * @param file The file to convert.
     * @return The TreePath corresponding to the file, or null if not found.
     */
    private TreePath fileToTreePath(File file) {
        List<Object> elements = new ArrayList<>();
        File workspace = App.getInstance().getCurrentWorkspace();
        if (workspace == null) {
            return null;
        }
        String relativePath = workspace.toURI().relativize(file.toURI()).getPath();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getModel().getRoot();
        elements.add(node);
        if (relativePath.isEmpty()) {
            return new TreePath(elements.toArray());
        }
        String[] parts = relativePath.split("/");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            boolean found = false;
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                if (child.getUserObject().toString().equals(part)) {
                    node = child;
                    elements.add(node);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        return new TreePath(elements.toArray());
    }

    private DefaultMutableTreeNode createTreeNodes(File file) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(file.getName());
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    // Skip files/folders in .vaiignore
                    if (child.getName().endsWith(".meta")) {
                        // Ignore meta files.
                        continue;
                    }
                    if (child.isDirectory()) {
                        node.add(createTreeNodes(child));
                    } else {
                        node.add(new DefaultMutableTreeNode(child.getName()));
                    }
                }
            }
        }
        return node;
    }

    public File pathToFile(TreePath path) {
        App backend = App.getInstance();
        if (path == null || backend.getCurrentWorkspace() == null) return null;
        StringBuilder sb = new StringBuilder(backend.getCurrentWorkspace().getAbsolutePath());
        Object[] parts = path.getPath();
        for (int i = 1; i < parts.length; i++) { // Start from 1 to skip root
            sb.append(File.separator).append(parts[i].toString());
        }
        File file = new File(sb.toString());
        return file.exists() ? file : null;
    }

    private void expandSingleChildNodes(TreePath path) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (node.getChildCount() == 1) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(0);

            // Check if the child node is a directory (i.e., has children)
            if (!child.isLeaf()) {
                TreePath childPath = path.pathByAddingChild(child);
                tree.expandPath(childPath);

                // Recursive call to continue expanding
                expandSingleChildNodes(childPath);
            }
        }
    }

    // Custom TreeCellRenderer to highlight active files
    private class ActiveFileTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Color activeColor = new Color(144, 238, 144); // Light Green

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            File file = pathToFile(tree.getPathForRow(row));
            if (file != null && file.isFile() && App.getInstance().getEnabledFiles().contains(file)) {
                c.setBackground(activeColor);
                if (c instanceof JComponent) {
                    ((JComponent) c).setOpaque(true);
                }
            } else {
                c.setBackground(null);
                if (c instanceof JComponent) {
                    ((JComponent) c).setOpaque(false);
                }
            }
            return c;
        }
    }

    private void showContextMenu(int x, int y, File selectedFile) {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem useAsWorkspaceItem = new JMenuItem("Use as workspace");
        useAsWorkspaceItem.addActionListener(e -> {
            File workspaceDir = selectedFile.isDirectory() ? selectedFile : selectedFile.getParentFile();
            if (workspaceDir != null && workspaceDir.exists() && workspaceDir.isDirectory()) {
                App.getInstance().openDirectory(workspaceDir);
            } else {
                JOptionPane.showMessageDialog(this, "Selected item is not a valid directory.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        contextMenu.add(useAsWorkspaceItem);
        contextMenu.show(tree, x, y);
    }

    public JTree getTree() {
        return this.tree;
    }

    /**
     * Callback method when enabledFiles changes in App.
     *
     * @param updatedEnabledFiles The updated list of enabled files.
     */
    @Override
    public void onEnabledFilesChanged(java.util.List<File> updatedEnabledFiles) {
        // Repaint the tree to update the cell renderer
        SwingUtilities.invokeLater(tree::repaint);
    }
}
