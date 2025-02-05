package io.improt.vai.llm;

import com.openai.models.ChatCompletionReasoningEffort;
import io.improt.vai.backend.App;
import io.improt.vai.llm.providers.O3MiniProvider;
import io.improt.vai.mapping.WorkspaceMapper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Tasks {

    public boolean queryRepositoryMap(String request) {
        WorkspaceMapper mapper = new WorkspaceMapper();
        String mappings = mapper.getAllMappingsConcatenated();

        String systemMessage = getSystemMessage(request);

        O3MiniProvider o3MiniProvider = new O3MiniProvider();
        ChatCompletionReasoningEffort configuredReasoningEffort = App.getInstance().getConfiguredReasoningEffort();
        App.getInstance().setReasoningEffort(ChatCompletionReasoningEffort.MEDIUM);
        String response = o3MiniProvider.request(systemMessage, mappings, null);
        // restore reasoning effort.
        App.getInstance().setReasoningEffort(configuredReasoningEffort);

        // Attempt to parse the response as a Markdown code block containing file paths
        List<String> filePaths = getFilePaths(response);

        // If file paths were extracted, show the approval dialog
        if (!filePaths.isEmpty()) {
            List<String> approvedFiles = ContextApprovalDialog.showDialog(null, filePaths);
            if (approvedFiles == null) {
                System.out.println("[Tasks::queryRepositoryMap] Dialog returned null. Assuming no files to add.");
                return false;
            } else {
                System.out.println("[Tasks::queryRepositoryMap] Dynamically adding " + approvedFiles.size() + " files to context.");
                App.getInstance().getActiveFileManager().setupDynamicFiles(approvedFiles);
                return true;
            }
        } else {
            return true;
        }
    }

    @NotNull
    private static List<String> getFilePaths(String response) {
        List<String> filePaths = new ArrayList<>();
        if (response != null && !response.isEmpty()) {
            int firstBacktick = response.indexOf("```");
            int lastBacktick = response.lastIndexOf("```");
            if (firstBacktick != -1 && lastBacktick != -1 && firstBacktick != lastBacktick) {
                String codeBlock = response.substring(firstBacktick + 3, lastBacktick).trim();
                String[] lines = codeBlock.split("\\r?\\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        filePaths.add(line.trim());
                    }
                }
            }
        }
        return filePaths;
    }

    @NotNull
    private static String getSystemMessage(String request) {
        String systemMessage = "**Goal:**\n" +
                "Output a list of file paths necessary to complete a specific request.\n" +
                "\n" +
                "**Instructions:**\n" +
                "1. The user will send a comprehensive document that overviews every file in the user's repository, including file paths, classes, methods, and fields. \n" +
                "2. Analyze this document to determine which files are relevant to the given request.  \n" +
                "3. Provide the list of these file paths as newline-separated entries, and wrap the list in a Markdown code block.\n" +
                "\n" +
                "**Example Format:**\n" +
                "```\n" +
                "src/com/example/pkgOne/Main.java\n" +
                "src/com/example/pkgTwo/other/OtherClass.java\n" +
                "```\n" +
                "\n" +
                "Do not write anything else in your response.\n" +
                "Request: <request>";

        systemMessage = systemMessage.replace("<request>", request);
        return systemMessage;
    }

}
