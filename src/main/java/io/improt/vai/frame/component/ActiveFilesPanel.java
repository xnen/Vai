package io.improt.vai.frame.component;

import io.improt.vai.backend.App;
import io.improt.vai.llm.providers.impl.IModelProvider;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
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
        setBorder(BorderFactory.createTitledBorder("LLM"));

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

        // Custom cell renderer to highlight unsupported files
        // TODO: This doesn't refresh when the model changes.
        fileTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String fileName = (String) tableModel.getValueAt(row, 0);
                String modelName = (String) App.getInstance().getClient().getModelList().getSelectedItem();
                IModelProvider provider = App.getInstance().getLLMProvider(modelName);

                if (provider != null) {
                    if (!provider.supportsAudio() && isAudioFile(fileName)) {
                        c.setForeground(Color.RED);
                    } else if (!provider.supportsVision() && isImageFile(fileName)) {
                        c.setForeground(Color.RED);
                    } else if (!provider.supportsVideo() && isVideoFile(fileName)) {
                        c.setForeground(Color.RED);
                    } else {
                        c.setForeground(Color.BLACK); // Default color
                    }
                } else {
                    c.setForeground(Color.BLACK); // Default color if no provider
                }
                return c;
            }
        });

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
                        backend.getActiveFileManager().addFile(file);
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
                        backend.getActiveFileManager().removeFile(fileName);
                        refreshTable();
                    });
                    contextMenu.add(deleteItem);

                    // Add Clear Active option to the context menu
                    JMenuItem clearActiveItem = new JMenuItem("Clear Active");
                    clearActiveItem.addActionListener(event -> {
                        backend.getActiveFileManager().clearActiveFiles();
                        refreshTable();
                    });
                    contextMenu.addSeparator();
                    contextMenu.add(clearActiveItem);

                    contextMenu.show(fileTable, e.getX(), e.getY());
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        // Double-click to delete
                        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                        backend.getActiveFileManager().removeFile(fileName);
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

    private boolean isAudioFile(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".wav") || lowerFileName.endsWith(".mp3"); // Add more audio extensions if needed
    }

    private boolean isImageFile(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".png") || lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg"); // Add more image extensions if needed
    }

    private boolean isVideoFile(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".mp4") || lowerFileName.endsWith(".avi"); // Add more video extensions if needed
    }

    public void refreshTable() {
        List<File> enabledFiles = backend.getEnabledFiles();
        tableModel.setRowCount(0); // Clear existing rows
        File workspace = backend.getCurrentWorkspace();
        for (File file : enabledFiles) {
            String relativePath = file.getAbsolutePath();
            if (workspace != null) {
                try {
                    relativePath = workspace.toPath().relativize(file.toPath()).toString();
                } catch(Exception e) {
                    // fallback to absolute path on error
                }
            }
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
                    Thread.sleep(200);
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
