package io.improt.vai.frame.dialogs;

import io.improt.vai.mapping.MappingProgressListener;
import io.improt.vai.mapping.WorkspaceMapper; // For ClassMapping
import io.improt.vai.util.UICommons;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MappingProgressDialog extends JDialog implements MappingProgressListener {

    private final AnimationPanel animationPanel;
    private final JList<String> fileList;
    private final DefaultListModel<String> fileListModel;
    private final JLabel statusLabel;

    private final Map<String, Integer> filePathToIndexMap; // Maps absolute path to list index
    private final File workspaceRoot;
    private final AtomicInteger totalFilesToProcess;
    private final AtomicInteger filesProcessedCount;

    public MappingProgressDialog(Window owner, List<WorkspaceMapper.ClassMapping> filesToMap, File workspaceRoot) {
        super(owner, "Mapping Workspace...", ModalityType.APPLICATION_MODAL);
        this.workspaceRoot = workspaceRoot;
        this.totalFilesToProcess = new AtomicInteger(filesToMap.size());
        this.filesProcessedCount = new AtomicInteger(0);
        this.filePathToIndexMap = new HashMap<>();

        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(10,10,10,10));
        setBackground(Color.WHITE);


        animationPanel = new AnimationPanel();
        add(animationPanel, BorderLayout.NORTH);

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(480, 150));
        add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Initializing...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        add(statusLabel, BorderLayout.SOUTH);

        initializeFiles(filesToMap);

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Allow closing via 'X' button
        setSize(520, 400);
        setLocationRelativeTo(owner);
        // UICommons.applyRoundedCorners(this, 20, 20); // If undecorated
    }

    private void initializeFiles(List<WorkspaceMapper.ClassMapping> filesToMap) {
        for (int i = 0; i < filesToMap.size(); i++) {
            WorkspaceMapper.ClassMapping cm = filesToMap.get(i);
            String relativePath = getRelativePath(cm.getPath());
            fileListModel.addElement(relativePath + " - Queued");
            filePathToIndexMap.put(cm.getPath(), i);
        }
        updateStatusLabel();
    }

    private String getRelativePath(String absolutePath) {
        if (workspaceRoot != null && absolutePath.startsWith(workspaceRoot.getAbsolutePath())) {
            String rel = workspaceRoot.toURI().relativize(new File(absolutePath).toURI()).getPath();
            // Handle potential leading slash if workspaceRoot is filesystem root
            return rel.startsWith("/") && workspaceRoot.getParent() == null ? rel.substring(1) : rel;
        }
        return new File(absolutePath).getName(); // Fallback to just filename
    }

    private void updateListItem(String absolutePath, String statusMessage, Color color) {
        SwingUtilities.invokeLater(() -> {
            Integer index = filePathToIndexMap.get(absolutePath);
            if (index != null && index < fileListModel.getSize()) {
                String relativePath = getRelativePath(absolutePath);
                fileListModel.setElementAt(relativePath + " - " + statusMessage, index);
                // Note: JList cell renderer would be needed for specific text colors.
                // For simplicity, we're not implementing that here, but status string indicates state.
            }
            updateStatusLabel();
        });
    }

    private void updateStatusLabel() {
         statusLabel.setText(String.format("Processed %d of %d files.", filesProcessedCount.get(), totalFilesToProcess.get()));
    }


    @Override
    public void fileMappingStarted(String filePath) {
        SwingUtilities.invokeLater(() -> {
            updateListItem(filePath, "Mapping...", Color.BLUE);
            animationPanel.startProcessingFile(filePath);
        });
    }

    @Override
    public void fileMappingCompleted(String filePath, boolean success, String message) {
         SwingUtilities.invokeLater(() -> {
            filesProcessedCount.incrementAndGet();
            if (success) {
                updateListItem(filePath, "Completed", Color.GREEN.darker());
            } else {
                updateListItem(filePath, "Error: " + message, Color.RED);
            }
            animationPanel.finishProcessingFile(filePath, success);
            checkAllFilesProcessed();
         });
    }
    
    @Override
    public void allFilesProcessed() {
        // This method is called by WorkspaceMapper after all workers for a batch are done.
        // The dialog will close itself once all animations are also complete,
        // or if this indicates all tasks submitted, and animations will catch up.
        // For now, we'll rely on individual file completions to trigger close.
        // This callback can be used for a final "All Done!" message if desired before auto-close.
        SwingUtilities.invokeLater(()-> {
             if (filesProcessedCount.get() >= totalFilesToProcess.get()) {
                statusLabel.setText("All files processed. Closing...");
                // Give a little time for the last animation to be seen
                Timer closeTimer = new Timer(2000, e -> dispose());
                closeTimer.setRepeats(false);
                closeTimer.start();
            }
        });
    }


    private void checkAllFilesProcessed() {
        // This is called after each fileMappingCompleted.
        // The allFilesProcessed from the listener can act as a final confirmation.
        if (filesProcessedCount.get() >= totalFilesToProcess.get()) {
            // The actual closing will be triggered by allFilesProcessed or after last animation.
            // For now, we ensure the status is updated.
            statusLabel.setText(String.format("All %d files processed. Finishing animations...", totalFilesToProcess.get()));
        }
    }
}