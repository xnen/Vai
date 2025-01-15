package io.improt.vai.backend;

import io.improt.vai.frame.ClientFrame;
import io.improt.vai.openai.LLMInteraction;
import io.improt.vai.openai.OpenAIProvider;
import io.improt.vai.util.FileUtils;
import io.improt.vai.util.Constants;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class App {

    public static final String API_KEY = Constants.API_KEY_PATH;
    private File currentWorkspace;

    private static App instance;
    private ClientFrame mainWindow;
    private OpenAIProvider openAIProvider;

    private LLMInteraction llmInteraction;

    private ActiveFileManager activeFileManager;

    /**
     * Constructs the App with the specified main window.
     *
     * @param mainWindow The main client window.
     */
    public App(ClientFrame mainWindow) {
        this.mainWindow = mainWindow;
        instance = this;
    }

    /**
     * Initializes the application by setting up necessary components and loading the last workspace.
     */
    public void init() {
        FileUtils.loadWorkspaceMappings();

        openAIProvider = new OpenAIProvider();
        openAIProvider.init();

        currentWorkspace = FileUtils.loadLastWorkspace();

        if (currentWorkspace != null) {
            mainWindow.getProjectPanel().refreshTree(currentWorkspace);

            activeFileManager = new ActiveFileManager(currentWorkspace);
            activeFileManager.addEnabledFilesChangeListener(updatedEnabledFiles -> mainWindow.getProjectPanel().refreshTree(currentWorkspace));
        }

        // Initialize LLM interaction
        this.llmInteraction = new LLMInteraction(this);
        llmInteraction.init();
    }

    public LLMInteraction getLLM() {
        return llmInteraction;
    }

    /**
     * Opens a directory chooser dialog and finalizes the directory opening process.
     *
     * @param parent The parent JFrame for the dialog.
     */
    public void showWorkspaceOpenDialog(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            finalizeOpenProject(chooser.getSelectedFile());
        }
    }

    /**
     * Finalizes the process of opening a new directory as the workspace.
     *
     * @param directory The directory to set as the new workspace.
     */
    private void finalizeOpenProject(File directory) {
        this.currentWorkspace = directory;
        FileUtils.saveLastWorkspace(this.currentWorkspace);

        if (this.activeFileManager != null) {
            this.activeFileManager.removeEnabledFilesChangeListener(projectTreeRefresher);
        }

        this.activeFileManager = new ActiveFileManager(this.currentWorkspace);
        this.activeFileManager.addEnabledFilesChangeListener(projectTreeRefresher);

        this.llmInteraction.init();

        FileUtils.addRecentProject(currentWorkspace.getAbsolutePath());

        this.mainWindow.getRecentActiveFilesPanel().refresh();
    }

    public ActiveFileManager getActiveFileManager() {
        return activeFileManager;
    }

    /**
     * Listener to refresh the project tree when enabled files change.
     */
    private final ActiveFileManager.EnabledFilesChangeListener projectTreeRefresher = updatedEnabledFiles -> mainWindow.getProjectPanel().refreshTree(currentWorkspace);

    /**
     * Opens a directory as the current workspace.
     */
    public void openDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            finalizeOpenProject(directory);
        } else {
            JOptionPane.showMessageDialog(mainWindow, "The provided path is not a valid directory.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Retrieves the API key from the specified path.
     */
    public static String getApiKey() {
        return FileUtils.readFileToString(new File(Constants.API_KEY_PATH));
    }

    /**
     * Gets the current workspace directory.
     */
    public File getCurrentWorkspace() {
        return currentWorkspace;
    }

    /**
     * Gets the list of currently enabled files.
     */
    public List<File> getEnabledFiles() {
        if (activeFileManager != null) {
            return activeFileManager.getEnabledFiles();
        }
        return Collections.emptyList();
    }

    /**
     * Retrieves the singleton instance of App.
     */
    public static App getInstance() {
        return instance;
    }

    /**
     * Retrieves the OpenAIProvider instance.
     */
    public OpenAIProvider getOpenAIProvider() {
        return this.openAIProvider;
    }

    public ClientFrame getClient() {
        return this.mainWindow;
    }

    /**
     * Executes the given file by opening it in the system's terminal.
     */
    public void executeFile(File file) {
        if (!file.exists()) {
            JOptionPane.showMessageDialog(mainWindow, "The file does not exist: " + file.getAbsolutePath(), "Execution Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("linux")) {
            JOptionPane.showMessageDialog(mainWindow, "Execute functionality is only supported on Linux.", "Unsupported OS", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Adjust the terminal command as needed for different Linux terminal emulators
            ProcessBuilder pb = new ProcessBuilder("gnome-terminal",
                    "--",
                    "bash",
                    "-c",
                    file.getAbsolutePath() + "; exec bash"
            );
            // Set working directory to the file's parent directory
            pb.directory(file.getParentFile());
            pb.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainWindow, "Failed to execute the file: " + e.getMessage(), "Execution Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
