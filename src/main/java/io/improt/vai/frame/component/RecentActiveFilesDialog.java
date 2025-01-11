package io.improt.vai.frame.component;

import io.improt.vai.backend.App;
import io.improt.vai.util.FileUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecentActiveFilesDialog extends JDialog {
    private final JTable recentFilesTable;
    private final DefaultTableModel tableModel;
    private final JButton addButton;
    private final App backend;
    private final List<String> recentlyActiveFiles;

    public RecentActiveFilesDialog(JFrame parent) {
        super(parent, "Recent Active Files", false); // Set modal to false to make it non-blocking
        setSize(600, 400);
        setLocationRelativeTo(parent);

        backend = App.getInstance();
        recentlyActiveFiles = FileUtils.loadRecentlyActiveFiles(backend.getCurrentWorkspace());

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

        JScrollPane scrollPane = new JScrollPane(recentFilesTable);

        // Add button
        addButton = new JButton("Add Selected");
        addButton.addActionListener(e -> addSelectedFiles());

        // Layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
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

    private void addSelectedFiles() {
        int[] selectedRows = recentFilesTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "No files selected.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<File> filesToAdd = new ArrayList<>();
        for (int row : selectedRows) {
            String filePath = (String) tableModel.getValueAt(row, 1);
            File file = new File(filePath);
            if (file.exists() && file.isFile() && !backend.getEnabledFiles().contains(file)) {
                filesToAdd.add(file);
            }
        }

        if (filesToAdd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No valid files to add.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        for (File file : filesToAdd) {
            backend.addFile(file);
        }

        JOptionPane.showMessageDialog(this, filesToAdd.size() + " file(s) added to active files.", "Success", JOptionPane.INFORMATION_MESSAGE);
        this.dispose(); // Close the dialog after adding
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
            if (backend.getEnabledFiles().contains(file)) {
                c.setBackground(activeColor);
            } else {
                c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            }

            return c;
        }
    }
}