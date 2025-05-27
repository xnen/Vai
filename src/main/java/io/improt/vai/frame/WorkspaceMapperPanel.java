package io.improt.vai.frame;

import io.improt.vai.backend.App;
import io.improt.vai.mapping.WorkspaceMapper;
import io.improt.vai.mapping.WorkspaceMapper.ClassMapping;
import io.improt.vai.mapping.SubWorkspace; 
import io.improt.vai.util.FileUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WorkspaceMapperPanel extends JPanel {

    private final WorkspaceMapper workspaceMapper;
    private final JTree fileTree;
    private final JTable mappingTable;
    private final MappingTableModel tableModel;
    private final JButton updateAllButton;

    // Components for SubWorkspace Management
    private JList<SubWorkspace> subWorkspaceList;
    private DefaultListModel<SubWorkspace> subWorkspaceListModel;
    private JList<String> filesInSubWorkspaceList;
    private DefaultListModel<String> filesInSubWorkspaceListModel;
    private JButton createSubWorkspaceButton;
    private JButton deleteSubWorkspaceButton;
    private JButton addTreeSelectionToSubWorkspaceButton; 
    private JButton removeFilesFromSubWorkspaceButton;


    public WorkspaceMapperPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.decode("#F1F3F4"));
        
        App appInstance = App.getInstance();
        File workspaceDir = appInstance.getCurrentWorkspace();

        if (workspaceDir == null || !workspaceDir.exists()) {
            add(new JLabel("No workspace is currently open. Please open a workspace and reopen this panel."), BorderLayout.CENTER);
            this.workspaceMapper = null;
            this.fileTree = null;
            this.mappingTable = null;
            this.tableModel = null;
            this.updateAllButton = null;
            this.subWorkspaceList = null;
            this.filesInSubWorkspaceList = null;
            return;
        }

        this.workspaceMapper = new WorkspaceMapper(workspaceDir);
        this.tableModel = new MappingTableModel();


        DefaultMutableTreeNode rootNode = createTreeNodes(workspaceDir);
        fileTree = new JTree(rootNode);
        fileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION); 
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
        treeScrollPane.setPreferredSize(new Dimension(300, 400));

        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) { 
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) return;
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node == null || !(node.getUserObject() instanceof File)) return;
                    File file = (File) node.getUserObject();
                    Window owner = SwingUtilities.getWindowAncestor(WorkspaceMapperPanel.this);
                    if (file.isFile()) {
                        // For single file, no progress dialog, map directly. Listener is null.
                        workspaceMapper.mapFile(file, null);
                        refreshMappingTable();
                    } else if (file.isDirectory()) {
                        int confirm = JOptionPane.showConfirmDialog(
                            WorkspaceMapperPanel.this,
                            "Map all valid files in directory '" + file.getName() + "' and its subdirectories?\nThis may take a moment.",
                            "Confirm Map Directory",
                            JOptionPane.YES_NO_OPTION
                        );
                        if (confirm == JOptionPane.YES_OPTION) {
                            workspaceMapper.mapDirectory(file, owner); 
                            // Dialog handles its own updates; table will refresh via timer or after dialog closes
                        }
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) { 
                     TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                     if (path == null) return;
                     DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                     if (node != null && node.getUserObject() instanceof File) {
                         File file = (File) node.getUserObject();
                         fileTree.setSelectionPath(path); 
                         showTreeContextMenu(e, file);
                     }
                }
            }
        });

        mappingTable = new JTable(tableModel);
        mappingTable.setFillsViewportHeight(true);
        mappingTable.setRowHeight(25);
        mappingTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        mappingTable.getColumnModel().getColumn(1).setCellRenderer(new MappingStatusRenderer());
        JScrollPane tableScrollPane = new JScrollPane(mappingTable);

        mappingTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = mappingTable.rowAtPoint(e.getPoint());
                if (row < 0) return;
                ClassMapping mapping = tableModel.getMappingAt(row);
                File file = new File(mapping.getPath());

                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) { 
                    workspaceMapper.mapFile(file, null); // Single file, no progress dialog
                    refreshMappingTable();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    mappingTable.setRowSelectionInterval(row, row); 
                    showTableContextMenu(e, file);
                }
            }
        });
        
        updateAllButton = new JButton("Update All Outdated Mappings");
        updateAllButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        updateAllButton.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(WorkspaceMapperPanel.this);
            workspaceMapper.mapAllOutdated(owner);
            // Dialog handles its own updates; table will refresh via timer or after dialog closes
        });

        JPanel mappingManagementPanel = new JPanel(new BorderLayout());
        mappingManagementPanel.add(tableScrollPane, BorderLayout.CENTER);
        mappingManagementPanel.add(updateAllButton, BorderLayout.SOUTH);

        JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, mappingManagementPanel);
        topSplitPane.setDividerLocation(350);
        topSplitPane.setResizeWeight(0.3);


        JPanel subWorkspaceManagementPanel = new JPanel(new BorderLayout(5,5));
        subWorkspaceManagementPanel.setBorder(BorderFactory.createTitledBorder("Sub-Workspace Management"));

        subWorkspaceListModel = new DefaultListModel<>();
        subWorkspaceList = new JList<>(subWorkspaceListModel);
        subWorkspaceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subWorkspaceList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshFilesInSelectedSubWorkspace(); 
            }
        });
        JScrollPane subWorkspaceListScrollPane = new JScrollPane(subWorkspaceList);
        subWorkspaceListScrollPane.setPreferredSize(new Dimension(200, 150));

        filesInSubWorkspaceListModel = new DefaultListModel<>();
        filesInSubWorkspaceList = new JList<>(filesInSubWorkspaceListModel);
        filesInSubWorkspaceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        filesInSubWorkspaceList.addListSelectionListener(e -> { 
            if (!e.getValueIsAdjusting()) {
                 SubWorkspace selectedSw = subWorkspaceList.getSelectedValue();
                 boolean isSwSelected = selectedSw != null;
                 boolean isDirBased = isSwSelected && selectedSw.isDirectoryBased();
                 removeFilesFromSubWorkspaceButton.setEnabled(isSwSelected && !isDirBased && filesInSubWorkspaceList.getSelectedIndex() != -1);
            }
        });
        JScrollPane filesInSubWorkspaceScrollPane = new JScrollPane(filesInSubWorkspaceList);
        filesInSubWorkspaceScrollPane.setPreferredSize(new Dimension(400, 150));

        createSubWorkspaceButton = new JButton("New Sub-Workspace");
        createSubWorkspaceButton.addActionListener(e -> createNewSubWorkspace());

        deleteSubWorkspaceButton = new JButton("Delete Sub-Workspace");
        deleteSubWorkspaceButton.addActionListener(e -> deleteSelectedSubWorkspace());

        addTreeSelectionToSubWorkspaceButton = new JButton("<< Add Selected from Tree");
        addTreeSelectionToSubWorkspaceButton.addActionListener(e -> addTreeSelectionsToActiveSubWorkspace());
        
        removeFilesFromSubWorkspaceButton = new JButton("Remove Selected Files from List >>");
        removeFilesFromSubWorkspaceButton.setToolTipText("For File-based: Removes selected files. For Dir-based: Disabled (manage directories by re-creating SW or adding more).");
        removeFilesFromSubWorkspaceButton.addActionListener(e -> removeSelectedFilesFromSubWorkspace());

        JPanel subWorkspaceButtonsPanel = new JPanel(new GridLayout(0, 1, 5, 5)); 
        subWorkspaceButtonsPanel.add(createSubWorkspaceButton);
        subWorkspaceButtonsPanel.add(deleteSubWorkspaceButton);
        
        JPanel subWorkspaceFilesButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        subWorkspaceFilesButtonPanel.add(addTreeSelectionToSubWorkspaceButton);
        subWorkspaceFilesButtonPanel.add(removeFilesFromSubWorkspaceButton);


        JPanel subWorkspaceListsPanel = new JPanel(new BorderLayout(5,5));
        subWorkspaceListsPanel.add(subWorkspaceListScrollPane, BorderLayout.WEST);
        subWorkspaceListsPanel.add(filesInSubWorkspaceScrollPane, BorderLayout.CENTER);
        subWorkspaceListsPanel.add(subWorkspaceFilesButtonPanel, BorderLayout.SOUTH);
        
        subWorkspaceManagementPanel.add(subWorkspaceListsPanel, BorderLayout.CENTER);
        subWorkspaceManagementPanel.add(subWorkspaceButtonsPanel, BorderLayout.WEST);


        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplitPane, subWorkspaceManagementPanel);
        mainSplitPane.setDividerLocation(450); 
        mainSplitPane.setResizeWeight(0.6); 
        add(mainSplitPane, BorderLayout.CENTER);

        refreshMappingTable();
        refreshSubWorkspaceList();
        
        Timer mappingRefreshTimer = new Timer(2000, evt -> refreshMappingTable()); // Periodically refresh table
        mappingRefreshTimer.start();
    }

    private void refreshMappingTable() {
        if (workspaceMapper == null || tableModel == null) return;
        List<ClassMapping> mappings = workspaceMapper.getMappings();
        tableModel.setMappingData(mappings);
    }

    private void refreshSubWorkspaceList() {
        if (subWorkspaceListModel == null || App.getInstance().getCurrentWorkspace() == null) return;
        
        int selectedIdx = subWorkspaceList.getSelectedIndex(); 
        subWorkspaceListModel.clear();
        List<SubWorkspace> sws = App.getInstance().getSubWorkspaces();
        for (SubWorkspace sw : sws) {
            subWorkspaceListModel.addElement(sw);
        }

        if (selectedIdx >= 0 && selectedIdx < subWorkspaceListModel.getSize()) {
            subWorkspaceList.setSelectedIndex(selectedIdx);
        } else if (!subWorkspaceListModel.isEmpty()) {
            subWorkspaceList.setSelectedIndex(0);
        }
        
        refreshFilesInSelectedSubWorkspace(); 
    }

    private void refreshFilesInSelectedSubWorkspace() {
        if (filesInSubWorkspaceListModel == null || App.getInstance().getCurrentWorkspace() == null) return;
        filesInSubWorkspaceListModel.clear();
        
        SubWorkspace selectedSw = subWorkspaceList.getSelectedValue();
        boolean isSwSelected = selectedSw != null;
        boolean isDirBased = isSwSelected && selectedSw.isDirectoryBased();

        addTreeSelectionToSubWorkspaceButton.setEnabled(isSwSelected);
        removeFilesFromSubWorkspaceButton.setEnabled(isSwSelected && !isDirBased && !filesInSubWorkspaceListModel.isEmpty());


        if (selectedSw != null) {
            File workspaceRoot = App.getInstance().getCurrentWorkspace();
            List<String> filePaths = new ArrayList<>(selectedSw.getFilePaths()); 
            Collections.sort(filePaths); 
            for (String absolutePath : filePaths) {
                try {
                    String relativePath = workspaceRoot.toURI().relativize(new File(absolutePath).toURI()).getPath();
                    filesInSubWorkspaceListModel.addElement(relativePath);
                } catch (Exception ex) {
                    filesInSubWorkspaceListModel.addElement(absolutePath); 
                }
            }
             removeFilesFromSubWorkspaceButton.setEnabled(isSwSelected && !isDirBased && !filesInSubWorkspaceListModel.isEmpty() && filesInSubWorkspaceList.getSelectedIndex() != -1);
        } else {
            addTreeSelectionToSubWorkspaceButton.setEnabled(false);
            removeFilesFromSubWorkspaceButton.setEnabled(false);
        }
    }
    
    private void createNewSubWorkspace() {
        String name = JOptionPane.showInputDialog(this, "Enter name for the new Sub-Workspace:", "New Sub-Workspace", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        name = name.trim();

        if (App.getInstance().getSubWorkspaceByName(name) != null) {
            JOptionPane.showMessageDialog(this, "A Sub-Workspace with this name already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Object[] options = {"File-based (select individual files)", "Directory-based (monitor selected directories from tree)"};
        int choice = JOptionPane.showOptionDialog(this,
                "Choose the type of Sub-Workspace:",
                "Sub-Workspace Type",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        SubWorkspace newSw = null;
        if (choice == JOptionPane.YES_OPTION) { 
            newSw = new SubWorkspace(name);
            newSw.setDirectoryBased(false);
        } else if (choice == JOptionPane.NO_OPTION) { 
            newSw = new SubWorkspace(name);
            newSw.setDirectoryBased(true);
             JOptionPane.showMessageDialog(this,
                    "Directory-based Sub-Workspace '" + name + "' created.\n" +
                    "Now, select director(y/ies) from the file tree on the left and click '" +
                    addTreeSelectionToSubWorkspaceButton.getText() + "' to add them for monitoring.",
                    "Next Steps", JOptionPane.INFORMATION_MESSAGE);
        } else {
            return; 
        }

        if (newSw != null) {
            App.getInstance().addSubWorkspace(newSw);
            refreshSubWorkspaceList();
            for (int i = 0; i < subWorkspaceListModel.getSize(); i++) {
                if (subWorkspaceListModel.getElementAt(i).getName().equals(newSw.getName())) {
                    subWorkspaceList.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void deleteSelectedSubWorkspace() {
        SubWorkspace selectedSw = subWorkspaceList.getSelectedValue();
        if (selectedSw != null) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete Sub-Workspace '" + selectedSw.getName() + "'?\nThis will not delete the actual files or their mappings.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                App.getInstance().removeSubWorkspace(selectedSw.getName());
                refreshSubWorkspaceList();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a Sub-Workspace to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void addTreeSelectionsToActiveSubWorkspace() {
        SubWorkspace selectedSw = subWorkspaceList.getSelectedValue();
        if (selectedSw == null) {
            JOptionPane.showMessageDialog(this, "Please select a Sub-Workspace first.", "No Sub-Workspace Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        TreePath[] selectedTreePaths = fileTree.getSelectionPaths();
        if (selectedTreePaths == null || selectedTreePaths.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select file(s) or director(y/ies) from the tree on the left.", "No Tree Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        SubWorkspace mutableSelectedSw = App.getInstance().getSubWorkspaceByName(selectedSw.getName()); 
        if(mutableSelectedSw == null) {
            JOptionPane.showMessageDialog(this, "Error: Could not find the sub-workspace to modify.", "Error", JOptionPane.ERROR_MESSAGE);
            return; 
        }

        boolean changed = false;
        if (mutableSelectedSw.isDirectoryBased()) {
            int dirsAdded = 0;
            for (TreePath treePath : selectedTreePaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                if (node.getUserObject() instanceof File) {
                    File fileOrDir = (File) node.getUserObject();
                    if (fileOrDir.isDirectory()) {
                        mutableSelectedSw.addMonitoredDirectoryPath(fileOrDir.getAbsolutePath());
                        changed = true;
                        dirsAdded++;
                    }
                }
            }
            if (dirsAdded == 0) {
                JOptionPane.showMessageDialog(this, "No directories were selected from the tree to add for monitoring.", "No Directories Selected", JOptionPane.INFORMATION_MESSAGE);
            }

        } else { 
            List<File> filesToAdd = new ArrayList<>();
            for (TreePath treePath : selectedTreePaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                if (node.getUserObject() instanceof File) {
                    File fileOrDir = (File) node.getUserObject();
                    collectFiles(fileOrDir, filesToAdd); 
                }
            }
            
            if (filesToAdd.isEmpty()) {
                 JOptionPane.showMessageDialog(this, "No valid files found in the tree selection to add.", "No Valid Files Selected", JOptionPane.INFORMATION_MESSAGE);
                 return;
            }

            for (File file : filesToAdd) {
                if (!workspaceMapper.getMappings().stream().anyMatch(m -> m.getPath().equals(file.getAbsolutePath()))) {
                    workspaceMapper.addFile(file); 
                    workspaceMapper.mapFile(file, null); // Map individually, no progress dialog for this part
                }
                mutableSelectedSw.addFilePath(file.getAbsolutePath()); 
                changed = true;
            }
        }

        if (changed) {
            App.getInstance().updateSubWorkspace(mutableSelectedSw); 
            refreshFilesInSelectedSubWorkspace();
            if (!mutableSelectedSw.isDirectoryBased()) { 
                refreshMappingTable(); 
            }
        }
    }

    private void collectFiles(File fileOrDir, List<File> collectedFiles) {
        if (fileOrDir.isFile()) {
            if (WorkspaceMapper.hasValidExtension(fileOrDir.getName().toLowerCase())) { 
                 if (!collectedFiles.stream().anyMatch(f -> f.getAbsolutePath().equals(fileOrDir.getAbsolutePath()))) {
                    collectedFiles.add(fileOrDir);
                 }
            }
        } else if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    collectFiles(child, collectedFiles); 
                }
            }
        }
    }


    private void removeSelectedFilesFromSubWorkspace() {
        SubWorkspace selectedSw = subWorkspaceList.getSelectedValue();
        if (selectedSw == null) {
            JOptionPane.showMessageDialog(this, "Please select a Sub-Workspace first.", "No Sub-Workspace Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedSw.isDirectoryBased()) {
             JOptionPane.showMessageDialog(this, "Cannot remove individual files from a directory-based Sub-Workspace.\nFiles are determined by the monitored directories. To change content, modify the monitored directories (e.g., by re-creating the Sub-Workspace or via a future edit feature).", "Operation Not Allowed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<String> selectedRelativePaths = filesInSubWorkspaceList.getSelectedValuesList();
        if (selectedRelativePaths.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select file(s) from the list of files in the Sub-Workspace.", "No Files Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File workspaceRoot = App.getInstance().getCurrentWorkspace();
        boolean changed = false;
        SubWorkspace mutableSelectedSw = App.getInstance().getSubWorkspaceByName(selectedSw.getName());
        if(mutableSelectedSw == null || mutableSelectedSw.isDirectoryBased()) return; 

        for (String relativePath : selectedRelativePaths) {
            File absoluteFile = new File(workspaceRoot, relativePath);
            mutableSelectedSw.removeFilePath(absoluteFile.getAbsolutePath());
            changed = true;
        }

        if (changed) {
            App.getInstance().updateSubWorkspace(mutableSelectedSw);
            refreshFilesInSelectedSubWorkspace();
        }
    }

    private DefaultMutableTreeNode createTreeNodes(File file) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(file); 
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                List<File> fileList = Arrays.stream(children)
                    .filter(child -> child.isDirectory() || WorkspaceMapper.hasValidExtension(child.getName().toLowerCase()))
                    .collect(Collectors.toList());
                fileList.sort((f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                });
                for (File child : fileList) {
                    node.add(createTreeNodes(child));
                }
            }
        }
        return node;
    }


    private void showTreeContextMenu(MouseEvent e, File file) {
        JPopupMenu popup = new JPopupMenu();
        if (file.isFile()) {
            JMenuItem mapFileItem = new JMenuItem("Map File (Update Mapping)");
            mapFileItem.addActionListener(ae -> {
                workspaceMapper.mapFile(file, null); // Single file, no progress dialog
                refreshMappingTable();
            });

            JMenuItem unmapFileItem = new JMenuItem("Unmap File (Remove from Mappings)");
            unmapFileItem.addActionListener(ae -> {
                workspaceMapper.removeFile(file); 
                for(SubWorkspace sw : App.getInstance().getSubWorkspaces()){
                    if (!sw.isDirectoryBased()) {
                        SubWorkspace mutableSw = App.getInstance().getSubWorkspaceByName(sw.getName());
                        if(mutableSw != null && mutableSw.getFilePaths().contains(file.getAbsolutePath())){
                            mutableSw.removeFilePath(file.getAbsolutePath());
                            App.getInstance().updateSubWorkspace(mutableSw);
                        }
                    }
                }
                refreshMappingTable();
                refreshFilesInSelectedSubWorkspace(); 
            });
            popup.add(mapFileItem);
            popup.add(unmapFileItem);
        } else if (file.isDirectory()) {
            JMenuItem mapDirItem = new JMenuItem("Map All in Directory (Update Mappings)");
            mapDirItem.addActionListener(ae -> {
                Window owner = SwingUtilities.getWindowAncestor(WorkspaceMapperPanel.this);
                workspaceMapper.mapDirectory(file, owner); 
            });
            
            JMenuItem unmapDirItem = new JMenuItem("Unmap All in Directory (Remove from Mappings)");
            unmapDirItem.addActionListener(ae -> {
                List<File> filesInDir = new ArrayList<>();
                collectFiles(file, filesInDir); 
                for(File f : filesInDir){
                    workspaceMapper.removeFile(f); 
                    for(SubWorkspace sw : App.getInstance().getSubWorkspaces()){
                         if (!sw.isDirectoryBased()) {
                            SubWorkspace mutableSw = App.getInstance().getSubWorkspaceByName(sw.getName());
                            if(mutableSw != null && mutableSw.getFilePaths().contains(f.getAbsolutePath())){
                                mutableSw.removeFilePath(f.getAbsolutePath());
                                App.getInstance().updateSubWorkspace(mutableSw);
                            }
                        }
                    }
                }
                refreshMappingTable();
                refreshFilesInSelectedSubWorkspace();
            });
            popup.add(mapDirItem);
            popup.add(unmapDirItem);
        }
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showTableContextMenu(MouseEvent e, File file) {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem updateMappingItem = new JMenuItem("Update Mapping");
        updateMappingItem.addActionListener(ae -> {
            workspaceMapper.mapFile(file, null); // Single file, no progress dialog
            refreshMappingTable();
        });

        JMenuItem removeFromMappingsItem = new JMenuItem("Remove from Mappings");
        removeFromMappingsItem.addActionListener(ae -> {
            workspaceMapper.removeFile(file);
            for(SubWorkspace sw : App.getInstance().getSubWorkspaces()){
                if (!sw.isDirectoryBased()) {
                    SubWorkspace mutableSw = App.getInstance().getSubWorkspaceByName(sw.getName());
                    if(mutableSw != null && mutableSw.getFilePaths().contains(file.getAbsolutePath())){
                        mutableSw.removeFilePath(file.getAbsolutePath());
                        App.getInstance().updateSubWorkspace(mutableSw);
                    }
                }
            }
            refreshMappingTable();
            refreshFilesInSelectedSubWorkspace();
        });
        
        popup.add(updateMappingItem);
        popup.add(removeFromMappingsItem);
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private class MappingTableModel extends AbstractTableModel {
        private final String[] columns = {"Relative Path", "Mapping Status"};
        private List<ClassMapping> data = new ArrayList<>();

        public void setMappingData(List<ClassMapping> mappings) {
            File workspace = App.getInstance().getCurrentWorkspace();
            if (workspace != null) {
                 mappings.sort((cm1, cm2) -> {
                    String path1 = getRelativePath(cm1.getPath(), workspace);
                    String path2 = getRelativePath(cm2.getPath(), workspace);
                    return path1.compareToIgnoreCase(path2);
                });
            }
            this.data = mappings;
            fireTableDataChanged();
        }
        
        private String getRelativePath(String absolutePath, File workspaceRoot) {
            if (workspaceRoot == null || absolutePath == null) return absolutePath;
            try {
                String relPath = workspaceRoot.toURI().relativize(new File(absolutePath).toURI()).getPath();
                // Handle case where workspaceRoot might be the filesystem root, causing leading "/"
                if (relPath.startsWith("/") && workspaceRoot.getParentFile() == null) {
                    return relPath.substring(1);
                }
                return relPath;
            } catch (Exception ex) {
                return absolutePath; 
            }
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
                    File workspace = App.getInstance().getCurrentWorkspace();
                    return getRelativePath(cm.getPath(), workspace);
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

    private class MappingStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = (String) value;
            if ("Unmapped".equalsIgnoreCase(status)) {
                c.setBackground(Color.decode("#E0E0E0")); 
                c.setForeground(Color.BLACK);
            } else if ("Outdated".equalsIgnoreCase(status)) {
                c.setBackground(Color.decode("#FFCDD2")); 
                c.setForeground(Color.BLACK);
            } else if ("OK".equalsIgnoreCase(status)) {
                c.setBackground(Color.decode("#C8E6C9")); 
                c.setForeground(Color.BLACK);
            } else {
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
            }
            if (isSelected) {
                c.setBackground(table.getSelectionBackground()); 
                c.setForeground(table.getSelectionForeground());
            }
            return c;
        }
    }
}
