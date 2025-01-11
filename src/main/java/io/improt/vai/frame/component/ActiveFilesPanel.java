package io.improt.vai.frame.component;

import io.improt.vai.backend.App;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ActiveFilesPanel extends JPanel {
    private final JTable fileTable;
    private final DefaultTableModel tableModel;
    private final App backend;
    
    private FileSelectionListener fileSelectionListener;

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
        
        // Enable drag and drop
        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDrop()) {
                    return false;
                }
                if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                try {
                    Transferable t = support.getTransferable();
                    List<File> droppedFiles = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : droppedFiles) {
                        backend.addFile(file);
                    }
                    refreshTable();
                    return true;
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                }

                return false;
            }
        });
        
        // Add mouse listener for context menu, single-click selection, and double-click deletion
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
                        refreshTable();
                    });
                    contextMenu.add(deleteItem);
                    
                    // Add Clear Active option to the context menu
                    JMenuItem clearActiveItem = new JMenuItem("Clear Active");
                    clearActiveItem.addActionListener(event -> {
                        backend.clearActiveFiles();
                        refreshTable();
                        JOptionPane.showMessageDialog(null, "All active files have been cleared.");
                    });
                    contextMenu.addSeparator();
                    contextMenu.add(clearActiveItem);
                    
                    contextMenu.show(fileTable, e.getX(), e.getY());
                }
                
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        // Double-click to delete
                        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                        backend.removeFile(fileName);
                        refreshTable();
                    } else if (e.getClickCount() == 1) {
                        // Single-click to open in File Viewer
                        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                        String filePath = (String) tableModel.getValueAt(selectedRow, 1);
                        File file = new File(filePath);
                        if (fileSelectionListener != null && file.exists() && file.isFile()) {
                            fileSelectionListener.onFileSelected(file);
                        }
                    }
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

    /**
     * Sets the listener for file selection events.
     *
     * @param listener The FileSelectionListener to set.
     */
    public void setFileSelectionListener(FileSelectionListener listener) {
        this.fileSelectionListener = listener;
    }

    /**
     * Interface for listening to file selection events.
     */
    public interface FileSelectionListener {
        void onFileSelected(File file);
    }
}