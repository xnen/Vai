package io.improt.vai.frame.component;

import io.improt.vai.backend.App;
import io.improt.vai.backend.ActiveFileManager;
import io.improt.vai.util.FileUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class RecentActiveFilesPanel extends JPanel implements ActiveFileManager.EnabledFilesChangeListener {
    private final JTable recentFilesTable;
    private final DefaultTableModel tableModel;
    private final App backend;
    private List<String> recentlyActiveFiles; // Removed 'final' to allow refreshing

    public RecentActiveFilesPanel() {
        this.backend = App.getInstance();
        this.recentlyActiveFiles = FileUtils.loadRecentlyActiveFiles(backend.getCurrentWorkspace());
        setBorder(BorderFactory.createTitledBorder("Recent"));

        setLayout(new BorderLayout());

        // Initialize table model with columns
        tableModel = new DefaultTableModel(new Object[]{"File Name", "Path"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };

        recentFilesTable = new JTable(tableModel);
        recentFilesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        recentFilesTable.setFillsViewportHeight(true);

        // Populate the table
        populateTable();

        // Highlight active files in green
        recentFilesTable.setDefaultRenderer(Object.class, new RecentFileTableCellRenderer());

        // Add mouse listener for single-click toggle
        recentFilesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = recentFilesTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    toggleFileAtRow(row);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(recentFilesTable);

        // Add components to this panel
        add(scrollPane, BorderLayout.CENTER);

        // Register this panel as a listener to enabledFiles changes
        if (backend.getActiveFileManager() != null) {
            backend.getActiveFileManager().addEnabledFilesChangeListener(this);
        }
    }

    private void populateTable() {
        tableModel.setRowCount(0); // Clear existing rows
        for (String filePath : recentlyActiveFiles) {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                tableModel.addRow(new Object[]{file.getName(), file.getAbsolutePath()});
            }
        }
    }

    private void toggleFileAtRow(int row) {
        String filePath = (String) tableModel.getValueAt(row, 1);
        File file = new File(filePath);
        if (backend.getActiveFileManager().isFileActive(file)) {
            boolean removed = backend.getActiveFileManager().removeFile(file);

            if (!removed) {
                JOptionPane.showMessageDialog(this, "Failed to remove file from active files: " + file.getName(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            backend.getActiveFileManager().addFile(file);
        }

        // Update the table to reflect changes
        recentFilesTable.repaint();
    }

    // Custom Cell Renderer to highlight active files in green
    private class RecentFileTableCellRenderer extends DefaultTableCellRenderer {
        private final Color activeColor = new Color(144, 238, 144); // Light Green

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String filePath = (String) table.getValueAt(row, 1);
            File file = new File(filePath);
            if (backend.getActiveFileManager().isFileActive(file)) {
                c.setBackground(activeColor);
            } else {
                c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            }

            return c;
        }
    }

    @Override
    public void onEnabledFilesChanged(List<File> updatedEnabledFiles) {
        // Ensure UI updates are performed on the Event Dispatch Thread
        SwingUtilities.invokeLater(recentFilesTable::repaint);
    }

    // New method to refresh the recent active files
    public void refresh() {
        recentlyActiveFiles = FileUtils.loadRecentlyActiveFiles(backend.getCurrentWorkspace());
        populateTable();
    }
}
