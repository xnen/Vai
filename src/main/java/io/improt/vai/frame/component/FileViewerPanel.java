package io.improt.vai.frame.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

import io.improt.vai.backend.App;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

public class FileViewerPanel extends JPanel {
    private final RSyntaxTextArea textArea;
    private final JButton addButton;
    private final JButton subtractButton;
    private final JButton saveButton;
    private File currentFile;

    public FileViewerPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("File Viewer"));

        // Initialize text area
        textArea = new RSyntaxTextArea();
        textArea.setEditable(true); // Make editable
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        textArea.setCodeFoldingEnabled(true);

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        // Initialize buttons
        addButton = new JButton();
        subtractButton = new JButton();
        saveButton = new JButton();

        // Set icons for buttons
        try {
            addButton.setIcon(new ImageIcon("images/add.png"));
            subtractButton.setIcon(new ImageIcon("images/sub.png"));
            saveButton.setIcon(new ImageIcon("images/save.png"));
        } catch (Exception e) {
            // If icons not found, set text
            addButton.setText("Add");
            subtractButton.setText("Subtract");
            saveButton.setText("Save");
        }

        // Add action listeners
        addButton.addActionListener(e -> addCurrentFile());
        subtractButton.addActionListener(e -> subtractCurrentFile());
        saveButton.addActionListener(e -> saveCurrentFile());

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        buttonPanel.add(subtractButton);
        buttonPanel.add(saveButton);

        // Add button panel to the top of the FileViewerPanel
        add(buttonPanel, BorderLayout.NORTH);

        // Setup CTRL+S shortcut for saving
        setupSaveShortcut();

        // Initially disable buttons
        disableButtons();

        // Start the watchdog thread
        watchdogThread();
    }

    public void displayFile(File file) {
        try {
            currentFile = file;
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            textArea.setText(content);
            textArea.setCaretPosition(0);
            setSyntaxEditingStyle(file);
            updateButtonStates();
        } catch (IOException e) {
            textArea.setText("Error loading file: " + e.getMessage());
            disableButtons();
        }
    }

    public void clear() {
        currentFile = null;
        textArea.setText("");
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        disableButtons();
    }

    private void setSyntaxEditingStyle(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".java")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        } else if (fileName.endsWith(".py")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        } else if (fileName.endsWith(".js")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        } else if (fileName.endsWith(".html")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
        } else if (fileName.endsWith(".css")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSS);
        } else {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        }
    }

    private void addCurrentFile() {
        if (currentFile != null && !App.getInstance().getEnabledFiles().contains(currentFile)) {
            App.getInstance().toggleFile(currentFile);
            updateButtonStates();
        }
    }

    private void subtractCurrentFile() {
        if (currentFile != null && App.getInstance().getEnabledFiles().contains(currentFile)) {
            App.getInstance().removeFile(currentFile.getName());
            updateButtonStates();
        }
    }

    private void saveCurrentFile() {
        if (currentFile != null) {
            try {
                String newContent = textArea.getText();
                java.nio.file.Files.write(currentFile.toPath(), newContent.getBytes());
                JOptionPane.showMessageDialog(this, "File saved successfully.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void setupSaveShortcut() {
        // Get the input map for the text area
        InputMap im = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = textArea.getActionMap();

        // Define the key stroke for CTRL+S
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);

        // Bind CTRL+S to the save action
        im.put(keyStroke, "saveAction");
        am.put("saveAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCurrentFile();
            }
        });
    }

    private void watchdogThread() {
        Thread watchdogThread = new Thread(() -> {
            while (true) {
                try {
                    updateButtonStates();
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

    private void updateButtonStates() {
        if (currentFile != null) {
            boolean isActive = App.getInstance().getEnabledFiles().contains(currentFile);
            addButton.setEnabled(!isActive);
            subtractButton.setEnabled(isActive);
            saveButton.setEnabled(true);
        } else {
            disableButtons();
        }
    }

    private void disableButtons() {
        addButton.setEnabled(false);
        subtractButton.setEnabled(false);
        saveButton.setEnabled(false);
    }
}