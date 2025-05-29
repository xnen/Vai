package io.improt.vai.frame.dialogs;

import io.improt.vai.backend.App;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MissingFilesDialog extends JDialog {
    private final DefaultListModel<String> listModel;
    private final JList<String> missingFilesList;
    private final JButton okButton;

    public MissingFilesDialog(Frame owner, List<String> missingFileEntries) {
        super(owner, "Missing Files", true); // Modal dialog

        listModel = new DefaultListModel<>();
        for (String entry : missingFileEntries) {
            listModel.addElement(entry);
        }
        missingFilesList = new JList<>(listModel);
        missingFilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Not strictly necessary for display only

        okButton = new JButton("OK");
        okButton.addActionListener(e -> dispose()); // Close the dialog

        setupUI();
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top label: Instructions or title
        JLabel titleLabel = new JLabel("The following files suggested by the plan were not found:");
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Center: List of missing files in a scroll pane
        JScrollPane listScrollPane = new JScrollPane(missingFilesList);
        listScrollPane.setPreferredSize(new Dimension(450, 200)); // Adjust size as needed
        mainPanel.add(listScrollPane, BorderLayout.CENTER);

        // South: OK button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Center the OK button
        buttonPanel.add(okButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(getOwner()); // Center relative to the parent frame
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Ensure dialog closes properly
    }

    /**
     * Static method to create and show the dialog.
     *
     * @param owner              The parent frame.
     * @param missingFileEntries A list of strings, each formatted as "File Name | Full Path".
     */
    public static void showDialog(Frame owner, List<String> missingFileEntries) {
        if (missingFileEntries == null || missingFileEntries.isEmpty()) {
            return; // Don't show dialog if there's nothing to show
        }
        MissingFilesDialog dialog = new MissingFilesDialog(owner, missingFileEntries);
        dialog.setVisible(true);
    }
}