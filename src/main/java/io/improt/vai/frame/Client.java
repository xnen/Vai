package io.improt.vai.frame;

import io.improt.vai.backend.App;
import io.improt.vai.frame.component.FileViewerPanel;
import io.improt.vai.frame.component.ProjectPanel;
import io.improt.vai.frame.component.ActiveFilesPanel;
import io.improt.vai.frame.component.RecentActiveFilesPanel;
import io.improt.vai.util.FileUtils;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.List;

public class Client extends JFrame implements ActiveFilesPanel.FileSelectionListener {

    private final JComboBox<String> modelCombo;
    private final JTextArea textArea;
    private FileViewerPanel fileViewerPanel = null;
    private ProjectPanel projectPanel;
    private App backend;
    private JMenu recentMenu;

    public Client() {
        super("Vai");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setResizable(true);

        // Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        recentMenu = new JMenu("Recent");
        populateRecentMenu();
        JMenu configMenu = new JMenu("Config");
        JMenuItem openDirItem = new JMenuItem("Open Directory...");
        JMenuItem openPathItem = new JMenuItem("Open Path...");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        JMenuItem exitItem = new JMenuItem("Exit");
        JMenuItem configureItem = new JMenuItem("Configure...");

        openDirItem.addActionListener(e -> {
            backend.openDirectory(this);
            projectPanel.refreshTree(backend.getCurrentWorkspace());
            populateRecentMenu();
        });

        openPathItem.addActionListener(e -> {
            String path = JOptionPane.showInputDialog(this, "Enter workspace path:", "Open Path", JOptionPane.PLAIN_MESSAGE);
            if (path != null && !path.trim().isEmpty()) {
                File workspace = new File(path.trim());
                if (workspace.exists() && workspace.isDirectory()) {
                    backend.openDirectory(workspace);
                    projectPanel.refreshTree(backend.getCurrentWorkspace());
                    populateRecentMenu();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid path. Please enter a valid directory.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        exitItem.addActionListener(e -> System.exit(0));
        configureItem.addActionListener(e -> new Configure(this));
        refreshItem.addActionListener(e -> projectPanel.refreshTree(backend.getCurrentWorkspace()));

        fileMenu.add(openDirItem);
        fileMenu.add(openPathItem);
        fileMenu.add(recentMenu);
        fileMenu.add(refreshItem);
        fileMenu.addSeparator(); // Adds a separator line
        fileMenu.add(exitItem);

        configMenu.add(configureItem);
        menuBar.add(fileMenu);
        menuBar.add(configMenu);
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
        RecentActiveFilesPanel recentActiveFilesPanel = new RecentActiveFilesPanel();

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

        // Panel for buttons and modelCombo
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        // Model combo
        modelCombo = new JComboBox<>(new String[]{"o1-mini", "o1-preview"});
        bottomPanel.add(modelCombo);

        // Clear button
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> textArea.setText(""));
        bottomPanel.add(clearButton);

        // Submit button
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            if (modelCombo.getSelectedItem() == null) {
                System.out.println("Must select a model");
                return;
            }

            App.getInstance().submitRequest(modelCombo.getSelectedItem().toString(), textArea.getText());
        });
        bottomPanel.add(submitButton);

        inputPanel.add(bottomPanel, BorderLayout.SOUTH);

        rightPanel.add(inputPanel, BorderLayout.SOUTH);

        // Create vertical splits for left side
        JSplitPane verticalSplitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, activeFilesPanel, projectPanel);
        verticalSplitPane1.setDividerLocation(180);
        verticalSplitPane1.setResizeWeight(0);
        verticalSplitPane1.setOneTouchExpandable(true);

        JSplitPane verticalSplitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, verticalSplitPane1, recentActiveFilesPanel);
        verticalSplitPane2.setDividerLocation(180+276);
        verticalSplitPane2.setResizeWeight(0);
        verticalSplitPane2.setOneTouchExpandable(true);

        // Create main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, verticalSplitPane2, rightPanel);
        mainSplitPane.setDividerLocation(400); // Adjust as needed

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
            } else {
                fileViewerPanel.clear();
            }
        });

        setVisible(true);
    }

    /**
     * Callback method when a file is selected in the ActiveFilesPanel.
     *
     * @param file The file that was selected.
     */
    @Override
    public void onFileSelected(File file) {
        fileViewerPanel.displayFile(file);
    }

    public ProjectPanel getProjectPanel() {
        return this.projectPanel;
    }

    // New method to populate the Recent menu
    private void populateRecentMenu() {
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
                    backend.openDirectory(workspace);
                    projectPanel.refreshTree(backend.getCurrentWorkspace());
                    populateRecentMenu();
                } else {
                    JOptionPane.showMessageDialog(this, "The project directory does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                    FileUtils.loadRecentProjects().remove(path);
                    FileUtils.saveRecentProjects(FileUtils.loadRecentProjects());
                    populateRecentMenu();
                }
            });
            recentMenu.add(projectItem);
        }
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
}
