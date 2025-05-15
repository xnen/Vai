package io.improt.vai.util;

import io.improt.vai.frame.dialogs.NonParsedTextDialog;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BerzfadParser {

    // Pattern to check if a string contains only whitespace. Matches empty string as well.
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s*");

    /**
     * Parses a string potentially containing Berzfad formatted file blocks.
     * Valid blocks follow the pattern:
     * [filepath]
     * ```lang
     * content...
     * ```
     * !EOF
     *
     * Stricter parsing rules:
     * - '[' must be at the start of a line.
     * - ']' must be immediately followed by '\n```'.
     * - Code block must end with '\n```'.
     * - '!EOF' must follow the closing '\n```' on its own line (allowing preceding whitespace).
     * - Text not matching this pattern is collected as leftover text.
     *
     * @param response The input string from the LLM.
     * @return A list of parsed FileContent objects.
     * @throws Exception If the response contained text but nothing could be parsed (neither files nor leftover).
     */
    public static List<FileContent> parse(String response) throws Exception {
        List<FileContent> fileContents = new ArrayList<>();
        StringBuilder leftoverBuffer = new StringBuilder();
        int index = 0; // Current position in the response string

        while (index < response.length()) {
            // 1. Find the next potential '[' marker at the beginning of a line
            int startBracket = -1;
            int searchFrom = index;
            while (true) {
                int potentialBracket = response.indexOf('[', searchFrom);
                if (potentialBracket == -1) {
                    // No more '[' found in the remaining response
                    startBracket = -1;
                    break;
                }
                // Check if it's at the start of the string or preceded by a newline
                if (potentialBracket == 0 || response.charAt(potentialBracket - 1) == '\n') {
                    // Found a potential starting bracket at the beginning of a line
                    startBracket = potentialBracket;
                    break;
                } else {
                    // This '[' is not at the start of a line. Skip it and continue searching.
                    searchFrom = potentialBracket + 1;
                }
            }

            // Append any text between the last position (index) and the found valid bracket start
            if (startBracket != -1 && startBracket > index) {
                leftoverBuffer.append(response, index, startBracket);
            } else if (startBracket == -1) {
                // No more valid file markers found, append the rest of the response to leftover
                leftoverBuffer.append(response.substring(index));
                index = response.length(); // Move index to end
                break; // Exit the main loop
            }

            // We have a potential start bracket at `startBracket`
            index = startBracket; // Update main index to the start of the potential block

            // 2. Find the corresponding closing ']'
            int endBracket = response.indexOf(']', startBracket + 1);
            if (endBracket == -1) {
                // Found '[' at start of line, but no ']' -> Treat '[' as text and continue searching after it.
                leftoverBuffer.append(response.charAt(startBracket));
                index = startBracket + 1;
                continue; // Continue main loop to search for the next valid '['
            }

            // 3. Validate the sequence immediately following ']': must be exactly "\n```"
            int codeBlockMarkerStart = endBracket + 1;
            String requiredSequence = "\n```";
            if (codeBlockMarkerStart + requiredSequence.length() > response.length() ||
                !response.substring(codeBlockMarkerStart, codeBlockMarkerStart + requiredSequence.length()).equals(requiredSequence)) {
                // Sequence is invalid or goes beyond response length. Treat '[...]' as text and continue search after ']'.
                leftoverBuffer.append(response, startBracket, endBracket + 1);
                index = endBracket + 1;
                continue; // Continue main loop to search for the next valid '['
            }

            // 4. Valid block header found: [filepath]\n```
            // Extract file path
            String filePath = response.substring(startBracket + 1, endBracket).trim();
            if (filePath.isEmpty()) {
                // Invalid block: Empty filename. Treat as text.
                leftoverBuffer.append(response, startBracket, codeBlockMarkerStart + requiredSequence.length());
                index = codeBlockMarkerStart + requiredSequence.length();
                continue;
            }


            // Find the end of the language identifier line (newline after ```lang)
            int langDeclStart = codeBlockMarkerStart + requiredSequence.length();
            int langEnd = response.indexOf('\n', langDeclStart);
            if (langEnd == -1) {
                // Malformed block: Missing newline after language declaration. Treat block header as text.
                leftoverBuffer.append(response, startBracket, langDeclStart);
                index = langDeclStart;
                continue;
            }

            // Extract language
            String lang = response.substring(langDeclStart, langEnd).trim();
            // Note: Language can technically be empty according to markdown spec.

            // 5. Find the end of the code block using a valid "!EOF" marker
            int codeContentStart = langEnd + 1;
            int validEofIndex = findValidEof(response, codeContentStart);

            if (validEofIndex == -1) {
                // Block is invalid because mandatory !EOF is missing. Treat header and potential content start as text.
                leftoverBuffer.append(response, startBracket, codeContentStart);
                index = codeContentStart;
                continue; // Look for next potential block
            }

            // 6. Find the closing code marker "\n```" which must precede the !EOF marker's line
            // Search backwards from the beginning of the !EOF line
            int eofLineStart = response.lastIndexOf('\n', validEofIndex - 1) + 1; // Find start of !EOF line (or 0)
            int closingCodeMarkerIndex = response.lastIndexOf("\n```", eofLineStart - 1); // Search for \n``` before the start of the !EOF line

            if (closingCodeMarkerIndex == -1 || closingCodeMarkerIndex < langEnd) {
                 // Malformed block: Closing "\n```" not found between lang line and !EOF line. Treat header as text.
                 leftoverBuffer.append(response, startBracket, codeContentStart);
                 index = codeContentStart;
                 continue;
            }

            // 7. Validate that only whitespace exists between the closing "\n```" and the start of the "!EOF" line
            int contentEnd = closingCodeMarkerIndex; // Code content ends *before* the final \n```
            int closingMarkerEnd = closingCodeMarkerIndex + "\n```".length();
            String betweenCodeAndEofLine = response.substring(closingMarkerEnd, eofLineStart);
            if (!WHITESPACE_PATTERN.matcher(betweenCodeAndEofLine).matches()) {
                 // Malformed block: Non-whitespace found between closing ``` and the line where !EOF starts. Treat header as text.
                 leftoverBuffer.append(response, startBracket, codeContentStart);
                 index = codeContentStart;
                 continue;
            }

            // 8. Extract code content
            String codeContent = response.substring(codeContentStart, contentEnd);

            // Successfully parsed a block
            System.out.println("BerzfadParser: Successfully parsed block for file: " + filePath);
            FileContent fileContent = new FileContent(filePath, codeContent, lang);
            fileContents.add(fileContent);

            // Update index to position after the full "!EOF" marker for the next search iteration
            index = validEofIndex + 4;

        } // End while loop scanning through response

        // Process the accumulated leftover non-parsed text.
        String leftover = leftoverBuffer.toString();
        // Remove any diagnostic <think>...</think> blocks
        leftover = leftover.replaceAll("(?s)<think>.*?</think>", "").trim();
        if (!leftover.isEmpty()) {
            System.out.println("BerzfadParser: Found leftover text: \"" + (leftover.length() > 50 ? leftover.substring(0, 50) + "..." : leftover) + "\"");
            String finalLeftover = leftover;
            // Ensure Swing dialog is shown on the Event Dispatch Thread
            SwingUtilities.invokeLater(() -> NonParsedTextDialog.showDialog(finalLeftover, null));
        }

        // Check if parsing yielded anything meaningful *if* the input response was not empty/whitespace.
        if (fileContents.isEmpty() && leftover.isEmpty() && !response.trim().isEmpty()) {
            throw new Exception("Response contained text but no valid Berzfad blocks or leftover text could be parsed.");
        }
        // If the original response was empty or only whitespace, returning an empty list is expected and correct.

        return fileContents;
    }

    /**
     * Searches for a valid !EOF marker starting at or after the specified index.
     * A valid !EOF marker must be exactly "!EOF" and appear with no non-whitespace
     * characters preceding it on its line (leading tabs and spaces are allowed).
     *
     * @param text The full text to search within.
     * @param fromIndex The index from which to start searching.
     * @return The starting index of the valid "!EOF" marker, or -1 if not found.
     */
    private static int findValidEof(String text, int fromIndex) {
        int searchIndex = fromIndex;
        while (true) {
            int eofIndex = text.indexOf("!EOF", searchIndex);
            if (eofIndex == -1) {
                return -1; // "!EOF" not found in the remainder of the text
            }

            // Check if "!EOF" is followed by something other than newline or EOF, which would make it invalid.
            // (e.g., "!EOFabc"). Allow !EOF at the very end of the string.
            if (eofIndex + 4 < text.length() && text.charAt(eofIndex + 4) != '\n') {
                 // It's part of a larger word, not the marker. Continue search.
                 searchIndex = eofIndex + 1; // Start search after the '!'
                 continue;
            }


            // Find the start of the line containing this "!EOF".
            int lineStart = text.lastIndexOf('\n', eofIndex - 1);
            // Adjust lineStart to be the index *after* the newline, or 0 if it's the first line.
            lineStart = (lineStart == -1) ? 0 : lineStart + 1;

            // Check if the prefix on the line (from lineStart up to eofIndex) contains only whitespace.
            String prefix = text.substring(lineStart, eofIndex);
            if (WHITESPACE_PATTERN.matcher(prefix).matches()) {
                // Valid !EOF found: it starts its line (ignoring whitespace) and isn't part of another word.
                return eofIndex;
            }

            // This "!EOF" occurrence is not correctly positioned (has preceding non-whitespace on its line).
            // Continue searching from the position right after this occurrence.
            searchIndex = eofIndex + 4; // Start search after current "!EOF"
        }
    }

    /**
     * Simple container for parsed file information.
     */
    public static class FileContent {
        private final String fileName;
        private final String newContents;
        private final String fileType;

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

        @Override
        public String toString() {
            String preview = newContents.replace("\n", "\\n");
            if (preview.length() > 20) {
                preview = preview.substring(0, 19) + "...";
            }
            return "FileContent{fileName='" + fileName + '\'' + ", fileType='" + fileType + '\'' + ", preview='" + preview + '\'' + '}';
        }
    }
}
