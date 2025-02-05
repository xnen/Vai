package io.improt.vai.util;

import io.improt.vai.frame.dialogs.NonParsedTextDialog;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

public class BerzfadParser {

    public static List<FileContent> parse(String response) throws Exception {
        List<FileContent> fileContents = new ArrayList<>();
        StringBuilder leftoverBuffer = new StringBuilder();

        int index = 0;
        while (index < response.length()) {
            // Look for the next '[' (file path marker)
            int startBracket = response.indexOf('[', index);
            if (startBracket == -1) {
                // Append any remaining text.
                leftoverBuffer.append(response.substring(index));
                break;
            }
            if (startBracket > index) {
                leftoverBuffer.append(response, index, startBracket);
            }

            int endBracket = response.indexOf(']', startBracket);
            if (endBracket == -1) {
                throw new Exception("Missing closing ']' for file path starting at index " + startBracket);
            }
            String filePath = response.substring(startBracket + 1, endBracket).trim();

            // Find opening triple-backticks and language identifier.
            int codeStart = response.indexOf("```", endBracket);
            if (codeStart == -1) {
                throw new Exception("Missing opening code block for file: " + filePath);
            }
            int langEnd = response.indexOf('\n', codeStart);
            if (langEnd == -1) {
                throw new Exception("Missing newline after language declaration for file: " + filePath);
            }
            String lang = response.substring(codeStart + 3, langEnd).trim();

            int codeEnd;
            // Look for a valid !EOF marker starting after the language line.
            int validEofIndex = findValidEof(response, langEnd + 1);
            if (validEofIndex != -1) {
                // Use last occurrence of triple-backticks before the valid !EOF marker.
                codeEnd = response.lastIndexOf("```", validEofIndex);
                if (codeEnd == -1 || codeEnd <= langEnd) {
                    throw new Exception("Missing closing code block for file (1): " + filePath);
                }
                index = validEofIndex + 4; // Skip past "!EOF"
            } else {
                // No valid !EOF found; use the next triple-backticks.
                codeEnd = response.indexOf("```", langEnd);
                if (codeEnd == -1) {
                    throw new Exception("Missing closing code block for file (2): " + filePath);
                }
                index = codeEnd + 3;
            }

            String codeContent = response.substring(langEnd + 1, codeEnd);
            FileContent fileContent = new FileContent(filePath, codeContent, lang);
            fileContents.add(fileContent);
        }

        // Process the leftover non-parsed text.
        String leftover = leftoverBuffer.toString();
        // Remove any content surrounded by <think> ... </think> (including the tags)
        leftover = leftover.replaceAll("(?s)<think>.*?</think>", "").trim();
        if (!leftover.isEmpty()) {
            String finalLeftover = leftover;
            SwingUtilities.invokeLater(() -> NonParsedTextDialog.showDialog(finalLeftover, null));
        }

        if (fileContents.isEmpty() && leftover.isEmpty()) {
            throw new Exception("No files found in response.");
        }

        return fileContents;
    }

    /**
     * Searches for a valid !EOF marker starting at the specified index.
     * A valid !EOF marker must appear with no non-whitespace characters
     * preceding it on its line (tabs and spaces are allowed).
     *
     * @param response the full text.
     * @param fromIndex the index from which to search.
     * @return the index of the valid !EOF marker, or -1 if not found.
     */
    private static int findValidEof(String response, int fromIndex) {
        int searchIndex = fromIndex;
        while (true) {
            int eofIndex = response.indexOf("!EOF", searchIndex);
            if (eofIndex == -1) {
                return -1;
            }
            // Find the start of the line containing !EOF.
            int lineStart = response.lastIndexOf('\n', eofIndex);
            String prefix = (lineStart == -1) ? response.substring(0, eofIndex)
                    : response.substring(lineStart + 1, eofIndex);
            if (prefix.trim().isEmpty()) {
                // This !EOF is on its own line (ignoring white space).
                return eofIndex;
            }
            // Continue searching after this occurrence.
            searchIndex = eofIndex + 4;
        }
    }

    /**
     * Simple container for file information.
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
