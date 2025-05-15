package io.improt.vai.llm;

import com.openai.models.ReasoningEffort;
import io.improt.vai.backend.App;
import io.improt.vai.llm.providers.GeminiProProvider;
import io.improt.vai.llm.providers.O3MiniProvider;
import io.improt.vai.llm.providers.O4MiniProvider;
import io.improt.vai.mapping.WorkspaceMapper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Tasks {

    public boolean queryRepositoryMap(String request) {
        WorkspaceMapper mapper = new WorkspaceMapper(App.getInstance().getCurrentWorkspace());
        String mappings = mapper.getAllMappingsConcatenated();
        String systemMessage = getSystemMessage(request);

        GeminiProProvider o3MiniProvider = new GeminiProProvider();
        ReasoningEffort configuredReasoningEffort = App.getInstance().getConfiguredReasoningEffort();
        App.getInstance().setReasoningEffort(ReasoningEffort.HIGH);
        String response = o3MiniProvider.request(systemMessage, mappings, null);

        // restore reasoning effort.
        App.getInstance().setReasoningEffort(configuredReasoningEffort);

        // Attempt to parse the response as a Markdown code block containing file paths
        List<String> filePaths = getFilePaths(response);
        String addlDetails = getAdditionalDetails(response);

        // If file paths were extracted, show the approval dialog
        if (!filePaths.isEmpty()) {
            List<String> approvedFiles = ContextApprovalDialog.showDialog(null, filePaths, addlDetails);
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
            if (firstBacktick == -1) {
                System.out.println("error, no md block");
                return filePaths;
            }

            String afterFirst = response.substring(firstBacktick + 3);
            int secondBackTick = afterFirst.indexOf("```");
            if (secondBackTick == -1) {
                System.out.println("error2, no md block end.");
                return filePaths;
            }

            String codeBlock = afterFirst.substring(0, secondBackTick).trim();
            String[] lines = codeBlock.split("\\r?\\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    filePaths.add(line.trim());
                }
            }
        }

        System.out.println(response);
        System.out.println(filePaths.size());

        return filePaths;
    }

    @NotNull
    private static String getAdditionalDetails(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }
        int firstBacktick = response.indexOf("```");
        if (firstBacktick == -1) {
            System.out.println("err, no md block");
            return "";
        }
        String afterFirst = response.substring(firstBacktick + 3);
        int secondBacktick = afterFirst.indexOf("```");
        if (secondBacktick == -1) {
            System.out.println("err, no md block end.");
            return "";
        }

        // Text before the code block and after the code block are considered additional details
        String afterCode = afterFirst.substring(secondBacktick + 3);
        return (response.substring(0, firstBacktick) + afterCode).trim();
    }

    @NotNull
    private static String getSystemMessage(String request) {
        String systemMessage = "**Goal:**\n" +
                "Your name is TOM. Output a list of file paths necessary to complete a specific request.\n" +
                "\n" +
                "### **Instructions:**\n" +
                "1. The user will send a comprehensive document that overviews every file in the user's repository, including file paths, classes, methods, and fields. \n" +
                "2. Analyze this document to determine which files are relevant to the given request. THINK: Interfaces, Classes, etc. Anything the developer may need to reference in order to write good code around it.  \n" +
                "3. Provide the list of these file paths as newline-separated entries, and wrap the list in a Markdown code block.\n\n" +
                "Anything written after the Markdown code block will be provided as additional details for the developer. Using the comprehensive document, write a " +
                "suggestion to the developer about how to achieve the given request. ONLY USE MARKDOWN BLOCKS FOR THE FILE LIST. Do not use more than one markdown block in your response. The developer is unable to do anything but edit and create files.\n" +
                "\n" +
                "The developer cannot see anything outside the files and suggestions you provide them. They must rely entirely on your given information to complete the task. Ensure they have enough context to complete the given task.\n" +
                "!! CRITICAL !! :: You must provide ALL files initially within the markdown block, then write all additional instructions afterward. Don't sparsely write some files, and then instructions, and then more files afterward.\n " +
                "\n" +
                "**Format:**\n" +
                "```\n" +
                "src/com/example/pkgOne/Main.java\n" +
                "src/com/example/pkgTwo/other/OtherClass.java\n" +
                "```\n" +
                "<write any additional info for the developer here. Do NOT use your own code blocks here.>\n" +
                "\n\n" +
                "----------------------\n" +
                "Request: <request>";

        systemMessage = systemMessage.replace("<request>", request);
        return systemMessage;
    }

}
