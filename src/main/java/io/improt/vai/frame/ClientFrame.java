package io.improt.vai.frame;

import io.improt.vai.backend.App;
import io.improt.vai.frame.actions.NewProjectAction;
import io.improt.vai.frame.actions.OpenPathAction;
import io.improt.vai.frame.actions.TempProjectAction;
import io.improt.vai.frame.component.FileViewerPanel;
import io.improt.vai.frame.component.ProjectPanel;
import io.improt.vai.frame.component.ActiveFilesPanel;
import io.improt.vai.frame.component.RecentActiveFilesPanel;
import io.improt.vai.util.FileUtils;
import io.improt.vai.util.VaiUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientFrame extends JFrame implements ActiveFilesPanel.FileSelectionListener {

    private final JComboBox<String> modelCombo;
    private final JTextArea textArea;
    private FileViewerPanel fileViewerPanel = null;
    private ProjectPanel projectPanel;
    private App backend;
    private final JMenu recentMenu;
    private final JMenu recentActiveFilesMenu;

    private final RecentActiveFilesPanel recentActiveFilesPanel; // Added as a class field

    // Added Execute button and currentFile variable
    private final JButton executeButton;
    private File currentFile;

    public static boolean pasting = false;

    public ClientFrame() {
        super("Vai");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setResizable(true);

        // Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        recentMenu = new JMenu("Recent");

        recentActiveFilesMenu = new JMenu("Recent");
        populateRecentMenu();
        populateMenu();
        JMenu configMenu = new JMenu("Config");

        JMenuItem newProjectItem = new JMenuItem("New Project...");
        JMenuItem openDirItem = new JMenuItem("Open Directory...");
        JMenuItem openPathItem = new JMenuItem("Open Path...");
        JMenuItem tempProjectItem = new JMenuItem("Temp Project");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        JMenuItem exitItem = new JMenuItem("Exit");
        JMenuItem configureItem = new JMenuItem("Configure...");

        // Action Listener for New Project...
        newProjectItem.addActionListener(new NewProjectAction(this));
        tempProjectItem.addActionListener(new TempProjectAction(this));

        openDirItem.addActionListener(e -> {
            backend.showOpenWorkspaceDialog(this);
            projectPanel.refreshTree(backend.getCurrentWorkspace());
            populateRecentMenu();
            updateTitle();
        });

        openPathItem.addActionListener(new OpenPathAction(this));

        exitItem.addActionListener(e -> System.exit(0));
        configureItem.addActionListener(e -> new ConfigureFrame(this));
        refreshItem.addActionListener(e -> projectPanel.refreshTree(backend.getCurrentWorkspace()));

        // Adding menu items to File menu
        fileMenu.add(newProjectItem);
        fileMenu.add(tempProjectItem);
        fileMenu.add(openDirItem);
        fileMenu.add(openPathItem);
        fileMenu.add(recentMenu);
        fileMenu.add(refreshItem);
        fileMenu.addSeparator(); // Adds a separator line

        // Initialize and add "Open Project Directory" menu item
        // Added "Open Project Directory" menu item
        JMenuItem openProjectDirItem = new JMenuItem("Open Project Directory");
        openProjectDirItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openProjectDirectory();
            }
        });
        fileMenu.add(openProjectDirItem);

        fileMenu.addSeparator(); // Adds another separator line
        fileMenu.add(exitItem);

        // Adding menu items to Config menu
        configMenu.add(configureItem);

        // Adding menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(configMenu);
        menuBar.add(recentActiveFilesMenu);
        setJMenuBar(menuBar);

        // Project Panel
        projectPanel = new ProjectPanel();

        // Initialize backend before panels that depend on it
        backend = new App(this);
        backend.init();

        projectPanel.init(backend);

        // Active Files Panel
        ActiveFilesPanel activeFilesPanel = new ActiveFilesPanel(backend);
        activeFilesPanel.setFileSelectionListener(this);

        // Recent Active Files Panel
        recentActiveFilesPanel = new RecentActiveFilesPanel(); // Assigned to class field

        // File Viewer Panel
        fileViewerPanel = new FileViewerPanel();

        // Right panel contains fileViewerPanel and other components
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());

        // Add fileViewerPanel at the center
        rightPanel.add(fileViewerPanel, BorderLayout.CENTER);

        // Panel for textArea and buttons
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        // Text area
        textArea = new JTextArea(5, 40); // Set initial rows and columns for better visibility
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Inter", Font.PLAIN, 14));
        JScrollPane textScrollPane = new JScrollPane(textArea);
        textScrollPane.setPreferredSize(new Dimension(100, 150)); // Set preferred size to make it taller
        inputPanel.add(textScrollPane, BorderLayout.CENTER);

        // Add Paste Listener to textArea
        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    // Show default paste menu, and then handle potentially pasted image
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem pasteMenuItem = new JMenuItem("Paste");
                    pasteMenuItem.addActionListener(pasteAction -> handlePaste());
                    popup.add(pasteMenuItem);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        textArea.getInputMap().put(KeyStroke.getKeyStroke("control V"), "paste");
        textArea.getActionMap().put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!pasting) {
                    handlePaste();
                }
            }
        });


        // Panel for buttons and modelCombo
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        // Model combo
        modelCombo = new JComboBox<>(new String[]{"gemini-2.0-flash-thinking-exp-01-21", "o1-mini", "o1-preview"}); // Added Gemini model
        bottomPanel.add(modelCombo);

        // Clear button
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> textArea.setText(""));
        bottomPanel.add(clearButton);

        // Execute button
        executeButton = new JButton("Execute");
        executeButton.setEnabled(false); // Initially disabled
        executeButton.addActionListener(e -> {
            if (currentFile != null && VaiUtils.isExecutable(currentFile)) {
                int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to execute " + currentFile.getName() + "?", "Execute Confirmation", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_OPTION) {
                    backend.executeFile(currentFile);
                }
            }
        });
        bottomPanel.add(executeButton);

        // Submit button
        JButton submitButton = createSubmitButton();
        bottomPanel.add(submitButton);

        inputPanel.add(bottomPanel, BorderLayout.SOUTH);
        rightPanel.add(inputPanel, BorderLayout.SOUTH);

        // Create vertical splits for left side
        JSplitPane mainSplitPane = createLeftPanel(activeFilesPanel, rightPanel);

        // Add the split pane to the frame
        getContentPane().add(mainSplitPane, BorderLayout.CENTER);

        // Status bar at the bottom
        JLabel statusLabel = new JLabel("Ready");
        getContentPane().add(statusLabel, BorderLayout.SOUTH);

        // Add listener to ProjectPanel for file selection
        projectPanel.getTree().addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            File selectedFile = projectPanel.pathToFile(path);
            if (selectedFile != null && selectedFile.isFile()) {
                fileViewerPanel.displayFile(selectedFile);
                this.currentFile = selectedFile;
                updateExecuteButton();
            } else {
                fileViewerPanel.clear();
                this.currentFile = null;
                updateExecuteButton();
            }
        });

        // Add CTRL+ENTER shortcut for Submit button
        addSubmitShortcut(submitButton);

        // Initial title update
        updateTitle();

        setVisible(true);
    }

    private void handlePaste() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                Image image = (Image) contents.getTransferData(DataFlavor.imageFlavor);
                if (image != null) {
                    // Save image as temporary file and add to active files
                    File tempImageFile = backend.saveImageAsTempFile(image);
                    if (tempImageFile != null) {
                        backend.getActiveFileManager().addFile(tempImageFile);
                        fileViewerPanel.displayFile(tempImageFile); // Immediately display the file
                        projectPanel.refreshTree(backend.getCurrentWorkspace()); // Refresh file viewer to show new file instantly
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to save image from clipboard.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error pasting image: " + ex.getMessage(), "Paste Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // Handle text paste as default
            pasting = true;
            try {
                String text = (String) clipboard.getData(DataFlavor.stringFlavor);
                // Get selected text
                String selectedText = textArea.getSelectedText();
                if (selectedText != null) {
                    textArea.setText(textArea.getText().replace(selectedText, text)); // replace selected text with pasted text
                } else {
                    // get cursor position
                    int caretPosition = textArea.getCaretPosition();
                    // insert text
                    textArea.replaceRange(text, caretPosition, caretPosition);
                }
            } catch (UnsupportedFlavorException | IOException ex) {
//                textArea.paste(); // Fallback to default paste if string flavor fails
                // tough luck.
                JOptionPane.showMessageDialog(this, "Failed to paste: " + ex.getMessage(), "Paste Error", JOptionPane.ERROR_MESSAGE);
            }
            pasting = false;
        }
    }


    @NotNull
    private JButton createSubmitButton() {
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            if (modelCombo.getSelectedItem() == null) {
                System.out.println("Must select a model");
                return;
            }

            String prompt = textArea.getText();
//            prompt += " Continue prompting as needed to continue writing this program -- analyze any missing or incorrect pieces and implement as you go.";

            App.getInstance().getLLM().submitRequest(modelCombo.getSelectedItem().toString(), prompt);
        });
        return submitButton;
    }

    @NotNull
    private JSplitPane createLeftPanel(ActiveFilesPanel activeFilesPanel, JPanel rightPanel) {
        JSplitPane verticalSplitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, activeFilesPanel, projectPanel);
        verticalSplitPane1.setDividerLocation(180);
        verticalSplitPane1.setResizeWeight(0);
        verticalSplitPane1.setOneTouchExpandable(true);

        JSplitPane verticalSplitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, verticalSplitPane1, recentActiveFilesPanel);
        verticalSplitPane2.setDividerLocation(180 + 276);
        verticalSplitPane2.setResizeWeight(0);
        verticalSplitPane2.setOneTouchExpandable(true);

        // Create main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, verticalSplitPane2, rightPanel);
        mainSplitPane.setDividerLocation(400); // Adjust as needed

        return mainSplitPane;
    }

    /**
     * Callback method when a file is selected in the ActiveFilesPanel.
     *
     * @param file The file that was selected.
     */
    @Override
    public void onFileSelected(File file) {
        fileViewerPanel.displayFile(file);
        this.currentFile = file;
        updateExecuteButton();
    }

    public ProjectPanel getProjectPanel() {
        return this.projectPanel;
    }

    // New method to populate the Recent menu
    public void populateRecentMenu() {
        recentMenu.removeAll();

        List<String> recentProjects = FileUtils.loadRecentProjects();

        if (recentProjects.isEmpty()) {
            JMenuItem emptyItem = new JMenuItem("No Recent Projects");
            emptyItem.setEnabled(false);
            recentMenu.add(emptyItem);
            return;
        }

        for (String path : recentProjects) {
            JMenuItem projectItem = new JMenuItem(formatProjectName(path));
            projectItem.setToolTipText(path);
            projectItem.addActionListener(e -> {
                File workspace = new File(path);
                if (workspace.exists() && workspace.isDirectory()) {
                    backend.openWorkspace(workspace);
                    projectPanel.refreshTree(backend.getCurrentWorkspace());
                    populateRecentMenu();
                    updateTitle();
                } else {
                    JOptionPane.showMessageDialog(this, "The project directory does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                    List<String> updatedRecentProjects = new ArrayList<>(FileUtils.loadRecentProjects());
                    updatedRecentProjects.remove(path);
                    FileUtils.saveRecentProjects(updatedRecentProjects);
                    populateRecentMenu();
                }
            });
            recentMenu.add(projectItem);
        }
    }

    private void populateMenu() {
        // Add a separator and "Clear Recent Files" menu item
        JMenuItem clearRecentFilesItem = new JMenuItem("Clear Recent Files");
        JMenuItem hack = new JMenuItem("Hack");
        clearRecentFilesItem.addActionListener(e -> {
            int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear all recent files?", "Confirm Clear", JOptionPane.YES_NO_OPTION);
            if (confirmation == JOptionPane.YES_OPTION) {
                File currentWorkspace = backend.getCurrentWorkspace();
                FileUtils.saveRecentlyActiveFiles(new ArrayList<>(), currentWorkspace);
                recentActiveFilesPanel.refresh();
            }
        });

        // Giga hack to allow manual testing of a given response.
        hack.addActionListener(e -> {
            RepairFrame repairDialog = new RepairFrame(this, "Hello world", "Exception");
            repairDialog.setVisible(true);
            backend.getLLM().handleCodeResponse(repairDialog.getCorrectedCode());
        });
        recentActiveFilesMenu.add(clearRecentFilesItem);
        recentActiveFilesMenu.add(hack);
    }

    // Helper method to format the project name using the last two directories
    private String formatProjectName(String path) {
        File file = new File(path);
        StringBuilder nameBuilder = new StringBuilder("...");
        File parent = file.getParentFile();
        if (parent != null) {
            File grandParent = parent.getParentFile();
            if (grandParent != null) {
                nameBuilder.append(grandParent.getName()).append("/");
            }
            nameBuilder.append(file.getName());
        } else {
            nameBuilder.append(file.getName());
        }
        return nameBuilder.toString();
    }

    public RecentActiveFilesPanel getRecentActiveFilesPanel() {
        return recentActiveFilesPanel;
    }

    private void updateExecuteButton() {
        executeButton.setEnabled(currentFile != null && VaiUtils.isExecutable(currentFile));
    }

    public void setLLMPrompt(String prompt) {
        textArea.setText(prompt);
        textArea.setBackground(new Color(144, 238, 144)); // Light green background

        // Add a DocumentListener to reset the background when the user modifies the text
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            private boolean isReset = false;

            @Override
            public void insertUpdate(DocumentEvent e) {
                resetBackground();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                resetBackground();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                resetBackground();
            }

            private void resetBackground() {
                if (!isReset) {
                    textArea.setBackground(Color.WHITE);
                    isReset = true;
                    textArea.getDocument().removeDocumentListener(this);
                }
            }
        });
    }

    /**
     * Updates the JFrame title to the current project path.
     * If no project is open, sets a default title.
     */
    private void updateTitle() {
        File currentWorkspace = backend.getCurrentWorkspace();
        if (currentWorkspace != null && currentWorkspace.exists()) {
            setTitle("Vai - " + currentWorkspace.getAbsolutePath());
        } else {
            setTitle("Vai");
        }
    }

    /**
     * Opens the current project directory in the system file explorer.
     */
    private void openProjectDirectory() {
        File currentWorkspace = backend.getCurrentWorkspace();
        if (currentWorkspace != null && currentWorkspace.exists()) {
            try {
                Desktop.getDesktop().open(currentWorkspace);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Unable to open project directory.", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(this, "No project directory is currently open.", "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Adds a CTRL+ENTER keyboard shortcut to trigger the Submit button.
     *
     * @param submitButton The Submit button to be triggered.
     */
    private void addSubmitShortcut(JButton submitButton) {
        // Define the key stroke for CTRL+ENTER
        KeyStroke ctrlEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);

        // Get the root pane's input map and action map
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlEnter, "submitAction");
        getRootPane().getActionMap().put("submitAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitButton.doClick();
            }
        });
    }
}
