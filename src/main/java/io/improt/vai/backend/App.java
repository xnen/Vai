package io.improt.vai.backend;

import com.openai.models.ChatCompletionReasoningEffort;
import io.improt.vai.frame.ClientFrame;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.*;
import io.improt.vai.util.FileUtils;
import io.improt.vai.util.Constants;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.Objects;

public class App {

    public static final String API_KEY = Constants.OAI_API_KEY_PATH;
    private File currentWorkspace;
    private static App instance;
    private ClientFrame mainWindow;
    private LLMRegistry llmRegistry;
    private LLMInteraction llmInteraction;
    private ActiveFileManager activeFileManager;

    private static final int VAI_INTEGRATION_PORT = 12345; // Port for Vai integration
    private static final String VAI_INTEGRATION_SALT = "YourSuperSecretSalt"; // TODO: Configurable.

    private ChatCompletionReasoningEffort reasoningEffort = ChatCompletionReasoningEffort.MEDIUM;

    public App(ClientFrame mainWindow) {
        this.mainWindow = mainWindow;
        instance = this;
        startGlobalHotkeyListener();
    }

    public void init() {
        FileUtils.loadWorkspaceMappings();

        llmRegistry = new LLMRegistry();
        llmRegistry.registerModels();

        currentWorkspace = FileUtils.loadLastWorkspace();

        if (currentWorkspace != null) {
            mainWindow.getProjectPanel().refreshTree(currentWorkspace);

            activeFileManager = new ActiveFileManager(currentWorkspace);
            activeFileManager.addEnabledFilesChangeListener(updatedEnabledFiles -> mainWindow.getProjectPanel().refreshTree(currentWorkspace));
        }

        this.llmInteraction = new LLMInteraction(this);
        llmInteraction.init();

        startSocketListener(); // Start listening for socket connections
    }

