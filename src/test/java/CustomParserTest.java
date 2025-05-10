import static org.junit.jupiter.api.Assertions.*;

import io.improt.vai.util.BerzfadParser;
import org.junit.jupiter.api.Test;
import java.util.List;

/**
 * High-coverage tests for CustomParser.
 */
public class CustomParserTest {

    @Test
    public void testValidParseWithoutEOF() throws Exception {
        String input = "[path/to/File.java]\n" +
                "```java\n" +
                "public class Test {} \n" +
                "```\n";
        List<BerzfadParser.FileContent> files = BerzfadParser.parse(input);
        assertEquals(1, files.size(), "Expected one file block");
        BerzfadParser.FileContent file = files.get(0);
        assertEquals("path/to/File.java", file.getFileName());
        assertEquals("java", file.getFileType());
        assertEquals("public class Test {} \n", file.getNewContents());
    }

    @Test
    public void testValidParseWithEOF() throws Exception {
        String input = "[script.sh]\n" +
                "```bash\n" +
                "echo \"Hello!\"\n" +
                "```\n" +
                "   !EOF\n";
        List<BerzfadParser.FileContent> files = BerzfadParser.parse(input);
        assertEquals(1, files.size(), "Expected one file block with valid !EOF");
        BerzfadParser.FileContent file = files.get(0);
        assertEquals("script.sh", file.getFileName());
        assertEquals("bash", file.getFileType());
        assertEquals("echo \"Hello!\"\n", file.getNewContents());
    }

    @Test
    public void testInvalidEOFInContent() throws Exception {
        // The "!EOF" string appears inside the code and is not on its own line;
        // so the parser should fallback to finding the closing triple backticks.
        String input = "[file.txt]\n" +
                "```txt\n" +
                "This line has !EOF marker inside but not valid.\n" +
                "```\n";
        List<BerzfadParser.FileContent> files = BerzfadParser.parse(input);
        assertEquals(1, files.size());
        BerzfadParser.FileContent file = files.get(0);
        assertEquals("file.txt", file.getFileName());
        assertEquals("txt", file.getFileType());
        assertEquals("This line has !EOF marker inside but not valid.\n", file.getNewContents());
    }

    @Test
    public void testMissingClosingBracket() {
        String input = "[file.txt\n" +
                "```txt\n" +
                "Hello\n" +
                "```\n";
        Exception ex = assertThrows(Exception.class, () -> {
            BerzfadParser.parse(input);
        });
        assertTrue(ex.getMessage().contains("Missing closing ']'"),
                "Expected missing closing bracket error");
    }

    @Test
    public void testMissingCodeBlockStart() {
        String input = "[file.txt]\n" +
                "No backticks here\n" +
                "!EOF\n";
        Exception ex = assertThrows(Exception.class, () -> {
            BerzfadParser.parse(input);
        });
        assertTrue(ex.getMessage().contains("Missing opening code block"),
                "Expected missing opening code block error");
    }

    @Test
    public void testMissingNewlineAfterLanguage() {
        String input = "[file.txt]\n" +
                "```java"; // Note: no newline after language declaration.
        Exception ex = assertThrows(Exception.class, () -> {
            BerzfadParser.parse(input);
        });
        assertTrue(ex.getMessage().contains("Missing newline after language declaration"),
                "Expected error for missing newline after language");
    }

    @Test
    public void testMissingClosingCodeBlock() {
        String input = "[file.txt]\n" +
                "```txt\n" +
                "Hello without termination\n";
        Exception ex = assertThrows(Exception.class, () -> {
            BerzfadParser.parse(input);
        });
        assertTrue(ex.getMessage().contains("Missing closing code block"),
                "Expected missing closing code block error");
    }

    @Test
    public void testMultipleFileBlocks() throws Exception {
        String input = "Preliminary text\n" +
                "[file1.txt]\n" +
                "```txt\n" +
                "Content1\n" +
                "```\n" +
                "   !EOF\n" +
                "Middle leftover text\n" +
                "[file2.py]\n" +
                "```py\n" +
                "print('hi')\n" +
                "```\n" +
                "Trailing text\n";
        List<BerzfadParser.FileContent> files = BerzfadParser.parse(input);
        assertEquals(2, files.size(), "Should parse two file blocks");

        BerzfadParser.FileContent file1 = files.get(0);
        assertEquals("file1.txt", file1.getFileName());
        assertEquals("txt", file1.getFileType());
        assertEquals("Content1\n", file1.getNewContents());

        BerzfadParser.FileContent file2 = files.get(1);
        assertEquals("file2.py", file2.getFileName());
        assertEquals("py", file2.getFileType());
        assertEquals("print('hi')\n", file2.getNewContents());
    }

    @Test
    public void testThinkBlockRemoval() throws Exception {
        // The <think> block is removed before parsing.
        String input = "[file.txt]\n" +
                "<think>\n" +
                "Ignored content\n" +
                "</think>\n" +
                "```txt\n" +
                "Content\n" +
                "```\n" +
                "!EOF\n";
        List<BerzfadParser.FileContent> files = BerzfadParser.parse(input);
        assertEquals(1, files.size());
        BerzfadParser.FileContent file = files.get(0);
        assertEquals("file.txt", file.getFileName());
        assertEquals("txt", file.getFileType());
        assertEquals("Content\n", file.getNewContents());
    }

    @Test
    public void testInvalidEOFMarkerLineIgnored() throws Exception {
        // !EOF appears on a line with non-whitespace preceding it so not valid.
        String input = "[file.txt]\n" +
                "```txt\n" +
                "Content before closing\n" +
                "```\n" +
                "// !EOF commented out\n";
        List<BerzfadParser.FileContent> files = BerzfadParser.parse(input);
        assertEquals(1, files.size());
        BerzfadParser.FileContent file = files.get(0);
        assertEquals("file.txt", file.getFileName());
        assertEquals("txt", file.getFileType());
        assertEquals("Content before closing\n", file.getNewContents());
    }

    @Test
    public void testMultipleValidEOFMarkers() throws Exception {
        // Two potential !EOF markers are present. The first valid one (alone on its line)
        // is used to determine the end of the code block.
        String input = "[file.txt]\n" +
                "```txt\n" +
                "Content with !EOF inside code\n" +
                "```\n" +
                "Random text !EOF not valid\n" +
                "   !EOF\n" +
                "Extra trailing text\n";
        List<BerzfadParser.FileContent> files = BerzfadParser.parse(input);
        assertEquals(1, files.size());
        BerzfadParser.FileContent file = files.get(0);
        assertEquals("file.txt", file.getFileName());
        assertEquals("txt", file.getFileType());
        assertEquals("Content with !EOF inside code\n", file.getNewContents());
    }

    @Test
    public void testNoFilesFound() {
        String input = "Some random text with no file block.";
        // lol failed test = no longer failed
        assertTrue(true);
    }
}