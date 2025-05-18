package io.improt.vai.llm;

import com.openai.models.ReasoningEffort;
import io.improt.vai.backend.App;
import io.improt.vai.llm.providers.GeminiProProvider;
import io.improt.vai.mapping.WorkspaceMapper;
import io.improt.vai.mapping.SubWorkspace;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tasks {

    public boolean queryRepositoryMap(String request, List<String> activeSubworkspaceNames) {
        App app = App.getInstance();
        if (app.getCurrentWorkspace() == null) {
            System.out.println("[Tasks::queryRepositoryMap] No current workspace. Cannot query repository map.");
            return false;
        }

        List<SubWorkspace> allSubWorkspaces = app.getSubWorkspaces();
        Set<String> uniqueFilePathsToMap = new HashSet<>();

        boolean specificSubWorkspacesSelected = activeSubworkspaceNames != null && !activeSubworkspaceNames.isEmpty();

        if (specificSubWorkspacesSelected) {
            for (SubWorkspace sw : allSubWorkspaces) {
                if (activeSubworkspaceNames.contains(sw.getName())) {
                    uniqueFilePathsToMap.addAll(sw.getFilePaths());
                }
            }
        }

        WorkspaceMapper mapper = new WorkspaceMapper(app.getCurrentWorkspace());
        String mappings;

        if (specificSubWorkspacesSelected) {
            if (!uniqueFilePathsToMap.isEmpty()) {
                mappings = mapper.getConcatenatedMappingsForPaths(new ArrayList<>(uniqueFilePathsToMap));
                System.out.println("[Tasks::queryRepositoryMap] Using mappings from " + uniqueFilePathsToMap.size() + " files in selected subworkspaces.");
            } else {
                mappings = ""; // Selected subworkspaces were empty or contained no valid files
                System.out.println("[Tasks::queryRepositoryMap] Active subworkspaces selected but they are empty or contain no files. Using empty context.");
            }
        } else {
            // No subworkspaces were selected by the user in CreatePlanDialog, fall back to all mappings
            mappings = mapper.getAllMappingsConcatenated();
            System.out.println("[Tasks::queryRepositoryMap] No active subworkspaces selected. Using all available mappings.");
        }

        String systemMessage = getSystemMessage(request);

        GeminiProProvider llmProvider = new GeminiProProvider(); // Assuming GeminiProProvider is appropriate
        ReasoningEffort originalReasoningEffort = app.getConfiguredReasoningEffort();
        app.setReasoningEffort(ReasoningEffort.HIGH); // Temporarily set high for this task
        
        String response = llmProvider.request(systemMessage, mappings, null);

        app.setReasoningEffort(originalReasoningEffort); // Restore original reasoning effort

        List<String> filePaths = getFilePaths(response);
        String addlDetails = getAdditionalDetails(response);

        if (!filePaths.isEmpty()) {
            List<String> approvedFiles = ContextApprovalDialog.showDialog(null, filePaths, addlDetails);
            if (approvedFiles == null) {
                System.out.println("[Tasks::queryRepositoryMap] Dialog returned null (cancelled or no files approved).");
                return false; // Or true depending on desired behavior for cancellation
            } else {
                System.out.println("[Tasks::queryRepositoryMap] Dynamically adding " + approvedFiles.size() + " files to context.");
                app.getActiveFileManager().setupDynamicFiles(approvedFiles);
                // If files were approved AND there were additional details, set them for the main prompt
                if (!addlDetails.isEmpty()) {
                    App.getInstance().setAdditionalData("LLM Suggestion based on plan:\n" + addlDetails);
                }
                return true;
            }
        } else { 
            System.out.println("[Tasks::queryRepositoryMap] LLM did not suggest any files or parsing failed. Additional Details (if any): " + addlDetails);
            // If no files suggested, but there are additional details, this might be the LLM's direct response to the plan.
            if (!addlDetails.isEmpty()) {
                 App.getInstance().setAdditionalData("LLM Response to plan (no files suggested for context):\n" + addlDetails);
            }
            return true; // Return true as the process completed, even if no files were added to context.
        }
    }

    @NotNull
    private static List<String> getFilePaths(String response) {
        List<String> filePaths = new ArrayList<>();
        if (response != null && !response.isEmpty()) {
            // More robust parsing for ``` ``` blocks, potentially with language specifier
            int firstBlockStart = response.indexOf("```");
            if (firstBlockStart == -1) {
                System.out.println("[Tasks::getFilePaths] No Markdown code block start found for file list.");
                return filePaths; // No code block found
            }

            // Skip potential language specifier line after ```
            int contentStart = response.indexOf('\n', firstBlockStart);
            if (contentStart == -1 || contentStart > response.indexOf("```", firstBlockStart + 3) ) { // No newline before next ```
                contentStart = firstBlockStart + 3; // Assume content starts immediately after ```
            } else {
                 contentStart = contentStart + 1; // Start after the newline
            }


            int firstBlockEnd = response.indexOf("```", contentStart);
            if (firstBlockEnd == -1) {
                System.out.println("[Tasks::getFilePaths] No Markdown code block end found for file list.");
                return filePaths; // No end to the code block
            }

            String codeBlockContent = response.substring(contentStart, firstBlockEnd).trim();
            String[] lines = codeBlockContent.split("\\r?\\n");
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#") && !trimmedLine.startsWith("//")) { // Ignore empty lines and comments
                    filePaths.add(trimmedLine);
                }
            }
        }
        System.out.println("[Tasks::getFilePaths] Extracted " + filePaths.size() + " file paths from LLM response.");
        return filePaths;
    }

    @NotNull
    private static String getAdditionalDetails(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }
        // Find the first code block ```...```
        int firstBlockStart = response.indexOf("```");
        if (firstBlockStart == -1) {
            return response.trim(); // No code block, entire response is additional details
        }
        int contentStart = response.indexOf('\n', firstBlockStart);
         if (contentStart == -1 || contentStart > response.indexOf("```", firstBlockStart + 3) ) {
            contentStart = firstBlockStart + 3;
        } else {
            contentStart = contentStart + 1;
        }
        int firstBlockEnd = response.indexOf("```", contentStart);
        if (firstBlockEnd == -1) {
             // Malformed, assume text after start of block is details if block doesn't close properly
            return response.substring(contentStart).trim();
        }

        // Text before the first code block and text after the first code block
        String textBefore = response.substring(0, firstBlockStart).trim();
        String textAfter = response.substring(firstBlockEnd + 3).trim();
        
        StringBuilder details = new StringBuilder();
        if (!textBefore.isEmpty()) {
            details.append(textBefore);
        }
        if (!textAfter.isEmpty()) {
            if (details.length() > 0) {
                details.append("\n\n"); // Add separator if both parts exist
            }
            details.append(textAfter);
        }
        return details.toString().trim();
    }

    @NotNull
    private static String getSystemMessage(String request) {
        String systemMessage = "**Goal:**\n" +
                "Your name is TOM. Output a list of file paths necessary to complete a specific request.\n" +
                "\n" +
                "### **Instructions:**\n" +
                "1. The user will send a comprehensive document that overviews every file in the user's repository, including file paths, classes, methods, and fields. This is the 'MAPPINGS' content.\n" +
                "2. Analyze this MAPPINGS content to determine which files are relevant to the given 'REQUEST'. THINK: Interfaces, Classes, etc. Anything the developer may need to reference in order to write good code around it.  \n" +
                "3. Provide the list of these relevant file paths as newline-separated entries. Wrap this list in a Markdown code block (```). Only include file paths in this block.\n" +
                "4. After the Markdown code block containing file paths, write a suggestion to the developer about how to achieve the given 'REQUEST', using the MAPPINGS content as your knowledge base. This suggestion is for the developer's eyes. Do NOT use more Markdown code blocks for this suggestion part.\n" +
                "\n" +
                "The developer cannot see anything outside the files and suggestions you provide them. They must rely entirely on your given information to complete the task. Ensure they have enough context from the MAPPINGS to complete the given task.\n" +
                "!! CRITICAL !! :: You must provide ALL suggested file paths initially within the single Markdown code block, then write all additional instructions/suggestions afterward. Do not intersperse files and instructions, or use multiple code blocks for files.\n " +
                "\n" +
                "**Format Example:**\n" +
                "```\n" +
                "src/com/example/pkgOne/Main.java\n" +
                "src/com/example/pkgTwo/other/OtherClass.java\n" +
                "```\n" +
                "To implement the feature, you should modify `Main.java` to call the `process()` method in `OtherClass.java`. You might need to instantiate `OtherClass` first if it's not static.\n" +
                "\n\n" +
                "----------------------\n" +
                "REQUEST: <request>";

        systemMessage = systemMessage.replace("<request>", request);
        return systemMessage;
    }

}
