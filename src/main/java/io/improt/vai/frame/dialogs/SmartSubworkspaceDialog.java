package io.improt.vai.frame.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SmartSubworkspaceDialog extends JDialog {

    private JTextArea userPromptField;
    private JButton submitPromptButton;

    private JList<String> suggestedPathsList;
    private DefaultListModel<String> suggestedPathsListModel;
    private JTextField suggestedNameField;
    private JRadioButton fileBasedRadioButton;
    private JRadioButton directoryBasedRadioButton;
    private JButton createSubworkspaceButton;
    private JButton cancelButton;

    private JPanel initialPanel;
    private JPanel resultsPanel;

    private String userPrompt;
    private List<String> llmSuggestedPaths;
    private String llmSuggestedName;

    private boolean confirmedCreation = false;
    private String finalSubworkspaceName;
    private boolean createAsFileBased;
    private List<String> finalPathsToProcess;


    public SmartSubworkspaceDialog(Frame owner) {
        super(owner, "Smart Sub-Workspace Creation", true);
        initComponents();
        switchToInitialView();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        // Initial Panel Components
        userPromptField = new JTextArea(5, 40);
        userPromptField.setLineWrap(true);
        userPromptField.setWrapStyleWord(true);
        JScrollPane promptScrollPane = new JScrollPane(userPromptField);

        submitPromptButton = new JButton("Get Suggestions from LLM");
        submitPromptButton.addActionListener(e -> {
            userPrompt = userPromptField.getText();
            if (userPrompt != null && !userPrompt.trim().isEmpty()) {
                // This will be handled by the caller, dialog just closes
                setVisible(false);
            } else {
                JOptionPane.showMessageDialog(this, "Please enter a description or request for the sub-workspace.", "Input Required", JOptionPane.WARNING_MESSAGE);
            }
        });

        initialPanel = new JPanel(new BorderLayout(10, 10));
        initialPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        initialPanel.add(new JLabel("Describe the sub-workspace you want to create (e.g., 'Files related to user authentication'):"), BorderLayout.NORTH);
        initialPanel.add(promptScrollPane, BorderLayout.CENTER);
        initialPanel.add(submitPromptButton, BorderLayout.SOUTH);

        // Results Panel Components
        suggestedPathsListModel = new DefaultListModel<>();
        suggestedPathsList = new JList<>(suggestedPathsListModel);
        JScrollPane pathsScrollPane = new JScrollPane(suggestedPathsList);
        pathsScrollPane.setPreferredSize(new Dimension(400, 150));

        suggestedNameField = new JTextField(30);
        fileBasedRadioButton = new JRadioButton("File-based (includes all files from suggested directories)");
        directoryBasedRadioButton = new JRadioButton("Directory-based (monitors suggested directories)");
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(fileBasedRadioButton);
        typeGroup.add(directoryBasedRadioButton);
        fileBasedRadioButton.setSelected(true); // Default

        createSubworkspaceButton = new JButton("Create Sub-Workspace");
        createSubworkspaceButton.addActionListener(e -> {
            finalSubworkspaceName = suggestedNameField.getText();
            if (finalSubworkspaceName == null || finalSubworkspaceName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Sub-workspace name cannot be empty.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            createAsFileBased = fileBasedRadioButton.isSelected();
            finalPathsToProcess = new ArrayList<>();
            for (int i = 0; i < suggestedPathsListModel.getSize(); i++) {
                finalPathsToProcess.add(suggestedPathsListModel.getElementAt(i));
            }
            confirmedCreation = true;
            setVisible(false);
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            confirmedCreation = false;
            setVisible(false);
        });

        resultsPanel = new JPanel(new BorderLayout(10, 10));
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        resultsPanel.add(new JLabel("LLM Suggestions:"), BorderLayout.NORTH);

        JPanel centerResultsPanel = new JPanel();
        centerResultsPanel.setLayout(new BoxLayout(centerResultsPanel, BoxLayout.Y_AXIS));
        centerResultsPanel.add(new JLabel("Suggested relevant files/directories:"));
        centerResultsPanel.add(pathsScrollPane);
        centerResultsPanel.add(Box.createVerticalStrut(10));
        centerResultsPanel.add(new JLabel("Suggested sub-workspace name:"));
        centerResultsPanel.add(suggestedNameField);
        centerResultsPanel.add(Box.createVerticalStrut(10));
        centerResultsPanel.add(new JLabel("Create as:"));
        centerResultsPanel.add(fileBasedRadioButton);
        centerResultsPanel.add(directoryBasedRadioButton);

        resultsPanel.add(centerResultsPanel, BorderLayout.CENTER);

        JPanel resultsButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        resultsButtonPanel.add(cancelButton);
        resultsButtonPanel.add(createSubworkspaceButton);
        resultsPanel.add(resultsButtonPanel, BorderLayout.SOUTH);
    }

    private void switchToInitialView() {
        setContentPane(initialPanel);
        setTitle("Smart Sub-Workspace Creation - Step 1: Describe");
        userPromptField.setText("");
        pack();
    }

    public void switchToResultsView(List<String> paths, String name) {
        this.llmSuggestedPaths = paths;
        this.llmSuggestedName = name;

        suggestedPathsListModel.clear();
        if (paths != null) {
            for (String path : paths) {
                suggestedPathsListModel.addElement(path);
            }
        }
        suggestedNameField.setText(name != null ? name : "");

        boolean hasDirectories = paths != null && paths.stream().anyMatch(p -> p.endsWith("/"));
        directoryBasedRadioButton.setEnabled(hasDirectories);
        if (!hasDirectories && directoryBasedRadioButton.isSelected()) {
            fileBasedRadioButton.setSelected(true);
        }


        setContentPane(resultsPanel);
        setTitle("Smart Sub-Workspace Creation - Step 2: Confirm");
        pack();
        revalidate();
        repaint();
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public boolean isConfirmedCreation() {
        return confirmedCreation;
    }

    public String getFinalSubworkspaceName() {
        return finalSubworkspaceName;
    }

    public boolean shouldCreateAsFileBased() {
        return createAsFileBased;
    }

    public List<String> getFinalPathsToProcess() {
        return finalPathsToProcess;
    }
}