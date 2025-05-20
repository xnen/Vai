package io.improt.vai.llm;

import io.improt.vai.backend.App;
import io.improt.vai.frame.ClientFrame;
import io.improt.vai.frame.dialogs.SmartSubworkspaceDialog;
import io.improt.vai.mapping.SubWorkspace;
import io.improt.vai.mapping.WorkspaceMapper;
import io.improt.vai.util.FileUtils;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SmartSubworkspaceCreator {

    private final ClientFrame parentFrame;
    private final App appInstance;

    // Define START and END tags for parsing LLM response
    private static final String RELEVANT_PATHS_START_TAG = "RELEVANT_PATHS_START";
    private static final String RELEVANT_PATHS_END_TAG = "RELEVANT_PATHS_END";
    private static final String SUGGESTED_NAME_START_TAG = "SUGGESTED_NAME_START";
    private static final String SUGGESTED_NAME_END_TAG = "SUGGESTED_NAME_END";

    public SmartSubworkspaceCreator(ClientFrame parentFrame) {
        this.parentFrame = parentFrame;
        this.appInstance = App.getInstance();
    }

    public void startSmartCreationProcess() {
        if (appInstance.getCurrentWorkspace() == null) {
            JOptionPane.showMessageDialog(parentFrame, "Please open a workspace first.", "No Workspace", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SmartSubworkspaceDialog dialog = new SmartSubworkspaceDialog(parentFrame);
        dialog.setVisible(true); // Shows initial view for prompt

        String userPrompt = dialog.getUserPrompt();
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            // User cancelled or entered no prompt
            return;
        }

        // Gather repository map data
        WorkspaceMapper workspaceMapper = new WorkspaceMapper(appInstance.getCurrentWorkspace());
        String repositoryMapContext = workspaceMapper.getAllMappingsConcatenated();

        if (repositoryMapContext.trim().isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame, "The current workspace has no mapped files. Please map some files first using 'Manage Workspaces'.", "No Mappings", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Call LLMInteraction
        parentFrame.getStatusBar().setText("Querying LLM for sub-workspace suggestions...");
        String llmResponse;
        try {
            // Choose a capable model
            String modelName = "Gemini Pro"; // Or allow user to select, or pick a default like GPT-4o-mini
            if (appInstance.getLLMRegistry().getModel(modelName) == null) { // Fallback if Gemini Pro is not available
                List<String> models = appInstance.getLLMRegistry().getRegisteredModelNames();
                if (models.contains("GPT-4o mini")) modelName = "GPT-4o mini";
                else if (models.contains("GPT-4o")) modelName = "GPT-4o";
                else if (!models.isEmpty()) modelName = models.get(0); // Last resort
                else {
                     JOptionPane.showMessageDialog(parentFrame, "No suitable LLM models available.", "Error", JOptionPane.ERROR_MESSAGE);
                     parentFrame.getStatusBar().setText("Ready");
                     return;
                }
            }
            llmResponse = appInstance.getLLM().suggestSubworkspaceCreationDetails(modelName, userPrompt, repositoryMapContext);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame, "Error communicating with LLM: " + e.getMessage(), "LLM Error", JOptionPane.ERROR_MESSAGE);
            parentFrame.getStatusBar().setText("Ready");
            return;
        }
        parentFrame.getStatusBar().setText("Processing LLM response...");

        // Parse LLM response
        List<String> suggestedPaths = parseRelevantPaths(llmResponse);
        String suggestedName = parseSuggestedName(llmResponse);

        if (suggestedPaths.isEmpty() && (suggestedName == null || suggestedName.trim().isEmpty())) {
            JOptionPane.showMessageDialog(parentFrame, "LLM did not provide valid suggestions. The response was:\n" + llmResponse, "LLM Response Issue", JOptionPane.INFORMATION_MESSAGE);
            parentFrame.getStatusBar().setText("Ready");
            return;
        }
        if (suggestedName == null || suggestedName.trim().isEmpty()) {
            suggestedName = "SuggestedSubWorkspace"; // Default if name parsing fails
        }


        // Show results view in the same dialog
        dialog.switchToResultsView(suggestedPaths, suggestedName);
        dialog.setVisible(true);

        if (dialog.isConfirmedCreation()) {
            String finalName = dialog.getFinalSubworkspaceName();
            boolean createFileBased = dialog.shouldCreateAsFileBased();
            List<String> pathsToProcess = dialog.getFinalPathsToProcess(); // These are workspace-relative or absolute

            if (appInstance.getSubWorkspaceByName(finalName) != null) {
                JOptionPane.showMessageDialog(parentFrame, "A Sub-Workspace with the name '" + finalName + "' already exists.", "Name Conflict", JOptionPane.ERROR_MESSAGE);
                parentFrame.getStatusBar().setText("Ready");
                return;
            }

            SubWorkspace newSw = new SubWorkspace(finalName);
            File workspaceRoot = appInstance.getCurrentWorkspace();

            if (createFileBased) {
                newSw.setDirectoryBased(false);
                Set<String> uniqueAbsoluteFilePaths = new HashSet<>();
                for (String rawPath : pathsToProcess) {
                    File f = new File(rawPath); // Try as absolute first
                    if (!f.isAbsolute()) {
                        f = new File(workspaceRoot, rawPath); // Then as relative to workspace root
                    }

                    if (rawPath.endsWith("/")) { // It's a directory
                        if (f.isDirectory()) {
                            collectFilesRecursively(f, uniqueAbsoluteFilePaths);
                        } else {
                             System.err.println("LLM suggested directory path does not exist or is not a directory: " + f.getAbsolutePath());
                        }
                    } else { // It's a file
                         if (f.isFile()) {
                            uniqueAbsoluteFilePaths.add(f.getAbsolutePath());
                         } else {
                             System.err.println("LLM suggested file path does not exist or is not a file: " + f.getAbsolutePath());
                         }
                    }
                }
                newSw.setFilePaths(new ArrayList<>(uniqueAbsoluteFilePaths));
                 parentFrame.getStatusBar().setText("Created file-based sub-workspace '" + finalName + "' with " + uniqueAbsoluteFilePaths.size() + " files.");
            } else { // Directory-based
                newSw.setDirectoryBased(true);
                List<String> dirPathsToAdd = new ArrayList<>();
                for (String rawPath : pathsToProcess) {
                    if (rawPath.endsWith("/")) {
                        File f = new File(rawPath); // Try as absolute first
                        if(!f.isAbsolute()){
                           f = new File(workspaceRoot, rawPath); // Then as relative
                        }
                        if (f.isDirectory()) {
                             // Ensure mapping for directory based sub-workspaces is absolute.
                            dirPathsToAdd.add(f.getAbsolutePath());
                        } else {
                            System.err.println("LLM suggested directory path for directory-based SW is not a valid directory: " + f.getAbsolutePath());
                        }
                    }
                    // Individual files are ignored if creating a directory-based sub-workspace from LLM suggestions
                }
                if (dirPathsToAdd.isEmpty()) {
                     JOptionPane.showMessageDialog(parentFrame, "No valid directories were suggested by the LLM for a directory-based sub-workspace.", "No Directories", JOptionPane.WARNING_MESSAGE);
                     parentFrame.getStatusBar().setText("Ready");
                     return;
                }
                newSw.setMonitoredDirectoryPaths(dirPathsToAdd);
                parentFrame.getStatusBar().setText("Created directory-based sub-workspace '" + finalName + "' monitoring " + dirPathsToAdd.size() + " directories.");
            }

            appInstance.addSubWorkspace(newSw);
            // Optionally, refresh the WorkspaceMapperPanel if it's open
            // This would require a reference or a more global refresh mechanism.
            // For now, user can reopen it to see changes.
        } else {
             parentFrame.getStatusBar().setText("Sub-workspace creation cancelled.");
        }
         parentFrame.getStatusBar().setText("Ready");
    }

    private List<String> parseRelevantPaths(String llmResponse) {
        List<String> paths = new ArrayList<>();
        try {
            int startIndex = llmResponse.indexOf(RELEVANT_PATHS_START_TAG);
            int endIndex = llmResponse.indexOf(RELEVANT_PATHS_END_TAG);

            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                String pathsBlock = llmResponse.substring(startIndex + RELEVANT_PATHS_START_TAG.length(), endIndex).trim();
                String[] lines = pathsBlock.split("\\r?\\n");
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        paths.add(trimmedLine);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing relevant paths from LLM response: " + e.getMessage());
        }
        return paths;
    }

    private String parseSuggestedName(String llmResponse) {
        String name = null;
        try {
            int startIndex = llmResponse.indexOf(SUGGESTED_NAME_START_TAG);
            int endIndex = llmResponse.indexOf(SUGGESTED_NAME_END_TAG);

            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                name = llmResponse.substring(startIndex + SUGGESTED_NAME_START_TAG.length(), endIndex).trim();
            }
        } catch (Exception e) {
            System.err.println("Error parsing suggested name from LLM response: " + e.getMessage());
        }
        return name;
    }
    
    private void collectFilesRecursively(File directory, Set<String> collectedPaths) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Potentially add ignored directories logic from WorkspaceMapper here if needed
                String dirName = file.getName().toLowerCase();
                 if (dirName.equals(".git") || dirName.equals(".idea") || dirName.equals(".vscode") || dirName.equals("node_modules") || dirName.equals("target") || dirName.equals("build") || dirName.equals(".vai")) {
                    continue;
                }
                collectFilesRecursively(file, collectedPaths);
            } else if (file.isFile()) {
                if (WorkspaceMapper.hasValidExtension(file.getName())) {
                    collectedPaths.add(file.getAbsolutePath());
                }
            }
        }
    }
}