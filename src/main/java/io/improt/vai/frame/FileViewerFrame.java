package io.improt.vai.frame;

import io.improt.vai.backend.App;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class FileViewerFrame extends JFrame {
    private JList<String> fileList;
    private DefaultListModel<String> listModel;

    public FileViewerFrame() {
        setTitle("Active Files");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Context menu
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> {
            String selectedFile = fileList.getSelectedValue();
            if (selectedFile != null) {
                App.getInstance().removeFile(selectedFile);
            }
        });
        contextMenu.add(deleteItem);

        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && fileList.getSelectedValue() != null) {
                    contextMenu.show(fileList, e.getX(), e.getY());
                }
            }
        });

        add(new JScrollPane(fileList), BorderLayout.CENTER);

        // Start the watchdog thread
        startWatchdogThread();
        setVisible(true);
    }

    private int lastSize;

    private void startWatchdogThread() {
        Thread watchdogThread = new Thread(() -> {
            while (true) {
                try {
                    List<File> enabledFiles = App.getInstance().getEnabledFiles();
                    if (enabledFiles.size() != lastSize) {
                        SwingUtilities.invokeLater(() -> {
                            listModel.clear();
                            for (File file : enabledFiles) {
                                listModel.addElement(file.getName());
                            }
                        });
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