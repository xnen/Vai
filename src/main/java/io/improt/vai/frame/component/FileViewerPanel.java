package io.improt.vai.frame.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

import io.improt.vai.backend.App;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class FileViewerPanel extends JPanel {
    private final RSyntaxTextArea textArea;
    private final JLabel imageLabel; // For image display
    private final JScrollPane imageScrollPane; // Scroll pane for image
    private final JButton addButton;
    private final JButton subtractButton;
    private final JButton saveButton;
    private final JButton newFileButton; // New button for creating files
    private final JTextField filenameField; // New textfield for displaying and renaming file name
    private File currentFile;
    private JPanel contentPanel; // To hold either textArea or imageLabel
    private CardLayout cardLayout; // To switch between text and image views

    public FileViewerPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("File Viewer"));

        // Initialize filenameField
        filenameField = new JTextField();
        filenameField.setEditable(false); // Initially non-editable
        filenameField.setPreferredSize(new Dimension(200, 25)); // Set preferred height
        filenameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (currentFile != null) {
                    String newName = filenameField.getText().trim();
                    if (!newName.isEmpty() && !newName.equals(currentFile.getName())) {
                        renameFile(newName);
                    }
                }
            }
        });

        // Panel for filename label and textfield
        JPanel filenamePanel = new JPanel(new BorderLayout());
        filenamePanel.add(filenameField, BorderLayout.CENTER);

        add(filenamePanel, BorderLayout.NORTH); // Add filenamePanel to the top

        // Initialize content panel with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Initialize text area
        textArea = new RSyntaxTextArea();
        textArea.setEditable(true); // Make editable
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        textArea.setCodeFoldingEnabled(true);
        RTextScrollPane textScrollPane = new RTextScrollPane(textArea);
        contentPanel.add(textScrollPane, "TEXT"); // Add text area to content panel

        // Initialize image label
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageScrollPane = new JScrollPane(imageLabel);
        contentPanel.add(imageScrollPane, "IMAGE"); // Add image label to content panel

        add(contentPanel, BorderLayout.CENTER); // Add content panel to FileViewerPanel

        // Initialize buttons
        addButton = new JButton();
        subtractButton = new JButton();
        saveButton = new JButton();
        newFileButton = new JButton("New File"); // Initialize newFileButton

        // Set icons for buttons
        try {
            addButton.setIcon(new ImageIcon("images/add.png"));
            subtractButton.setIcon(new ImageIcon("images/sub.png"));
            saveButton.setIcon(new ImageIcon("images/save.png"));
            // Optionally set an icon for newFileButton if available
            // newFileButton.setIcon(new ImageIcon("images/newfile.png"));
        } catch (Exception e) {
            // If icons not found, set text
            addButton.setText("Add");
            subtractButton.setText("Subtract");
            saveButton.setText("Save");
            newFileButton.setText("New File");
        }

        // Add action listeners
        addButton.addActionListener(e -> addCurrentFile());
        subtractButton.addActionListener(e -> subtractCurrentFile());
        saveButton.addActionListener(e -> saveCurrentFile());
        newFileButton.addActionListener(e -> createNewFile()); // Action for newFileButton

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        buttonPanel.add(subtractButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(newFileButton); // Add newFileButton to the panel
        filenamePanel.add(buttonPanel, BorderLayout.EAST); // Add buttonPanel to the right side of filenamePanel

//        add(buttonPanel, BorderLayout.SOUTH); // Add buttonPanel to the bottom

        // Setup CTRL+S shortcut for saving
        setupSaveShortcut();

        // Initially disable buttons
        disableButtons();

        // Start the watchdog thread
        watchdogThread();

         // Enable paste functionality in the text area (already handled in ClientFrame, no need here)
        // textArea.setTransferHandler(new TextTransferHandler()); // Removed, handled in ClientFrame now
    }

    public void displayFile(File file) {
        if (file == null) {
            clear();
            return;
        }
        try {
            currentFile = file;
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                displayImageFile(file);
            } else {
                displayTextFile(file);
            }
            if (file != null) {
                filenameField.setText(file.getName());
                filenameField.setEditable(true);
            } else {
                filenameField.setText("");
                filenameField.setEditable(false);
            }
            updateButtonStates();
        } catch (Exception e) {
            textArea.setText("Error loading file: " + e.getMessage());
            disableButtons();
        }
    }

    private void displayImageFile(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image != null) {
                imageLabel.setIcon(new ImageIcon(image));
                cardLayout.show(contentPanel, "IMAGE");
            } else {
                displayTextFile(file); // Fallback to text view if image cannot be read
            }
        } catch (IOException e) {
            displayTextFile(file); // Fallback to text view on IO error
        }
    }


    private void displayTextFile(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            textArea.setText(content);
            textArea.setCaretPosition(0);
            setSyntaxEditingStyle(file);
            cardLayout.show(contentPanel, "TEXT");
        } catch (IOException e) {
            textArea.setText("Error loading file as text: " + e.getMessage());
        }
    }


    public void clear() {
        currentFile = null;
        textArea.setText("");
        imageLabel.setIcon(null); // Clear image
        cardLayout.show(contentPanel, "TEXT"); // Default to text view when clearing
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        disableButtons();
    }

    private void renameFile(String newName) {
        File parentDir = currentFile.getParentFile();
        File newFile = new File(parentDir, newName);
        if (newFile.exists()) {
            JOptionPane.showMessageDialog(this, "A file with the name '" + newName + "' already exists.", "Rename Error", JOptionPane.ERROR_MESSAGE);
            filenameField.setText(currentFile.getName()); // Revert to original name
            return;
        }
        try {
            Path sourcePath = currentFile.toPath();
            Path targetPath = newFile.toPath();
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            currentFile = newFile;
            textArea.setText("");
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            App.getInstance().getActiveFileManager().removeFile(sourcePath.toFile());
            App.getInstance().getActiveFileManager().addFile(newFile);
            filenameField.setText(newName);
            JOptionPane.showMessageDialog(this, "File renamed successfully to '" + newName + "'.", "Rename Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to rename the file: " + e.getMessage(), "Rename Error", JOptionPane.ERROR_MESSAGE);
            filenameField.setText(currentFile.getName()); // Revert to original name
        }
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
        } else if (fileName.endsWith(".cpp") || fileName.endsWith(".cxx") || fileName.endsWith(".cc")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
        } else if (fileName.endsWith(".cs")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSHARP);
        } else if (fileName.endsWith(".json")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        } else if (fileName.endsWith(".go")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GO);
        } else if (fileName.endsWith(".groovy")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
        } else if (fileName.endsWith(".rb")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY);
        } else if (fileName.endsWith(".php")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PHP);
        } else if (fileName.endsWith(".kt")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_KOTLIN);
        } else if (fileName.endsWith(".xml")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        } else if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
        } else if (fileName.endsWith(".sql")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
        } else if (fileName.endsWith(".ts")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
        } else if (fileName.endsWith(".bat")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
        } else if (fileName.endsWith(".sh") || fileName.endsWith(".bash")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
        } else {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        }
    }

    private void createNewFile() {
        // Determine the directory to create the new file in
        File targetDir;
        if (currentFile != null) {
            if (currentFile.isDirectory()) {
                targetDir = currentFile;
            } else {
                targetDir = currentFile.getParentFile();
            }
        } else {
            // If no file is currently open, use workspace root
            targetDir = App.getInstance().getCurrentWorkspace();
        }

        if (targetDir == null || !targetDir.exists() || !targetDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Cannot determine the directory to create a new file.", "New File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String newFileName = JOptionPane.showInputDialog(this, "Enter new file name:", "Create New File", JOptionPane.PLAIN_MESSAGE);
        if (newFileName == null || newFileName.trim().isEmpty()) {
            // User cancelled or entered empty name
            return;
        }

        File newFile = new File(targetDir, newFileName.trim());
        if (newFile.exists()) {
            JOptionPane.showMessageDialog(this, "A file with the name '" + newFileName + "' already exists.", "New File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            boolean created = newFile.createNewFile();
            if (created) {
                JOptionPane.showMessageDialog(this, "New file '" + newFileName + "' created successfully.", "New File", JOptionPane.INFORMATION_MESSAGE);
                // Refresh the project tree
                App.getInstance().getClient().getProjectPanel().refreshTree(App.getInstance().getCurrentWorkspace());
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create the new file.", "New File Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error creating new file: " + e.getMessage(), "New File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addCurrentFile() {
        if (currentFile != null && !App.getInstance().getEnabledFiles().contains(currentFile)) {
            App.getInstance().getActiveFileManager().addFile(currentFile);
            updateButtonStates();
        }
    }

    private void subtractCurrentFile() {
        if (currentFile != null && App.getInstance().getEnabledFiles().contains(currentFile)) {
            App.getInstance().getActiveFileManager().removeFile(currentFile.getName());
            updateButtonStates();
        }
    }

    private void saveCurrentFile() {
        if (currentFile != null) {
            if (!isImageFile(currentFile)) { // Only save if not an image file
                try {
                    String newContent = textArea.getText();
                    Files.write(currentFile.toPath(), newContent.getBytes());
                    JOptionPane.showMessageDialog(this, "File saved successfully.", "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Save is not applicable for this file type.", "Save Error", JOptionPane.ERROR_MESSAGE);
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
            saveButton.setEnabled(!isImageFile(currentFile)); // Disable save button for image files for now
            newFileButton.setEnabled(true);
            filenameField.setEditable(true);
        } else {
            disableButtons();
        }
    }

    private void disableButtons() {
        addButton.setEnabled(false);
        subtractButton.setEnabled(false);
        saveButton.setEnabled(false);
        newFileButton.setEnabled(false);
        filenameField.setEditable(false);
    }

    private boolean isImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
    }
}
