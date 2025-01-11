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

            // Look for closing triple backticks
            int codeEnd = response.indexOf("```\n", langEnd);
            if (codeEnd == -1) {
                codeEnd = response.indexOf("```", langEnd);
                if (codeEnd == -1) {
                    throw new Exception("Missing code block end.");
                }
            }

            // Extract code content
            String codeContent = response.substring(langEnd + 1, codeEnd);

            // Look for !EOF after code block
            int eofIndex = response.indexOf("!EOF", codeEnd);
            if (eofIndex == -1) {
                // EOF not found, continue parsing
                index = codeEnd + 3;
            } else {
                // EOF found, move index past it
                index = eofIndex + 4;
            }

            FileContent fileContent = new FileContent(filePath, codeContent);
            fileContents.add(fileContent);
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

        public FileContent(String fileName, String newContents) {
            this.fileName = fileName;
            this.newContents = newContents;
        }

        public String getFileName() {
            return fileName;
        }

        public String getNewContents() {
            return newContents;
        }
    }
}
  