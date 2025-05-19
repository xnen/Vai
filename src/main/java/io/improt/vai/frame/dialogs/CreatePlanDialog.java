package io.improt.vai.frame.dialogs;

import io.improt.vai.backend.App;
import io.improt.vai.mapping.SubWorkspace;
import io.improt.vai.util.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CreatePlanDialog extends JDialog {


    // Record to hold external subworkspace selection details
    public class ExternalSubWorkspaceSelection {

        private File projectFile;
        private SubWorkspace subWorkspace;

        public ExternalSubWorkspaceSelection(File projectFile, SubWorkspace subWorkspace) {
            this.projectFile = projectFile;
            this.subWorkspace = subWorkspace;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExternalSubWorkspaceSelection that = (ExternalSubWorkspaceSelection) o;
            return Objects.equals(projectFile.getAbsolutePath(), that.projectFile.getAbsolutePath()) &&
                   Objects.equals(subWorkspace.getName(), that.subWorkspace.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(projectFile.getAbsolutePath(), subWorkspace.getName());
        }

        public SubWorkspace getSubWorkspace() {
            return subWorkspace;
        }

        public File getProjectFile() {
            return projectFile;
        }
    }

    private JTextArea planTextArea;
    private JButton submitButton;
    private JButton cancelButton;
    private JPanel localSubworkspacePanel; // Renamed for clarity
    private List<JCheckBox> localSubworkspaceCheckBoxes;

    private JPanel externalSubworkspaceDisplayPanel; // Panel to display selected external subworkspaces
    private JButton importExternalButton;
    private List<ExternalSubWorkspaceSelection> selectedExternalSubWorkspaces;
    private Map<JCheckBox, ExternalSubWorkspaceSelection> externalCheckboxMap;


    private String planText = null;
    private boolean submitted = false;
    private List<SubWorkspace> availableLocalSubWorkspaces; // Renamed for clarity

    public CreatePlanDialog(Frame owner, String initialText, List<SubWorkspace> availableLocalSubWorkspaces) {
        super(owner, "Create Plan", true);
        this.availableLocalSubWorkspaces = availableLocalSubWorkspaces != null ? availableLocalSubWorkspaces : new ArrayList<>();
        this.localSubworkspaceCheckBoxes = new ArrayList<>();
        this.selectedExternalSubWorkspaces = new ArrayList<>();
        this.externalCheckboxMap = new HashMap<>();
        initComponents(initialText);
        layoutComponents();
        addListeners();

        setSize(700, 600); // Increased size slightly
        setLocationRelativeTo(owner);
    }

    private void initComponents(String initialText) {
        planTextArea = new JTextArea(initialText);
        planTextArea.setLineWrap(true);
        planTextArea.setWrapStyleWord(true);
        planTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Panel for local sub-workspaces
        localSubworkspacePanel = new JPanel();
        localSubworkspacePanel.setLayout(new BoxLayout(localSubworkspacePanel, BoxLayout.Y_AXIS));
        localSubworkspacePanel.setBorder(BorderFactory.createTitledBorder("Active Local Sub-Workspaces"));
        if (!this.availableLocalSubWorkspaces.isEmpty()) {
            for (SubWorkspace sw : this.availableLocalSubWorkspaces) {
                JCheckBox checkBox = new JCheckBox(sw.getName());
                localSubworkspaceCheckBoxes.add(checkBox);
                localSubworkspacePanel.add(checkBox);
            }
        } else {
            localSubworkspacePanel.add(new JLabel("No local sub-workspaces defined."));
        }

        // Section for external sub-workspaces
        importExternalButton = new JButton("Import External Sub-Workspaces...");
        externalSubworkspaceDisplayPanel = new JPanel();
        externalSubworkspaceDisplayPanel.setLayout(new BoxLayout(externalSubworkspaceDisplayPanel, BoxLayout.Y_AXIS));
        externalSubworkspaceDisplayPanel.setBorder(BorderFactory.createTitledBorder("Imported External Sub-Workspaces"));
        externalSubworkspaceDisplayPanel.add(new JLabel("No external sub-workspaces imported yet.")); // Initial message


        submitButton = new JButton("Submit Plan");
        cancelButton = new JButton("Cancel");
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        JScrollPane planScrollPane = new JScrollPane(planTextArea);

        // Combined panel for all subworkspace selections (local and external)
        JPanel allSubWorkspacesOuterPanel = new JPanel(new BorderLayout(5,5));
        allSubWorkspacesOuterPanel.add(importExternalButton, BorderLayout.NORTH);

        JPanel allSubWorkspacesInnerPanel = new JPanel();
        allSubWorkspacesInnerPanel.setLayout(new BoxLayout(allSubWorkspacesInnerPanel, BoxLayout.Y_AXIS));
        allSubWorkspacesInnerPanel.add(localSubworkspacePanel);
        allSubWorkspacesInnerPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer
        allSubWorkspacesInnerPanel.add(externalSubworkspaceDisplayPanel);
        
        allSubWorkspacesOuterPanel.add(new JScrollPane(allSubWorkspacesInnerPanel), BorderLayout.CENTER);


        // Determine if any subworkspaces (local or potential external) justify the split pane
        boolean showSubworkspaceArea = !this.availableLocalSubWorkspaces.isEmpty() || true; // Keep true to always show import button

        if (showSubworkspaceArea) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, planScrollPane, allSubWorkspacesOuterPanel);
            splitPane.setResizeWeight(0.6); // Give more space to plan text initially
            add(splitPane, BorderLayout.CENTER);
        } else {
            // This case should ideally not happen if we always show the import button
            add(planScrollPane, BorderLayout.CENTER);
            // Set a minimum size for the allSubWorkspacesOuterPanel if it's shown at bottom
            allSubWorkspacesOuterPanel.setPreferredSize(new Dimension(0,100));
            add(allSubWorkspacesOuterPanel, BorderLayout.SOUTH);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void addListeners() {
        submitButton.addActionListener(e -> {
            planText = planTextArea.getText();
            submitted = true;
            // Collect selected external subworkspaces
            selectedExternalSubWorkspaces.clear();
            for (Map.Entry<JCheckBox, ExternalSubWorkspaceSelection> entry : externalCheckboxMap.entrySet()) {
                if (entry.getKey().isSelected()) {
                    selectedExternalSubWorkspaces.add(entry.getValue());
                }
            }
            setVisible(false);
            dispose();
        });

        cancelButton.addActionListener(e -> {
            submitted = false;
            setVisible(false);
            dispose();
        });

        importExternalButton.addActionListener(e -> handleImportExternalSubWorkspaces());
    }

    private void handleImportExternalSubWorkspaces() {
        // 1. Select External Project
        Map<String, String> pathToUuid = FileUtils.getWorkspacePathToUuidMap();
        if (pathToUuid.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No other workspaces found in VAI configuration.", "No External Workspaces", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String currentWorkspacePath = App.getInstance().getCurrentWorkspace() != null ? App.getInstance().getCurrentWorkspace().getAbsolutePath() : null;

        List<String> externalProjectPaths = pathToUuid.keySet().stream()
                .filter(path -> !path.equals(currentWorkspacePath))
                .collect(Collectors.toList());

        if (externalProjectPaths.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No *other* workspaces found to import from.", "No External Workspaces", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] projectPathArray = externalProjectPaths.toArray(new String[0]);
        String selectedProjectPath = (String) JOptionPane.showInputDialog(
                this,
                "Select an external project:",
                "Import from Project",
                JOptionPane.PLAIN_MESSAGE,
                null,
                projectPathArray,
                projectPathArray[0]);

        if (selectedProjectPath == null || selectedProjectPath.trim().isEmpty()) {
            return; // User cancelled
        }

        File selectedProjectFile = new File(selectedProjectPath);

        // 2. Select SubWorkspaces from that Project
        List<SubWorkspace> externalProjectSubWorkspaces = FileUtils.loadSubWorkspaces(selectedProjectFile);
        if (externalProjectSubWorkspaces.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The selected project '" + selectedProjectFile.getName() + "' has no defined sub-workspaces.", "No Sub-Workspaces", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel selectionPanel = new JPanel();
        selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
        List<JCheckBox> checkBoxes = new ArrayList<>();
        for (SubWorkspace sw : externalProjectSubWorkspaces) {
            JCheckBox cb = new JCheckBox(sw.getName());
            checkBoxes.add(cb);
            selectionPanel.add(cb);
        }

        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(selectionPanel), "Select Sub-Workspaces from '" + selectedProjectFile.getName() + "'", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            boolean firstImportForDisplay = externalCheckboxMap.isEmpty();
            if (firstImportForDisplay) {
                 // Remove "No external sub-workspaces imported yet."
                Component[] components = externalSubworkspaceDisplayPanel.getComponents();
                if (components.length > 0 && components[0] instanceof JLabel && ((JLabel)components[0]).getText().startsWith("No external")) {
                    externalSubworkspaceDisplayPanel.remove(0);
                }
            }

            for (int i = 0; i < externalProjectSubWorkspaces.size(); i++) {
                if (checkBoxes.get(i).isSelected()) {
                    SubWorkspace selectedSw = externalProjectSubWorkspaces.get(i);
                    ExternalSubWorkspaceSelection newSelection = new ExternalSubWorkspaceSelection(selectedProjectFile, selectedSw);
                    
                    // Avoid adding duplicates visually and to the tracking map
                    if (selectedExternalSubWorkspaces.contains(newSelection) || externalCheckboxMap.values().stream().anyMatch(val -> val.equals(newSelection)) ) {
                        System.out.println("Skipping already imported/selected external subworkspace: " + newSelection.projectFile.getName() + " - " + newSelection.subWorkspace.getName());
                        continue;
                    }
                    // Add to selectedExternalSubWorkspaces as well, for pre-selection if dialog is re-opened (though current logic rebuilds checkboxes)
                    // this.selectedExternalSubWorkspaces.add(newSelection); 


                    JCheckBox displayCheckBox = new JCheckBox(selectedProjectFile.getName() + ": " + selectedSw.getName(), true); // Select by default
                    externalCheckboxMap.put(displayCheckBox, newSelection);
                    externalSubworkspaceDisplayPanel.add(displayCheckBox);
                }
            }
            externalSubworkspaceDisplayPanel.revalidate();
            externalSubworkspaceDisplayPanel.repaint();
        }
    }

    public String getPlanText() {
        return planText;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public List<String> getSelectedLocalSubworkspaceNames() { // Renamed for clarity
        List<String> selectedNames = new ArrayList<>();
        for (JCheckBox checkBox : localSubworkspaceCheckBoxes) {
            if (checkBox.isSelected()) {
                selectedNames.add(checkBox.getText());
            }
        }
        return selectedNames;
    }

    public List<ExternalSubWorkspaceSelection> getSelectedExternalSubWorkspaces() {
        // This list is populated on submit button click, so it's up-to-date if isSubmitted() is true
        return new ArrayList<>(selectedExternalSubWorkspaces);
    }
}