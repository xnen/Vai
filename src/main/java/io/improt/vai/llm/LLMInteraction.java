package io.improt.vai.llm;

import io.improt.vai.backend.App;
import io.improt.vai.backend.plugin.PluginManager;
import io.improt.vai.backend.plugin.AbstractPlugin;
import io.improt.vai.frame.ClientFrame;
import io.improt.vai.frame.dialogs.RepairDialog;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.util.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

public class LLMInteraction {
    private int currentIncrementalBackupNumber = 0;
    private final ClientFrame mainWindow;
    private final App app;

    private final PluginManager pluginManager;

    public LLMInteraction(App app) {
        this.mainWindow = app.getClient();
        this.app = app;
        // Initialize PluginManager so that it becomes available through its singleton accessor.
        this.pluginManager = new PluginManager();
    }

    public void init() {
        currentIncrementalBackupNumber = FileUtils.loadIncrementalBackupNumber(this.app.getCurrentWorkspace());
    }

    /**
     * Handles the response received from the OpenAI provider.
     *
     * @param formatted The formatted response string.
     */
    public void handleCodeResponse(String formatted) {
        boolean valid = false;
        String currentCode = formatted;
        String exceptionMessage;

        while (!valid) {
            try {
                List<BerzfadParser.FileContent> parse = BerzfadParser.parse(currentCode);
                processParsedFiles(parse);
                valid = true;
            } catch (Exception e) {
                exceptionMessage = e.getMessage();
                // Show JsonRepair dialog
                RepairDialog repairDialog = new RepairDialog(mainWindow, currentCode, exceptionMessage);
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
     * Submits a request to the LLM provider with the specified model, description, and (optionally) reasoning effort.
     *
     * @param model           The model to use for the request.
     * @param userRequest     The description of the request.
     */
    public void submitRequest(String model, String userRequest) {
        App app = App.getInstance();
        IModelProvider llmProvider = app.getLLMProvider(model); // Get provider based on model name

        if (llmProvider == null) {
            JOptionPane.showMessageDialog(mainWindow, "No LLM provider found for model: " + model, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String structure = FileTreeBuilder.createTree(app.getCurrentWorkspace(), app.getDynamicAndActiveFiles());

        // Replace the top level directory with a dot
        structure = structure.replaceFirst(app.getCurrentWorkspace().getName() + "/", "./");

        String PROMPT_TEMPLATE = FileUtils.readFileToString(new File(Constants.PROMPT_TEMPLATE_FILE));

        // Temporary Hack for DeepSeek.
        if (model.equals("DeepSeek")) {
            PROMPT_TEMPLATE = FileUtils.readFileToString(new File("data/deepseek.template"));
            System.out.println("Using DeepSeek template");
        }

        if (PROMPT_TEMPLATE == null) {
            String defaultPromptBase64 = "todo";//Constants.DEFAULT_PROMPT_TEMPLATE_B64;
            byte[] decodedBytes = Base64.getDecoder().decode(defaultPromptBase64);
            String defaultPrompt = new String(decodedBytes, StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(new File(Constants.PROMPT_TEMPLATE_FILE), defaultPrompt);
            PROMPT_TEMPLATE = defaultPrompt;
        }

        String prompt = PROMPT_TEMPLATE
                .replace("<REPLACEME_WITH_REQUEST>", userRequest)
                .replace("<REPLACEME_WITH_STRUCTURE>", structure)
                .replace("<REPLACEME_WITH_FILES>", app.getActiveFileManager().formatEnabledFiles())
                .replace("<REPLACEME_WITH_OS>", System.getProperty("os.name"))
                .replace("<REPLACEME_WITH_FEATURES>", buildFeaturesBlock())
                .replace("<REPLACEME_WITH_ADDL_INFO>", app.popAdditionalData());

        System.out.println("=== LLM PROMPT ===");
        System.out.println(prompt);
        System.out.println(userRequest);
        System.out.println("====================");

        // Get the list of enabled files to be sent to Gemini. Currently, only text-based files are included in the prompt string.
        // For non-text files (images, audio), we'll pass them separately.
        List<File> filesForContext = this.getMedia();

        String response = llmProvider.request(prompt, userRequest, filesForContext); // Pass the reasoningEffort parameter.
        System.out.println("=== LLM RESPONSE ===");
        System.out.println(response);
        System.out.println("====================");


        if (response == null) {
            return;
        }

        // Trim leading and trailing whitespaces
        response = response.trim();
        this.handleCodeResponse(response);

        // Refresh the directory tree
        app.getClient().getProjectPanel().refreshTree(app.getCurrentWorkspace());
        // GPTODO: We need to refresh the file viewer as well.
    }

    @NotNull
    private List<File> getMedia() {
        List<File> filesForContext = this.app.getActiveFileManager().getEnabledFiles(); // Get all enabled files. You can filter if needed.
        List<File> filteredFilesForContext = new ArrayList<>();
        String[] allowedExtensions = {"png", "jpg", "jpeg", "mp3", "wav", "mp4"};

        for (File file : filesForContext) {
            String fileName = file.getName().toLowerCase();
            for (String ext : allowedExtensions) {
                if (fileName.endsWith("." + ext)) {
                    filteredFilesForContext.add(file);
                    break; // No need to check other extensions for this file
                }
            }
        }
        filesForContext = filteredFilesForContext; // Use the filtered list
        return filesForContext;
    }

    /**
     * Builds a block of enabled features (plugin identifiers) to insert into the prompt.
     */
    private String buildFeaturesBlock() {
        PluginManager pm = PluginManager.getInstance();
        if (pm == null) return "";

        StringBuilder sb = new StringBuilder();

        for (AbstractPlugin plugin : pm.getPlugins()) {
            if (plugin.isActive()) {
                sb.append(plugin.getFeaturePrompt()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Processes the list of parsed files and updates the workspace accordingly.
     *
     * @param parsedFiles The list of parsed FileContent objects.
     */
    private void processParsedFiles(List<BerzfadParser.FileContent> parsedFiles) {
        try {
            File vaiDir = FileUtils.getWorkspaceVaiDir(this.app.getCurrentWorkspace());
            File backupDirectory = new File(vaiDir, Constants.VAI_BACKUP_DIR + "/" + getNextIncrementalBackupNumber());

            while (backupDirectory.exists()) {
                backupDirectory = new File(vaiDir, Constants.VAI_BACKUP_DIR + "/" + getNextIncrementalBackupNumber());
            }

            if (!backupDirectory.mkdirs()) {
                JOptionPane.showMessageDialog(null, "Failed to create backup directory: " + backupDirectory.getAbsolutePath());
                return;
            }

            Path workspacePath = Paths.get(this.app.getCurrentWorkspace().getAbsolutePath());
            System.out.println("Parsed '" + parsedFiles.size() + "' files...");

            for (BerzfadParser.FileContent fileContent : parsedFiles) {
                if (fileContent.getFileName().trim().isEmpty()) {
                    System.out.println(fileContent.toString());
                    System.out.println("[SEVERE] File parsed had no filename!?");
                    continue;
                }

                String fileName = fileContent.getFileName();
                String newContents = fileContent.getNewContents();
                String fileType = fileContent.getFileType();

                // Plugins
                if (pluginManager.passResponse(fileName, fileType, newContents)) {
                    continue;
                }

                System.out.println("Writing to " + fileName);

                File targetFile = new File(workspacePath + "/" + fileName);

                // Security Check: Ensure the target file is within the project directory
                boolean securityValidation = VaiUtils.doSecurityValidation(targetFile, newContents);
                if (!securityValidation) {
                    continue;
                }

                File backupFile = new File(backupDirectory.getAbsolutePath() + "/" + fileName);

                if (!backupFile.getParentFile().mkdirs()) {
                    System.out.println("[WARNING][Backup] Failed to create parent directory for " + backupFile.getAbsolutePath());
                }

                // Copy the file to the backup directory
                if (targetFile.exists()) {
                    try {
                        Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (!targetFile.getParentFile().mkdirs()) {
                        System.out.println("[WARNING][Backup] Failed to create parent directory for target " + targetFile.getAbsolutePath());
                    }
                    if (!targetFile.createNewFile()) {
                        System.out.println("[WARNING][Backup] Failed to create target file " + targetFile.getAbsolutePath());
                    }

                    // Automatically add to enabled files
                    // Resolved issue with duplicate due to '.' in path by normalizing the file path
                    Path normalizedPath = targetFile.toPath().normalize();
                    File normalizedFile = normalizedPath.toFile();
                    App.getInstance().getActiveFileManager().addFile(normalizedFile);
                }

                FileUtils.writeStringToFile(targetFile, newContents);

                // Refresh FileViewer if the currently displayed file is modified
                File currentFileDisplayed = mainWindow.getCurrentFile();
                if (currentFileDisplayed != null && targetFile.getAbsolutePath().equals(currentFileDisplayed.getAbsolutePath())) {
                    mainWindow.refreshFileViewer();
                }

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
     * Retrieves the next incremental backup number and updates the stored value.
     *
     * @return The next incremental backup number.
     */
    private int getNextIncrementalBackupNumber() {
        int nextNumber = this.currentIncrementalBackupNumber + 1;
        this.currentIncrementalBackupNumber = nextNumber;
        FileUtils.saveIncrementalBackupNumber(this.currentIncrementalBackupNumber, app.getCurrentWorkspace());
        return nextNumber;
    }

    /**
     * New method for Smart Subworkspace Creation.
     * Queries an LLM to suggest relevant file/directory paths and a name for a new sub-workspace
     * based on a user query and the current repository map.
     *
     * @param modelName                The name of the LLM model to use.
     * @param userQueryForSubworkspace The user's textual request for the sub-workspace (e.g., "files for chat feature").
     * @param repositoryMapContext     A string containing the concatenated mappings of the current workspace.
     * @return The raw string response from the LLM.
     * @throws RuntimeException if the LLM provider is not found or an error occurs during the request.
     */
    public String suggestSubworkspaceCreationDetails(String modelName, String userQueryForSubworkspace, String repositoryMapContext) {
        IModelProvider llmProvider = app.getLLMProvider(modelName);
        if (llmProvider == null) {
            throw new RuntimeException("LLM provider not found for model: " + modelName);
        }

        String systemInstructionPrompt =
                "Your goal is to analyze a user's request and a provided repository map (a summary of files and their contents/structure) to identify relevant files or directories for creating a new \"sub-workspace\". You also need to suggest a concise and descriptive name for this new sub-workspace.\n\n" +
                        "Based on the USER REQUEST and the REPOSITORY MAP, provide your response in the following format STRICTLY:\n\n" +
                        "RELEVANT_PATHS_START\n" +
                        "path/to/relevant/file1.filetype\n" +
                        "path/to/relevant_directory/\n" +
                        "another/path/to/file.ts\n" +
                        "RELEVANT_PATHS_END\n\n" +
                        "SUGGESTED_NAME_START\n" +
                        "MyNewChatFeatureSubWorkspace\n" +
                        "SUGGESTED_NAME_END\n\n" +
                        "Instructions for RELEVANT_PATHS:\n" +
                        "- List each relevant file or directory path on a new line, relative to the project root if possible.\n" +
                        "- If a path is a directory, ensure it ends with a forward slash (e.g., `path/to/directory/`).\n" +
                        "- If a path is a file, it should not end with a slash.\n" +
                        "- Be comprehensive but also focused. Only include paths highly relevant to the user's request.\n\n" +
                        "Instructions for SUGGESTED_NAME:\n" +
                        "- Provide a single, concise, camelCase or PascalCase name suitable for a sub-workspace.\n" +
                        "- The name should reflect the user's request or the content of the identified paths.\n" +
                        "- Do not include any other text or explanation around the name within its START/END tags.";

        String combinedUserContent =
                "USER_REQUEST:\n" + userQueryForSubworkspace + "\n\n" +
                        "REPOSITORY_MAP:\n" + repositoryMapContext;

        System.out.println("=== LLM PROMPT (Smart Subworkspace) ===");
        System.out.println("System Prompt:\n" + systemInstructionPrompt);
        System.out.println("User Content (excerpt):\n" + combinedUserContent.substring(0, Math.min(combinedUserContent.length(), 500)) + "..."); // Log excerpt
        System.out.println("=======================================");

        // This specific task might benefit from higher reasoning, but we use the app's configured one for now.
        // Or, we could override it: app.setReasoningEffort(ReasoningEffort.HIGH);
        String response = llmProvider.request(systemInstructionPrompt, combinedUserContent, null); // No specific files for this type of request
        // app.setReasoningEffort(originalReasoningEffort); // Restore if changed

        System.out.println("=== LLM RESPONSE (Smart Subworkspace) ===");
        System.out.println(response);
        System.out.println("=======================================");

        return response != null ? response.trim() : "";
    }
}
