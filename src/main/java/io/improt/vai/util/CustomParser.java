package io.improt.vai.util;

import java.util.ArrayList;
import java.util.List;

public class CustomParser {

    public static List<FileContent> parse(String response) throws Exception {
        List<FileContent> fileContents = new ArrayList<>();
        StringBuilder leftoverBuffer = new StringBuilder();

        // First, strip out <think>...</think> blocks.
        int thinkStart = response.indexOf("<think>");
        while (thinkStart != -1) {
            int thinkEnd = response.indexOf("</think>", thinkStart);
            if (thinkEnd != -1) {
                response = response.substring(0, thinkStart) + response.substring(thinkEnd + "</think>".length());
            } else {
                // If closing tag is missing, remove everything from <think> to end.
                response = response.substring(0, thinkStart);
                break;
            }
            thinkStart = response.indexOf("<think>");
        }

        int index = 0;
        while (index < response.length()) {
            // Look for the next file path start.
            int startBracket = response.indexOf('[', index);
            if (startBracket == -1) {
                // Append any remaining text to leftoverBuffer.
                leftoverBuffer.append(response.substring(index));
                break;
            }
            // Accumulate any text before the file path marker.
            if (startBracket > index) {
                leftoverBuffer.append(response.substring(index, startBracket));
            }

            int endBracket = response.indexOf(']', startBracket);
            if (endBracket == -1) {
                throw new Exception("Missing closing ']' for file path starting at index " + startBracket);
            }
            String filePath = response.substring(startBracket + 1, endBracket).trim();

            // Find the next triple-backticks for code block start.
            int codeStart = response.indexOf("```", endBracket);
            if (codeStart == -1) {
                throw new Exception("Missing opening code block for file: " + filePath);
            }
            int langEnd = response.indexOf('\n', codeStart);
            if (langEnd == -1) {
                throw new Exception("Missing newline after language declaration for file: " + filePath);
            }
            String lang = response.substring(codeStart + 3, langEnd).trim();

            // Now determine if we have an !EOF marker.
            int eofIndex = response.indexOf("!EOF", langEnd);
            int codeEnd;
            if (eofIndex != -1) {
                // Use lastIndexOf to account for triple backticks within the content.
                codeEnd = response.lastIndexOf("```", eofIndex);
                if (codeEnd == -1 || codeEnd <= langEnd) {
                    throw new Exception("Missing closing code block for file: " + filePath);
                }
                index = eofIndex + "!EOF".length();
            } else {
                // No !EOF marker, so use the next triple-backticks
                codeEnd = response.indexOf("```", langEnd);
                if (codeEnd == -1) {
                    throw new Exception("Missing closing code block for file: " + filePath);
                }
                index = codeEnd + 3;
            }

            String codeContent = response.substring(langEnd + 1, codeEnd);
            FileContent fileContent = new FileContent(filePath, codeContent, lang);
            fileContents.add(fileContent);
        }

        // At this point, leftoverBuffer contains any text not part of a file block.
        // You can handle it as needed (for example, by attaching it to a result object or processing later).
        // For now, we only return fileContents.

        if (fileContents.isEmpty()) {
            throw new Exception("No files found in response.");
        }

        System.out.println("Left over (" + leftoverBuffer.length() + "):");
        System.out.println(leftoverBuffer);

        return fileContents;
    }

    /**
     * Simple class to hold file name, contents and file type.
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

        public String getFileName() {
            return fileName;
        }
        public String getNewContents() {
            return newContents;
        }
        public String getFileType() {
            return fileType;
        }
    }
}