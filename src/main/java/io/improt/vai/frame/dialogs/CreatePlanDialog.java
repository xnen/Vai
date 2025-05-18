package io.improt.vai.frame.dialogs;

import io.improt.vai.mapping.SubWorkspace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class CreatePlanDialog extends JDialog {
    private JTextArea planTextArea;
    private JButton submitButton;
    private JButton cancelButton;
    private JPanel subworkspaceSelectionPanel;
    private List<JCheckBox> subworkspaceCheckBoxes;

    private String planText = null;
    private boolean submitted = false;
    private List<SubWorkspace> availableSubWorkspaces;

    public CreatePlanDialog(Frame owner, String initialText, List<SubWorkspace> availableSubWorkspaces) {
        super(owner, "Create Plan", true); 
        this.availableSubWorkspaces = availableSubWorkspaces != null ? availableSubWorkspaces : new ArrayList<>();
        this.subworkspaceCheckBoxes = new ArrayList<>();
        initComponents(initialText);
        layoutComponents();
        addListeners();

        setSize(600, 500); 
        setLocationRelativeTo(owner); 
    }

    private void initComponents(String initialText) {
        planTextArea = new JTextArea(initialText);
        planTextArea.setLineWrap(true);
        planTextArea.setWrapStyleWord(true);
        planTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        subworkspaceSelectionPanel = new JPanel();
        subworkspaceSelectionPanel.setLayout(new BoxLayout(subworkspaceSelectionPanel, BoxLayout.Y_AXIS));
        subworkspaceSelectionPanel.setBorder(BorderFactory.createTitledBorder("Active Sub-Workspaces"));
        if (!this.availableSubWorkspaces.isEmpty()) {
            for (SubWorkspace sw : this.availableSubWorkspaces) {
                JCheckBox checkBox = new JCheckBox(sw.getName());
                subworkspaceCheckBoxes.add(checkBox);
                subworkspaceSelectionPanel.add(checkBox);
            }
        } else {
            subworkspaceSelectionPanel.add(new JLabel("No sub-workspaces defined."));
        }


        submitButton = new JButton("Submit Plan");
        cancelButton = new JButton("Cancel");
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10)); 

        JScrollPane scrollPane = new JScrollPane(planTextArea);

        if (!this.availableSubWorkspaces.isEmpty()) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, new JScrollPane(subworkspaceSelectionPanel));
            splitPane.setResizeWeight(0.7); 
            add(splitPane, BorderLayout.CENTER);
        } else {
            add(scrollPane, BorderLayout.CENTER); 
            // Optionally add the subworkspaceSelectionPanel (which shows "No sub-workspaces defined")
            // add(new JScrollPane(subworkspaceSelectionPanel), BorderLayout.SOUTH); 
            // For now, let's keep it simple: if no subworkspaces, only show text area and buttons.
            // If you want to always show the panel:
            JPanel bottomArea = new JPanel(new BorderLayout());
            bottomArea.add(new JScrollPane(subworkspaceSelectionPanel), BorderLayout.CENTER);
            if (this.availableSubWorkspaces.isEmpty()){ // Add some min height if empty
                 subworkspaceSelectionPanel.setPreferredSize(new Dimension(0, 50));
            }
            add(bottomArea, BorderLayout.SOUTH);


        }


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);
        
        // Decide where to add button panel based on whether subworkspaces are shown in splitpane or south
        if (!this.availableSubWorkspaces.isEmpty()) {
             add(buttonPanel, BorderLayout.SOUTH); // Add buttons below the splitpane
        } else {
            // If subworkspaces are in south, button panel needs to be nested or placed carefully.
            // For simplicity, let's make the main layout a bit different if no subworkspaces.
            // Re-thinking: always have a main content panel and a button panel at SOUTH.
            // The main content panel can be the JSplitPane or just the JScrollPane for planText.

            // Simpler: Let's reorganize. A main panel holds text and optionally subworkspaces.
            // Then button panel at the very bottom.

            // Current: If availableSubWorkspaces is empty, the scrollPane for planText is CENTER.
            // And subworkspaceSelectionPanel (with "No sub-workspaces") is SOUTH.
            // This means buttonPanel should be added to the subworkspaceSelectionPanel's container or further south.

            // New structure for when availableSubWorkspaces is empty:
            // Center: scrollPane (planText)
            // South: a panel containing (subworkspaceSelectionPanel (shows "No sub-workspaces") AND buttonPanel)
            if (this.availableSubWorkspaces.isEmpty()) {
                JPanel southPanelContainer = new JPanel(new BorderLayout());
                southPanelContainer.add(new JScrollPane(subworkspaceSelectionPanel), BorderLayout.NORTH); // "No sub-workspaces" text
                southPanelContainer.add(buttonPanel, BorderLayout.SOUTH); // Buttons below that
                add(southPanelContainer, BorderLayout.SOUTH);
            } else {
                 add(buttonPanel, BorderLayout.SOUTH); // Normal case with split pane
            }
        }


        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void addListeners() {
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                planText = planTextArea.getText();
                submitted = true;
                setVisible(false);
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitted = false;
                setVisible(false);
                dispose();
            }
        });
    }

    public String getPlanText() {
        return planText;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public List<String> getSelectedSubworkspaceNames() {
        List<String> selectedNames = new ArrayList<>();
        for (JCheckBox checkBox : subworkspaceCheckBoxes) {
            if (checkBox.isSelected()) {
                selectedNames.add(checkBox.getText());
            }
        }
        return selectedNames;
    }
}