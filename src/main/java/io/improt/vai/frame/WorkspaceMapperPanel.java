package io.improt.vai.frame;

import io.improt.vai.backend.App;
import io.improt.vai.mapping.WorkspaceMapper;
import io.improt.vai.mapping.WorkspaceMapper.ClassMapping;
import io.improt.vai.util.FileUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorkspaceMapperPanel extends JPanel {

    private final WorkspaceMapper workspaceMapper;
    private final JTree fileTree;
    private final JTable mappingTable;
    private final MappingTableModel tableModel;
    private final JButton updateAllButton;

    public WorkspaceMapperPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.decode("#F1F3F4"));
        tableModel = new MappingTableModel();

        // Initialize WorkspaceMapper
        workspaceMapper = new WorkspaceMapper();

        File workspaceDir = App.getInstance().getCurrentWorkspace();
        if (workspaceDir == null || !workspaceDir.exists()) {
            add(new JLabel("No workspace is currently open."), BorderLayout.CENTER);
            fileTree = null;
            mappingTable = null;
            updateAllButton = null;
            return;
        }

        // Left Panel: File Tree
        DefaultMutableTreeNode rootNode = createTreeNodes(workspaceDir);
        fileTree = new JTree(rootNode);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                          boolean sel, boolean expanded, boolean leaf, int row,
                                                          boolean hasFocus) {
                Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    Object userObj = node.getUserObject();
                    if (userObj instanceof File) {
                        File file = (File) userObj;
                        setText(file.getName());
                    }
                }
                return c;
            }
        });
        JScrollPane treeScrollPane = new JScrollPane(fileTree);
        treeScrollPane.setPreferredSize(new Dimension(300, 600));

        // Add mouse listener for tree interactions
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                if (path == null) return;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node == null) return;
                Object userObj = node.getUserObject();
                if (!(userObj instanceof File)) {
                    return;
                }
                File file = (File) userObj;

                // Left-click on a file: map it
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                    if (file.isFile()) {
                        workspaceMapper.mapFile(file);
                        refreshMappingTable();
                    }
                }

                // Right-click: show context menu
                if (SwingUtilities.isRightMouseButton(e)) {
                    showTreeContextMenu(e, file);
                }
            }
        });

        // Right Panel: Mapping Table and Update All button
        mappingTable = new JTable(tableModel);
        mappingTable.setFillsViewportHeight(true);
        mappingTable.setRowHeight(25);
        mappingTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        mappingTable.getColumnModel().getColumn(1).setCellRenderer(new MappingStatusRenderer());
        JScrollPane tableScrollPane = new JScrollPane(mappingTable);

        // Mouse listener for table row actions
        mappingTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = mappingTable.rowAtPoint(e.getPoint());
                if (row < 0) return;
                ClassMapping mapping = tableModel.getMappingAt(row);
                File file = new File(mapping.getPath());

                // Left-click: update mapping for that file
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                    workspaceMapper.mapFile(file);
                    refreshMappingTable();
                }

                // Right-click: show context menu for file row
                if (SwingUtilities.isRightMouseButton(e)) {
                    showTableContextMenu(e, file);
                }
            }
        });

        // Update All button
        updateAllButton = new JButton("Update All");
        updateAllButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        updateAllButton.addActionListener(e -> {
            workspaceMapper.mapAllOutdated();
            refreshMappingTable();
        });
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(tableScrollPane, BorderLayout.CENTER);
        rightPanel.add(updateAllButton, BorderLayout.SOUTH);

        // Split Pane: Left (File Tree) and Right (Mapping Table)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, rightPanel);
        splitPane.setDividerLocation(300);
        splitPane.setOneTouchExpandable(true);
        add(splitPane, BorderLayout.CENTER);

        // Initial refresh of the mapping table
        refreshMappingTable();
        
        // Live update the mapping table every second using a Swing Timer
        Timer timer = new Timer(1000, evt -> refreshMappingTable());
        timer.start();
    }

    // Refresh the mapping table model with current mappings
    private void refreshMappingTable() {
        List<ClassMapping> mappings = workspaceMapper.getMappings();
        tableModel.setMappingData(mappings);
    }

    // Create tree nodes recursively based on the file system starting at 'file'
    private DefaultMutableTreeNode createTreeNodes(File file) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(file);
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                // Sort children alphabetically
                List<File> fileList = new ArrayList<>();
                for (File child : children) {
                    String fileName = child.getName().toLowerCase();
                    if (child.isDirectory() || fileName.endsWith(".cs") || fileName.endsWith(".java") || fileName.endsWith(".ts")) {
                        fileList.add(child);
                    }
                }
                fileList.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                for (File child : fileList) {
                    node.add(createTreeNodes(child));
                }
            }
        }
        return node;
    }

    // Show context menu for tree nodes based on file type (file or directory)
    private void showTreeContextMenu(MouseEvent e, File file) {
        JPopupMenu popup = new JPopupMenu();
        if (file.isFile()) {
            JMenuItem mapFile = new JMenuItem("Map File");
            mapFile.addActionListener(ae -> {
                workspaceMapper.mapFile(file);
                refreshMappingTable();
            });

            JMenuItem unmapFile = new JMenuItem("Unmap File");
            unmapFile.addActionListener(ae -> {
                workspaceMapper.removeFile(file);
                refreshMappingTable();
            });
            popup.add(mapFile);
            popup.add(unmapFile);
        } else if (file.isDirectory()) {
            JMenuItem mapDir = new JMenuItem("Map Directory");
            mapDir.addActionListener(ae -> {
                workspaceMapper.addDirectory(file);
                refreshMappingTable();
            });

            JMenuItem updateDir = new JMenuItem("Update Directory");
            updateDir.addActionListener(ae -> {
                workspaceMapper.mapDirectory(file);
                refreshMappingTable();
            });
            
            JMenuItem unmapDir = new JMenuItem("Unmap Directory");
            unmapDir.addActionListener(ae -> {
                workspaceMapper.removeDirectory(file);
                refreshMappingTable();
            });
            popup.add(mapDir);
            popup.add(updateDir);
            popup.add(unmapDir);
        }
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    // Show context menu for table rows (file actions)
    private void showTableContextMenu(MouseEvent e, File file) {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem mapFile = new JMenuItem("Map File");
        mapFile.addActionListener(ae -> {
            workspaceMapper.mapFile(file);
            refreshMappingTable();
        });

        JMenuItem unmapFile = new JMenuItem("Unmap File");
        unmapFile.addActionListener(ae -> {
            workspaceMapper.removeFile(file);
            refreshMappingTable();
        });
        
        JMenuItem updateFile = new JMenuItem("Update File");
        updateFile.addActionListener(ae -> {
            workspaceMapper.mapFile(file);
            refreshMappingTable();
        });
        
        popup.add(mapFile);
        popup.add(unmapFile);
        popup.add(updateFile);
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    // Table model for mapping entries
    private class MappingTableModel extends AbstractTableModel {
        private final String[] columns = {"Path", "Status"};
        private List<ClassMapping> data = new ArrayList<>();

        public void setMappingData(List<ClassMapping> mappings) {
            data = mappings;
            fireTableDataChanged();
        }

        public ClassMapping getMappingAt(int row) {
            return data.get(row);
        }

        @Override
        public int getRowCount() {
            return data.size();
        }
        @Override
        public int getColumnCount() {
            return columns.length;
        }
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ClassMapping cm = data.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    // Return relative path if possible
                    File workspace = App.getInstance().getCurrentWorkspace();
                    File file = new File(cm.getPath());
                    if (workspace != null) {
                        try {
                            return workspace.toURI().relativize(file.toURI()).getPath();
                        } catch (Exception ex) {
                            return cm.getPath();
                        }
                    }
                    return cm.getPath();
                case 1:
                    if (cm.getMapping() == null || cm.getMapping().isEmpty()) {
                        return "Unmapped";
                    } else if (!cm.isUpToDate()) {
                        return "Outdated";
                    } else {
                        return "OK";
                    }
                default:
                    return "";
            }
        }
        @Override
        public String getColumnName(int column) {
            return columns[column];
        }
    }

    // Custom cell renderer for the "Status" column to color-code rows
    private class MappingStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = (String) value;
            if ("Unmapped".equalsIgnoreCase(status)) {
                c.setBackground(Color.DARK_GRAY);
                c.setForeground(Color.WHITE);
            } else if ("Outdated".equalsIgnoreCase(status)) {
                c.setBackground(Color.RED);
                c.setForeground(Color.WHITE);
            } else if ("OK".equalsIgnoreCase(status)) {
                c.setBackground(Color.GREEN);
                c.setForeground(Color.BLACK);
            } else {
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
            }
            if (isSelected) {
                c.setBackground(c.getBackground().darker());
            }
            return c;
        }
    }
}
