package io.improt.vai.backend;

import io.improt.vai.frame.Client;
import io.improt.vai.frame.CodeRepair;
import io.improt.vai.openai.OpenAIProvider;
import io.improt.vai.util.FileUtils;
import io.improt.vai.util.Constants;
import io.improt.vai.util.CustomParser;
import io.improt.vai.util.CustomParser.FileContent;
import io.improt.vai.util.MeldLauncher;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class App {

    public static final String API_KEY = Constants.API_KEY_PATH;
    private File currentWorkspace;

    private final Set<String> ignoreList = new HashSet<>();

    private static App instance;
    private Client mainWindow;
    private OpenAIProvider openAIProvider;

    private int currentIncrementalBackupNumber = 0;

    private ActiveFileManager activeFileManager;

    /**
     * Constructs the App with the specified main window.
     *
     * @param mainWindow The main client window.
     */
    public App(Client mainWindow) {
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
            FileUtils.createDefaultVaiignore(currentWorkspace);
            ignoreList.addAll(FileUtils.readVaiignore(currentWorkspace));

            mainWindow.getProjectPanel().refreshTree(currentWorkspace);
            currentIncrementalBackupNumber = FileUtils.loadIncrementalBackupNumber(currentWorkspace);

            activeFileManager = new ActiveFileManager(currentWorkspace);
            activeFileManager.addEnabledFilesChangeListener(updatedEnabledFiles -> {
                mainWindow.getProjectPanel().refreshTree(currentWorkspace);
            });
        }
    }

    /**
     * Retrieves the next incremental backup number and updates the stored value.
     *
     * @return The next incremental backup number.
     */
    private int getNextIncrementalBackupNumber() {
        int nextNumber = this.currentIncrementalBackupNumber + 1;
        this.currentIncrementalBackupNumber = nextNumber;
        FileUtils.saveIncrementalBackupNumber(this.currentIncrementalBackupNumber, this.currentWorkspace);
        return nextNumber;
    }

    /**
     * Opens a directory chooser dialog and finalizes the directory opening process.
     *
     * @param parent The parent JFrame for the dialog.
     */
    public void openDirectory(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            finalizeDirectoryOpen(chooser.getSelectedFile());
        }
    }

    /**
     * Finalizes the process of opening a new directory as the workspace.
     *
     * @param directory The directory to set as the new workspace.
     */
    private void finalizeDirectoryOpen(File directory) {
        currentWorkspace = directory;
        FileUtils.saveLastWorkspace(currentWorkspace);
        FileUtils.createDefaultVaiignore(currentWorkspace);
        ignoreList.clear();
        ignoreList.addAll(FileUtils.readVaiignore(currentWorkspace));

        if (activeFileManager != null) {
            activeFileManager.clearActiveFiles();
            activeFileManager.removeEnabledFilesChangeListener(projectTreeRefresher);
        }

        activeFileManager = new ActiveFileManager(currentWorkspace);
        activeFileManager.addEnabledFilesChangeListener(projectTreeRefresher);

        currentIncrementalBackupNumber = FileUtils.loadIncrementalBackupNumber(currentWorkspace);

        FileUtils.addRecentProject(currentWorkspace.getAbsolutePath());
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
     *
     * @param directory The directory to open.
     */
    public void openDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            finalizeDirectoryOpen(directory);
        } else {
            JOptionPane.showMessageDialog(mainWindow, "The provided path is not a valid directory.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Retrieves the API key from the specified path.
     *
     * @return The API key as a String.
     */
    public static String getApiKey() {
        return FileUtils.readFileToString(new File(Constants.API_KEY_PATH));
    }

    /**
     * Gets the current workspace directory.
     *
     * @return The current workspace.
     */
    public File getCurrentWorkspace() {
        return currentWorkspace;
    }

    /**
     * Retrieves the list of paths to ignore.
     *
     * @return The ignore list.
     */
    public Set<String> getIgnoreList() {
        return ignoreList;
    }

    /**
     * Gets the list of currently enabled files.
     *
     * @return The list of enabled files.
     */
    public List<File> getEnabledFiles() {
        if (activeFileManager != null) {
            return activeFileManager.getEnabledFiles();
        }
        return Collections.emptyList();
    }

    /**
     * Retrieves the singleton instance of App.
     *
     * @return The App instance.
     */
    public static App getInstance() {
        return instance;
    }

    /**
     * Retrieves the OpenAIProvider instance.
     *
     * @return The OpenAIProvider.
     */
    public OpenAIProvider getOpenAIProvider() {
        return this.openAIProvider;
    }

    /**
     * Submits a request to the OpenAI provider with the specified model and description.
     *
     * @param model       The model to use for the request.
     * @param description The description of the request.
     */
    public void submitRequest(String model, String description) {
        OpenAIProvider openAIProvider = App.getInstance().getOpenAIProvider();

        String structure = activeFileManager.formatEnabledFiles();

        // Replace the top level directory with a dot
        structure = structure.replaceFirst(this.currentWorkspace.getName() + "/", "./");

        String PROMPT_TEMPLATE = FileUtils.readFileToString(new File(Constants.PROMPT_TEMPLATE_FILE));

        if (PROMPT_TEMPLATE == null) {
            String defaultPromptBase64 = Constants.DEFAULT_PROMPT_TEMPLATE_B64;
            byte[] decodedBytes = Base64.getDecoder().decode(defaultPromptBase64);
            String defaultPrompt = new String(decodedBytes, StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(new File(Constants.PROMPT_TEMPLATE_FILE), defaultPrompt);
            PROMPT_TEMPLATE = defaultPrompt;
        }

        String prompt = PROMPT_TEMPLATE
                .replace("<REPLACEME_WITH_REQUEST>", description)
                .replace("<REPLACEME_WITH_STRUCTURE>", structure)
                .replace("<REPLACEME_WITH_FILES>", activeFileManager.formatEnabledFiles())
                .replace("<REPLACEME_WITH_OS>", System.getProperty("os.name"));

        String response = openAIProvider.request(model, prompt);
        System.out.println(response);

        // Trim leading and trailing whitespaces
        response = response.trim();
        this.handleCodeResponse(response);

        // Refresh the directory tree
        this.mainWindow.getProjectPanel().refreshTree(this.currentWorkspace);
    }

    /**
     * Handles the response received from the OpenAI provider.
     *
     * @param formatted The formatted response string.
     */
    private void handleCodeResponse(String formatted) {
        boolean valid = false;
        String currentCode = formatted;
        String exceptionMessage = "";

        while (!valid) {
            try {
                List<FileContent> parse = CustomParser.parse(currentCode);
                processParsedFiles(parse);
                valid = true;
            } catch (Exception e) {
                exceptionMessage = e.getMessage();
                // Show JsonRepair dialog
                CodeRepair repairDialog = new CodeRepair(mainWindow, currentCode, exceptionMessage);
                repairDialog.setVisible(true);

                String userCorrectedCode = repairDialog.getCorrectedCode();
                if (userCorrectedCode != null) {
                    currentCode = userCorrectedCode;
                } else {
                    // User cancelled the dialog
                    JOptionPane.showMessageDialog(null, "Code repair was cancelled. Operation aborted.", "Operation Aborted", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }
    }

    /**
     * Processes the list of parsed files and updates the workspace accordingly.
     *
     * @param parsedFiles The list of parsed FileContent objects.
     */
    private void processParsedFiles(List<FileContent> parsedFiles) {
        try {
            File vaiDir = FileUtils.getWorkspaceVaiDir(this.currentWorkspace);
            File backupDirectory = new File(vaiDir, Constants.VAI_BACKUP_DIR + "/" + getNextIncrementalBackupNumber());

            while (backupDirectory.exists()) {
                backupDirectory = new File(vaiDir, Constants.VAI_BACKUP_DIR + "/" + getNextIncrementalBackupNumber());
            }

            boolean mkdirs = backupDirectory.mkdirs();
            if (!mkdirs) {
                JOptionPane.showMessageDialog(null, "Failed to create backup directory: " + backupDirectory.getAbsolutePath());
                return;
            }

            Path workspacePath = Paths.get(this.currentWorkspace.getAbsolutePath());

            for (FileContent fileContent : parsedFiles) {
                String fileName = fileContent.getFileName();
                String newContents = fileContent.getNewContents();
                String fileType = fileContent.getFileType();

                // Handle SHOW_MESSAGE and chat types
                if (fileName.toUpperCase().contains("SHOW_MESSAGE") || fileType.equals("chat")) {
                    JScrollPane scrollPane = createMessageDialog(newContents);
                    JOptionPane.showMessageDialog(null, scrollPane, "Message from model", JOptionPane.INFORMATION_MESSAGE);
                    continue;
                }

                // Handle RUN_COMMAND and shell types
                if (fileName.toUpperCase().contains("RUN_COMMAND") || fileType.equals("run")) {
                    handleRunCommand(newContents);
                    continue;
                }

                // Handle LLM_PROMPT
                if (fileName.toUpperCase().contains("LLM_PROMPT") || fileType.equals("prompt")) {
                    this.getClient().setLLMPrompt(newContents);
                    continue;
                }

                System.out.println("Writing to " + fileName);

                File targetFile = new File(workspacePath + "/" + fileName);
                File backupFile = new File(backupDirectory.getAbsolutePath() + "/" + fileName);
                boolean parentDirsCreated = backupFile.getParentFile().mkdirs();
                if (!parentDirsCreated) {
                    System.out.println("[WARNING][Backup] Failed to create parent directory for " + backupFile.getAbsolutePath());
                }

                // Copy the file to the backup directory
                if (!targetFile.exists()) {
                    System.out.println("Origin file did not exist. That's okay.");
                } else {
                    try {
                        Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Write the new contents to the file
                if (!targetFile.exists()) {
                    boolean parentCreated = targetFile.getParentFile().mkdirs();
                    if (!parentCreated) {
                        System.out.println("[WARNING][Backup] Failed to create parent directory for target " + targetFile.getAbsolutePath());
                    }
                    boolean fileCreated = targetFile.createNewFile();
                    if (!fileCreated) {
                        System.out.println("[WARNING][Backup] Failed to create target file " + targetFile.getAbsolutePath());
                    }
                }
                FileUtils.writeStringToFile(targetFile, newContents);

                // Launch diff tool (meld)
                MeldLauncher.launchMeld(backupFile.toPath(), targetFile.toPath());
            }
        } catch (Exception e) {
            // Popup a message saying it failed.
            JOptionPane.showMessageDialog(null, "Failed to handle parsed files: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles the RUN_COMMAND functionality by prompting the user and executing the command if approved.
     *
     * @param command The shell command to execute.
     */
    private void handleRunCommand(String command) {
        // Prompt the user with the command and options to approve or deny
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel("The LLM would like to run this command:");
        JTextField textField = new JTextField(command);
        textField.setEditable(false);
        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                mainWindow,
                panel,
                "Run Command Approval",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            // Execute the command
            String output = executeShellCommand(command);
            // Display the output
            JTextArea textArea = new JTextArea(output);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 400));
            JOptionPane.showMessageDialog(
                    mainWindow,
                    scrollPane,
                    "Command Output",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            // User denied the command execution
            JScrollPane scrollPane = createMessageDialog("Command execution was denied by the user.");
            JOptionPane.showMessageDialog(null, scrollPane, "Command Denied", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Executes a shell command in the current workspace directory and returns the output.
     *
     * @param command The shell command to execute.
     * @return The combined output and error streams from the command execution.
     */
    private String executeShellCommand(String command) {
        if (command == null || command.isEmpty()) {
            return "";
        }

        StringBuilder outputBuilder = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd.exe", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }
            pb.directory(currentWorkspace);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            outputBuilder.append("\n").append("Process exited with code ").append(exitCode).append(".");

        } catch (IOException | InterruptedException e) {
            outputBuilder.append("An error occurred while executing the command: ").append(e.getMessage());
        }
        return outputBuilder.toString();
    }

    /**
     * Creates a JScrollPane containing the provided message for display purposes.
     *
     * @param newContents The message content.
     * @return A JScrollPane with the message.
     */
    @NotNull
    private static JScrollPane createMessageDialog(String newContents) {
        JTextArea messageArea = new JTextArea(newContents);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        return scrollPane;
    }

    public Client getClient() {
        return this.mainWindow;
    }

    /**
     * Executes the given file by opening it in the system's terminal.
     *
     * @param file The executable file to run.
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
