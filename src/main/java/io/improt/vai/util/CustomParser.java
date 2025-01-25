package io.improt.vai.util;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomParser is designed to parse LLM responses that follow a specific custom format.
 * <p>
 * The expected format is:
 * <pre>
 * [Full Filename Path]
 * ```&lt;lang&gt;
 * &lt;file contents&gt;
 * ```
 * !EOF
 * </pre>
 * The parser reads the response string and extracts file paths and their corresponding contents.
 * It handles multiple such blocks in a single response.
 * <p>
 */
public class CustomParser {

    /**
     * Parses the LLM response and extracts file paths and contents.
     *
     * @param response The LLM response string.
     * @return A list of FileContent objects containing the fileName and newContents, or null if parsing fails.
     */
    public static List<FileContent> parse(String response) throws Exception {
        List<FileContent> fileContents = new ArrayList<>();

        // Remove <think>...</think> blocks of DeepSeek and similar models.
        // TODO: This is somewhat naive, but it works for now.
        int thinkStart = response.indexOf("<think>");
        while (thinkStart != -1) {
            int thinkEnd = response.indexOf("</think>", thinkStart);
            if (thinkEnd != -1) {
                response = response.substring(0, thinkStart) + response.substring(thinkEnd + "</think>".length());
            } else {
                // Handle case where </think> is missing (maybe log a warning or throw an exception)
                // For now, just remove from <think> to the end of the string.
                response = response.substring(0, thinkStart);
                break; // Exit loop as the rest of the string is removed.
            }
            thinkStart = response.indexOf("<think>"); // Look for next occurrence
        }


        int index = 0;
        while (index < response.length()) {
            // Look for the next '['
            int startBracket = response.indexOf('[', index);
            if (startBracket == -1) {
                break;
            }

            int endBracket = response.indexOf(']', startBracket);
            if (endBracket == -1) {
                throw new Exception("Missing closing bracket for file path.");
            }

            String filePath = response.substring(startBracket + 1, endBracket).trim();

            // Look for triple backticks
            int codeStart = response.indexOf("```", endBracket);
            if (codeStart == -1) {
                throw new Exception("Missing code block start.");
            }

            // Get language identifier
            int langEnd = response.indexOf('\n', codeStart);
            if (langEnd == -1) {
                throw new Exception("Missing code block language end.");
            }

            String lang = response.substring(codeStart + 3, langEnd).trim();

            // Determine where to search for the closing triple backticks
            int eofIndex = response.indexOf("!EOF", langEnd);
            int codeEnd;
            if (eofIndex != -1) {
                // Find the last occurrence of triple backticks before !EOF
                codeEnd = response.indexOf("```", eofIndex - 6); // 6 is generous. Can be adjusted if needed.
                if (codeEnd == -1) {
                    throw new Exception("Missing closing code block before !EOF.");
                }
                // Extract code content
                String codeContent = response.substring(langEnd + 1, codeEnd);

                // Look for !EOF after code block
                index = eofIndex + 4;
                FileContent fileContent = new FileContent(filePath, codeContent, lang);
                fileContents.add(fileContent);
            } else {
                // If !EOF is not found, find the next closing triple backticks
                codeEnd = response.indexOf("```", langEnd);
                if (codeEnd == -1) {
                    throw new Exception("Missing code block end.");
                }
                // Extract code content
                String codeContent = response.substring(langEnd + 1, codeEnd);

                // Continue parsing after the closing backticks
                index = codeEnd + 3;

                FileContent fileContent = new FileContent(filePath, codeContent, lang);
                fileContents.add(fileContent);
            }
        }

        if (fileContents.isEmpty()) {
            throw new Exception("No files found in response.");
        }

        return fileContents;
    }

    /**
     * Simple class to hold file name and contents
     */
    public static class FileContent {
        private String fileName;
        private String newContents;
        private String fileType;

        public FileContent(String fileName, String newContents, String fileType) {
            this.fileName = fileName;
            this.newContents = newContents;
            this.fileType = fileType;
        }

        public String getFileType() {
            return fileType;
        }

        public String getFileName() {
            return fileName;
        }

        public String getNewContents() {
            return newContents;
        }
    }
}
