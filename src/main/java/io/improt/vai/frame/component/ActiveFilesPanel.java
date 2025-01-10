package io.improt.vai.frame.component;

import io.improt.vai.backend.App;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class ActiveFilesPanel extends JPanel {
    private final JTable fileTable;
    private final DefaultTableModel tableModel;
    private final App backend;

    public ActiveFilesPanel(App backend) {
        this.backend = backend;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Active Files"));
        
        // Initialize table model with column names
        tableModel = new DefaultTableModel(new Object[]{"File Name", "Path"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        
        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileTable.setFillsViewportHeight(true);
        
        // Add mouse listener for context menu and double-click deletion
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = fileTable.getSelectedRow();
                if (selectedRow == -1) {
                    return; // No row selected
                }

                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu contextMenu = new JPopupMenu();
                    JMenuItem deleteItem = new JMenuItem("Delete");
                    deleteItem.addActionListener(event -> {
                        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                        backend.removeFile(fileName);
                    });
                    contextMenu.add(deleteItem);
                    contextMenu.show(fileTable, e.getX(), e.getY());
                }
                
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                    backend.removeFile(fileName);
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(fileTable);
        add(scrollPane, BorderLayout.CENTER);

        // Start the watchdog thread
        startWatchdogThread();
    }

    public void refreshTable() {
        List<File> enabledFiles = backend.getEnabledFiles();
        tableModel.setRowCount(0); // Clear existing rows
        for (File file : enabledFiles) {
            tableModel.addRow(new Object[]{file.getName(), file.getAbsolutePath()});
        }
    }

    private int lastSize;

    private void startWatchdogThread() {
        Thread watchdogThread = new Thread(() -> {
            while (true) {
                try {
                    List<File> enabledFiles = App.getInstance().getEnabledFiles();
                    if (enabledFiles.size() != lastSize) {
                        refreshTable();
                    }

                    lastSize = enabledFiles.size();
                    // Sleep for 1 second before checking again
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        watchdogThread.setDaemon(true);
        watchdogThread.start();
    }
}