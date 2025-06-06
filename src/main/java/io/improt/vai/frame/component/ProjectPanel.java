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

        // Set custom icons for tree arrows to improve contrast
        UIManager.put("Tree.expandedIcon", new ImageIcon("images/arrow_down.png"));
        UIManager.put("Tree.collapsedIcon", new ImageIcon("images/arrow_right.png"));
        // Adjust tree line color for better visibility
        UIManager.put("Tree.line", new Color(0, 0, 0));//, 150, 150));

        tree = new JTree();
        tree.setModel(null);

        tree.setCellRenderer(new ActiveFileTreeCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tree);

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    File selectedFile = pathToFile(selPath);
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (e.getClickCount() == 1) {
                            if (selectedFile != null && selectedFile.isFile()) {
                                App.getInstance().getActiveFileManager().addFile(selectedFile);
                            }
                        } else if (e.getClickCount() == 2) {
                            if (selectedFile != null && selectedFile.isFile()) {
                                App.getInstance().getActiveFileManager().removeFile(selectedFile);
                            }
                        }
                    } else if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1) {
                        if (selectedFile != null) {
                            showContextMenu(e.getX(), e.getY(), selectedFile);
                        }
                    }
                }
            }
        });

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
            }
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    public void init(App backend) {
        if (backend.getActiveFileManager() != null) {
            backend.getActiveFileManager().addEnabledFilesChangeListener(this);
        }
    }

    /**
     * Refreshes the tree view while retaining its expanded state.
     *
     * @param root The root directory to display in the tree.
     */
    public void refreshTree(File root) {
        if (root != null && root.isDirectory()) {
            DefaultMutableTreeNode rootNode = createTreeNodes(root);
            tree.setModel(new DefaultTreeModel(rootNode));

            TreePath rootPath = new TreePath(rootNode);
            expandSingleChildNodes(rootPath);

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
                    if (child.getName().endsWith(".meta")) {
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

            if (!child.isLeaf()) {
                TreePath childPath = path.pathByAddingChild(child);
                tree.expandPath(childPath);
                expandSingleChildNodes(childPath);
            }
        }
    }

    private class ActiveFileTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Color activeColor = new Color(144, 238, 144); // Light Green
        private final ImageIcon folderClosedIcon;
        private final ImageIcon folderOpenIcon;
        private final ImageIcon fileIcon;

        public ActiveFileTreeCellRenderer() {
            super();
            // Load custom icons for folders and files
            folderClosedIcon = new ImageIcon("images/folder.png");
            folderOpenIcon = new ImageIcon("images/folder-open.png");
            fileIcon = new ImageIcon("images/file.png");

            // Set default icons to be used by the renderer
            setClosedIcon(folderClosedIcon);
            setOpenIcon(folderOpenIcon);
            setLeafIcon(fileIcon);
            
            // Adjust text colors for better contrast
            setTextNonSelectionColor(Color.decode("#202124"));
            setTextSelectionColor(Color.WHITE);
            setBackgroundNonSelectionColor(Color.decode("#FFFFFF"));
            setBackgroundSelectionColor(Color.decode("#4285F4"));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            File file = pathToFile(tree.getPathForRow(row));
            if (file != null) {
                if (file.isDirectory()) {
                    setIcon(expanded ? folderOpenIcon : folderClosedIcon);
                } else if(file.isFile()){
                    setIcon(fileIcon);
                }
            }
            
            // Set background specifically for active files (only if it's a file)
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
                App.getInstance().openWorkspace(workspaceDir);
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
        SwingUtilities.invokeLater(tree::repaint);
    }
}
