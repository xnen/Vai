package io.improt.vai.frame.component;

import io.improt.vai.backend.App;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class ProjectPanel extends JPanel {
    private JTree tree;

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
                if (selRow != -1 && e.getClickCount() == 1) {
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    File selectedFile = pathToFile(selPath);
                    if (selectedFile != null && selectedFile.isFile()) {
                        App.getInstance().toggleFile(selectedFile);
                        tree.repaint(); // Refresh the tree to update colors
                    }
                }
            }
        });

        // Add TreeSelectionListener to handle selection changes
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                // Notify listeners if needed
            }
        });

        // Add TreeExpansionListener to auto-expand single directory nodes
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                TreePath path = event.getPath();
                expandSingleChildNodes(path);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                // Optional: Implement behavior on collapse if needed
            }
        });

        // Add the scroll pane containing the tree to the panel
        add(scrollPane, BorderLayout.CENTER);
    }

    public void refreshTree(File root) {
        if (root != null && root.isDirectory()) {
            DefaultMutableTreeNode rootNode = createTreeNodes(root);
            tree.setModel(new DefaultTreeModel(rootNode));

            // Expand nodes with a single child directory
            TreePath rootPath = new TreePath(rootNode);
            expandSingleChildNodes(rootPath);
        }
    }

    private DefaultMutableTreeNode createTreeNodes(File file) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(file.getName());
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    // Skip files/folders in .vaiignore
                    if (App.getInstance().getIgnoreList().contains(child.getName())) {
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
        StringBuilder sb = new StringBuilder(backend.getCurrentWorkspace().getParent());
        Object[] parts = path.getPath();
        for (Object part : parts) {
            sb.append(File.separator).append(part.toString());
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
        private Color activeColor = new Color(144, 238, 144); // Light Green

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

    public JTree getTree() {
        return this.tree;
    }
}
