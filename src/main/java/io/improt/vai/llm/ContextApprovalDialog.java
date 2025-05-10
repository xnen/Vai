package io.improt.vai.llm;

import io.improt.vai.backend.App;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ContextApprovalDialog extends JDialog {
    private final DefaultListModel<String> listModel;
    private final JList<String> fileList;
    private final JButton removeButton;
    private final JButton approveButton;
    private final JButton declineButton;
    private List<String> approvedList = null;
    private boolean approved = false;
    private final JTextArea addlTextArea;

    public ContextApprovalDialog(Frame owner, List<String> contextEntries, String addl) {
        super(owner, "Use this context?", true);

        listModel = new DefaultListModel<>();
        for (String entry : contextEntries) {
            listModel.addElement(entry);
        }
        fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        removeButton = new JButton("-");
        removeButton.setEnabled(false); // Disabled until an entry is selected

        approveButton = new JButton("Approve");
        declineButton = new JButton("Decline");
        
        // Initialize the additional text area with the passed addl string.
        // If addl is null, default to an empty string.
        addlTextArea = new JTextArea(addl != null ? addl : "");
        addlTextArea.setLineWrap(true);
        addlTextArea.setWrapStyleWord(true);
        
        // Enable or disable remove button based on selection
        fileList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                removeButton.setEnabled(!fileList.isSelectionEmpty());
            }
        });

        // Action for removal: remove the selected entry
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = fileList.getSelectedIndex();
                if (selectedIndex != -1) {
                    listModel.remove(selectedIndex);
                }
            }
        });

        // Approve button: collect remaining entries,
        // update additional data via App and close the dialog
        approveButton.addActionListener(e -> {
            approved = true;
            approvedList = new ArrayList<>();
            for (int i = 0; i < listModel.getSize(); i++) {
                approvedList.add(listModel.get(i));
            }
            App.getInstance().getClient().appendLLMPrompt("\n====\n" + addlTextArea.getText());
            dispose();
        });

        // Decline button: cancel the dialog and set approvedList to null
        declineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                approved = false;
                approvedList = null;
                dispose();
            }
        });

        setupUI();
    }

    // Initialize the dialog's UI components, including the new multi-line text field.
    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top label: prompt
        JLabel promptLabel = new JLabel("Use this context?");
        mainPanel.add(promptLabel, BorderLayout.NORTH);

        // Create a center panel with vertical BoxLayout to hold both the file list and the additional text area.
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // File list in a scroll pane
        JScrollPane listScrollPane = new JScrollPane(fileList);
        listScrollPane.setPreferredSize(new Dimension(300, 200));
        centerPanel.add(listScrollPane);

        // Label to indicate additional data field
        JLabel addlLabel = new JLabel("Suggested Instructions for LLM:");
        addlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(addlLabel);

        // Additional text area in its own scroll pane
        JScrollPane addlScrollPane = new JScrollPane(addlTextArea);
        addlScrollPane.setPreferredSize(new Dimension(300, 100));
        addlScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(addlScrollPane);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // East: remove button panel remains unchanged
        JPanel removePanel = new JPanel(new BorderLayout());
        removePanel.add(removeButton, BorderLayout.NORTH);
        mainPanel.add(removePanel, BorderLayout.EAST);

        // South: Approve and Decline buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(declineButton);
        buttonPanel.add(approveButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    // Public static method to show the dialog and return the approved entries list.
    // Returns null if the user declines.
    public static List<String> showDialog(Frame owner, List<String> contextEntries, String addl) {
        ContextApprovalDialog dialog = new ContextApprovalDialog(owner, contextEntries, addl);
        dialog.setVisible(true);
        return dialog.approved ? dialog.approvedList : null;
    }
}
