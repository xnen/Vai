package io.improt.vai.backend;

import io.improt.vai.frame.Client;
import io.improt.vai.frame.CodeRepair;
import io.improt.vai.openai.OpenAIProvider;
import io.improt.vai.util.FileTreeBuilder;
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
    private final List<File> enabledFiles = new ArrayList<>();

    private final Set<String> ignoreList = new HashSet<>();

    private static App instance;
    private final Client mainWindow;
    private OpenAIProvider openAIProvider;

    private int currentIncrementalBackupNumber = 0;

    public App(Client mainWindow) {
        this.mainWindow = mainWindow;
        instance = this;
    }

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
            List<File> loadedEnabledFiles = FileUtils.loadEnabledFiles(currentWorkspace);
            enabledFiles.addAll(loadedEnabledFiles);

            for (File file : loadedEnabledFiles) {
                addToRecentlyActive(file);
            }
        }
    }

    private int getNextIncrementalBackupNumber() {
        int nextNumber = this.currentIncrementalBackupNumber + 1;
        this.currentIncrementalBackupNumber = nextNumber;
        FileUtils.saveIncrementalBackupNumber(this.currentIncrementalBackupNumber, this.currentWorkspace);
        return nextNumber;
    }

    public void openDirectory(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            finalizeDirectoryOpen(chooser.getSelectedFile());
        }
    }

    private void finalizeDirectoryOpen(File directory) {
        currentWorkspace = directory;
        FileUtils.saveLastWorkspace(currentWorkspace);
        FileUtils.createDefaultVaiignore(currentWorkspace);
        ignoreList.clear();
        ignoreList.addAll(FileUtils.readVaiignore(currentWorkspace));
        enabledFiles.clear();

        List<File> loadedEnabledFiles = FileUtils.loadEnabledFiles(currentWorkspace);
        enabledFiles.addAll(loadedEnabledFiles);
        mainWindow.getProjectPanel().refreshTree(currentWorkspace);
        currentIncrementalBackupNumber = FileUtils.loadIncrementalBackupNumber(currentWorkspace);

        for (File file : loadedEnabledFiles) {
            addToRecentlyActive(file);
        }

        FileUtils.addRecentProject(currentWorkspace.getAbsolutePath());
    }

    public void openDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            finalizeDirectoryOpen(directory);
        } else {
            JOptionPane.showMessageDialog(mainWindow, "The provided path is not a valid directory.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String getApiKey() {
        return FileUtils.readFileToString(new File(Constants.API_KEY_PATH));
    }

    public File getCurrentWorkspace() {
        return currentWorkspace;
    }

    public Set<String> getIgnoreList() {
        return ignoreList;
    }

    public void toggleFile(File file) {
        if (enabledFiles.contains(file)) {
            enabledFiles.remove(file);
        } else {
            enabledFiles.add(file);
            addToRecentlyActive(file);
        }

        FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);

        String tree = FileTreeBuilder.createTree(this.currentWorkspace, enabledFiles);
        System.out.println(tree);
    }

    private void addToRecentlyActive(File file) {
        if (file == null) return;
        List<String> recentFiles = FileUtils.loadRecentlyActiveFiles(currentWorkspace);
        String filePath = file.getAbsolutePath();
        recentFiles.remove(filePath);
        recentFiles.add(0, filePath);

        if (recentFiles.size() > 100) {
            recentFiles = recentFiles.subList(0, 100);
        }
        FileUtils.saveRecentlyActiveFiles(recentFiles, currentWorkspace);
    }

    public void clearActiveFiles() {
        enabledFiles.clear();
        FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);
    }

    public List<File> getEnabledFiles() {
        return enabledFiles;
    }

    public static App getInstance() {
        return instance;
    }

    public void removeFile(String selectedFile) {
        enabledFiles.removeIf(file -> file.getName().equals(selectedFile));
        // Save the updated enabled files list
        FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);
    }

    public void addFile(File file) {
        if (file != null && file.exists() && file.isFile()) {
            String newFilePath = file.getAbsolutePath();
            for (File enabledFile : enabledFiles) {
                if (enabledFile.getAbsolutePath().equals(newFilePath)) {
                    // File already exists in enabledFiles
                    return;
                }
            }
            enabledFiles.add(file);
            // Save the updated enabled files list
            FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);

            String tree = FileTreeBuilder.createTree(this.currentWorkspace, enabledFiles);
            System.out.println(tree);

            // Also add to recently active files
            addToRecentlyActive(file);
        }
    }

    public OpenAIProvider getOpenAIProvider() {
        return this.openAIProvider;
    }

    public void submitRequest(String model, String description) {
        OpenAIProvider openAIProvider = App.getInstance().getOpenAIProvider();

        String structure = FileTreeBuilder.createTree(this.currentWorkspace, enabledFiles);

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
                .replace("<REPLACEME_WITH_FILES>", formatEnabledFiles());

        String response = openAIProvider.request(model, prompt);
        System.out.println(response);

        // Trim leading and trailing whitespaces
        response = response.trim();
        this.handleCodeResponse(response);

        // Refresh the directory tree
        this.mainWindow.getProjectPanel().refreshTree(this.currentWorkspace);
    }

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

                if (fileName.toUpperCase().contains("SHOW_MESSAGE")) {
                    // This is a custom message from the LLM, show it to the user, and continue.
                    JScrollPane scrollPane = createMessageDialog(newContents);
                    JOptionPane.showMessageDialog(null, scrollPane, "Message from model", JOptionPane.INFORMATION_MESSAGE);
                    continue;
                }

                System.out.println("Writing to " + fileName);

                File targetFile = new File(workspacePath + "/" + fileName);
                File backupFile = new File(backupDirectory.getAbsolutePath() + "/" + fileName);
                boolean b = backupFile.getParentFile().mkdirs();
                if (!b) {
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
                    boolean b1 = targetFile.getParentFile().mkdirs();
                    if (!b1) {
                        System.out.println("[WARNING][Backup] Failed to create parent directory for target " + targetFile.getAbsolutePath());
                    }
                    boolean b2 = targetFile.createNewFile();
                    if (!b2) {
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

    public String formatEnabledFiles() {
        StringBuilder sb = new StringBuilder();
        Path workspacePath = Paths.get(this.currentWorkspace.getAbsolutePath());

        for (File file : enabledFiles) {
            String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);

            Path relativePath = workspacePath.relativize(Paths.get(file.getAbsolutePath()));

            sb.append("== ").append(relativePath).append(" ==\n");
            sb.append("```").append(extension).append("\n");
            sb.append(FileUtils.readFileToString(file));
            sb.append("\n```\n");
        }
        return sb.toString();
    }
}