    private void startGlobalHotkeyListener() {
        new Thread(() -> {
            try {
                String cwd = System.getProperty("user.dir");
                ProcessBuilder pb = new ProcessBuilder("python3", cwd + File.separator + "python" + File.separator + "global_hotkey_listener.py");
                pb.directory(new File(cwd));
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                // Log that the listener is running
                System.out.println("[App] Running global hotkey listener");
                
                // Optionally read and output the process's input stream in background if needed.
                new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("[GlobalHotkeyListener] " + line);
                        }
                    } catch (IOException e) {
                        // Ignored
                    }
                }).start();
            } catch (IOException e) {
                System.err.println("[App] Failed to register hotkey... do you have pynput installed?");
            }
        }).start();
    }

    private void startSocketListener() {
        Thread socketThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(VAI_INTEGRATION_PORT, 50, InetAddress.getLoopbackAddress())) {
                System.out.println("[App] App integration port = " + VAI_INTEGRATION_PORT + " on localhost.");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    handleClientConnection(clientSocket);
                }
            } catch (IOException e) {
                System.err.println("[App] Could not listen on port " + VAI_INTEGRATION_PORT + " on localhost: " + e.getMessage());
            }
        });
        socketThread.setDaemon(true);
        socketThread.start();
    }

    private void handleClientConnection(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            if (!clientSocket.getInetAddress().isLoopbackAddress()) {
                System.err.println("Connection rejected: Not from localhost.");
                return; // Reject connection
            }

            // 1. Read and verify secret salt
            String receivedSalt = in.readLine();
            if (!Objects.equals(receivedSalt, VAI_INTEGRATION_SALT)) {
                System.err.println("Connection rejected: Invalid secret salt.");
                return; // Reject connection
            }

            // 2. Read command or file path
            String commandOrPath = in.readLine();
            if (commandOrPath != null) {
                if ("open-dialog".equals(commandOrPath)) {
                    SwingUtilities.invokeLater(() -> {
                        if (mainWindow.isChatDialogClosed()) {
                            mainWindow.openChatDialog(false);
                        }
                    });
                    return;
                } else if ("open-dialog-audio".equals(commandOrPath)) {
                    SwingUtilities.invokeLater(() -> {
                        if (mainWindow.isChatDialogClosed()) {
                            mainWindow.openChatDialog(true);
                        }
                    });
                    return;
                } else if (Files.exists(Paths.get(commandOrPath))) {
                    System.out.println("Received file path: " + commandOrPath);
                    File fileToOpen = new File(commandOrPath);
                    SwingUtilities.invokeLater(() -> {
                        mainWindow.setState(Frame.NORMAL); // Ensure window is not minimized
                        String os = System.getProperty("os.name").toLowerCase();
                        boolean isLinux = os.contains("linux");
                        if (isLinux) {
                            // Workaround: temporarily force the window always on top to gain focus
                            mainWindow.setAlwaysOnTop(true);
                        }
                        mainWindow.toFront();
                        mainWindow.requestFocus();
                        if (isLinux) {
                            // Remove the always-on-top flag after a short delay
                            Timer timer = new Timer(200, e -> mainWindow.setAlwaysOnTop(false));
                            timer.setRepeats(false);
                            timer.start();
                        }
                        activeFileManager.addFile(fileToOpen);
                        mainWindow.getFileViewerPanel().displayFile(fileToOpen);
                    });
                } else {
                    System.err.println("Invalid command or file path received: " + commandOrPath);
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling connection: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore close exception
            }
        }
    }

    public LLMInteraction getLLM() {
        return llmInteraction;
    }

    // Open File Dialog for Workspace
    public void showOpenWorkspaceDialog(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            finalizeOpenProject(chooser.getSelectedFile());
        }
    }

    /**
     * Finalizes the process of opening a new directory as the workspace.
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
        this.mainWindow.getProjectPanel().refreshTree(this.currentWorkspace);
        mainWindow.updateTitle(); // Call updateTitle here!
    }

    public ActiveFileManager getActiveFileManager() {
        return activeFileManager;
    }

    /**
     * Listener to refresh the project tree when enabled files change.
     */
    private final ActiveFileManager.EnabledFilesChangeListener projectTreeRefresher = updatedEnabledFiles -> mainWindow.getProjectPanel().refreshTree(currentWorkspace);

    public void openWorkspace(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            finalizeOpenProject(directory);
        } else {
            JOptionPane.showMessageDialog(mainWindow, "The provided path is not a valid directory.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String getOpenAIKey() {
        return FileUtils.readFileToString(new File(Constants.OAI_API_KEY_PATH));
    }

    public static String GetNvidiaKey() {
        return FileUtils.readFileToString(new File(Constants.NV_API_KEY_PATH));
    }

    public File getCurrentWorkspace() {
        return currentWorkspace;
    }

    public List<File> getEnabledFiles() {
        if (activeFileManager != null) {
            return activeFileManager.getEnabledFiles();
        }
        return Collections.emptyList();
    }

    public List<File> getDynamicAndActiveFiles() {
        if (activeFileManager != null) {
            return activeFileManager.concatenateWithoutDuplicates();
        }
        return Collections.emptyList();
    }

    public static App getInstance() {
        return instance;
    }

    public IModelProvider getLLMProvider(String modelName) {
        return this.llmRegistry.getModel(modelName);
    }
    
    // NEW: Expose the LLMRegistry to allow dynamic model population.
    public LLMRegistry getLLMRegistry() {
        return this.llmRegistry;
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

    public File saveImageAsTempFile(Image image) {
        if (currentWorkspace == null) {
            JOptionPane.showMessageDialog(mainWindow, "No workspace is open to save the image.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        File tempDir = FileUtils.getWorkspaceVaiDir(currentWorkspace);
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            JOptionPane.showMessageDialog(mainWindow, "Failed to create temporary directory.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        File tempFile = new File(tempDir, UUID.randomUUID().toString() + ".png"); // Default to PNG
        try {
            BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            ImageIO.write(bufferedImage, "png", tempFile); // Save as PNG
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainWindow, "Failed to save image to file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public ChatCompletionReasoningEffort getConfiguredReasoningEffort() {
        return this.reasoningEffort;
    }

    public void setReasoningEffort(ChatCompletionReasoningEffort effort) {
        this.reasoningEffort = effort;
    }
}
