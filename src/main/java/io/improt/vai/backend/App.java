package io.improt.vai.backend;

import io.improt.vai.frame.Client;
import io.improt.vai.openai.OpenAIProvider;
import io.improt.vai.util.FileTreeBuilder;
import io.improt.vai.util.FileUtils;
import io.improt.vai.util.Constants;

import io.improt.vai.util.MeldLauncher;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class App {

    public static final String API_KEY = Constants.API_KEY_PATH;
    private File currentWorkspace;
    private final List<File> enabledFiles = new ArrayList<>();

    private final Set<String> ignoreList = new HashSet<>();

    private static App instance;
    private final Client mainWindow;
    private OpenAIProvider openAIProvider;

    private int currentIncrementalBackupNumber = 0;

    // Constructor to load the last opened directory on instantiation
    public App(Client mainWindow) {
        this.mainWindow = mainWindow;
        instance = this;
    }

    public void init() {
        // Load workspace mappings
        FileUtils.loadWorkspaceMappings();
        
        openAIProvider = new OpenAIProvider();
        openAIProvider.init();

        currentWorkspace = FileUtils.loadLastWorkspace();
        if (currentWorkspace != null) {
            // Ensure .vaiignore exists
            FileUtils.createDefaultVaiignore(currentWorkspace);
            // Load ignore list
            ignoreList.addAll(FileUtils.readVaiignore(currentWorkspace));

            mainWindow.getProjectPanel().refreshTree(currentWorkspace);
            currentIncrementalBackupNumber = FileUtils.loadIncrementalBackupNumber(currentWorkspace);
            // Load enabled files
            List<File> loadedEnabledFiles = FileUtils.loadEnabledFiles(currentWorkspace);
            enabledFiles.addAll(loadedEnabledFiles);
        }
    }

    private int getNextIncrementalBackupNumber() {
        int nextNumber = this.currentIncrementalBackupNumber + 1;
        this.currentIncrementalBackupNumber = nextNumber;
        FileUtils.saveIncrementalBackupNumber(this.currentIncrementalBackupNumber, this.currentWorkspace);
        return nextNumber;
    }

    public void openDirectory(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            finalizeDirectoryOpen(chooser.getSelectedFile());
        }
    }

    private void finalizeDirectoryOpen(File directory) {
        currentWorkspace = directory;
        FileUtils.saveLastWorkspace(currentWorkspace);
        FileUtils.createDefaultVaiignore(currentWorkspace);
        ignoreList.clear();
        ignoreList.addAll(FileUtils.readVaiignore(currentWorkspace));
        enabledFiles.clear();

        List<File> loadedEnabledFiles = FileUtils.loadEnabledFiles(currentWorkspace);
        enabledFiles.addAll(loadedEnabledFiles);
        mainWindow.getProjectPanel().refreshTree(currentWorkspace);
        currentIncrementalBackupNumber = FileUtils.loadIncrementalBackupNumber(currentWorkspace);
    }

    // New method to open directory by path
    public void openDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            finalizeDirectoryOpen(directory);
        } else {
            JOptionPane.showMessageDialog(mainWindow, "The provided path is not a valid directory.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String getApiKey() {
        return FileUtils.readFileToString(new File(Constants.API_KEY_PATH));
    }

    public File getCurrentWorkspace() {
        return currentWorkspace;
    }

    public Set<String> getIgnoreList() {
        return ignoreList;
    }

    public void toggleFile(File file) {
        System.out.println("Toggling " + file.getPath());
        if (enabledFiles.contains(file)) {
            enabledFiles.remove(file);
        } else {
            enabledFiles.add(file);
        }

        // Save the updated enabled files list
        FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);

        String tree = FileTreeBuilder.createTree(this.currentWorkspace, enabledFiles);
        System.out.println(tree);
    }

    public List<File> getEnabledFiles() {
        return enabledFiles;
    }

    public static App getInstance() {
        return instance;
    }

    public void removeFile(String selectedFile) {
        enabledFiles.removeIf(file -> file.getName().equals(selectedFile));
        // Save the updated enabled files list
        FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);
    }

    public static String PROMPT_TEMPLATE = """
GOAL: Take the following request, and make relevant changes to any relevant files of the supplied file context:

REQUEST: <REPLACEME_WITH_REQUEST>

You MUST respond with the following format for all changed files (json, with the magic)

```json
MAGIC_JSON_START
[
{
    "fileName": "full path found in == <path> ==",
    "new_contents": "<new contents of file>"
},
{
    "fileName": "another path",
    "new_contents": "<more contents>"
},
... etc
]
```

MAGIC_JSON_START is a token to denote where your json response will begin.
If you supply a fileName that does not exist, it will be created.

Think about any required changes, infer in areas whereas the context may not be supplied.
If a request is deemed impossible, you may communicate a reply via another magic "MAGIC_MESSAGE_START" with a simple string.

i.e.
MAGIC_MESSAGE_START "Sorry, but your request is impossible, for the following reason: ..."

Do not include multiple magics per response, pick MESSAGE or JSON.

-----

File Structure:
<REPLACEME_WITH_STRUCTURE>

Files (prefixed with == <FullRelativePath> ==, and ```s)
<REPLACEME_WITH_FILES>

-----

Begin now, ensuring correctness, good formatting, and adequate coding practices.
""";

    public OpenAIProvider getOpenAIProvider() {
        return this.openAIProvider;
    }

    public void submitRequest(String model, String description) {
        OpenAIProvider openAIProvider = App.getInstance().getOpenAIProvider();

        String structure = FileTreeBuilder.createTree(this.currentWorkspace, enabledFiles);

        // Replace the top level directory with a dot
        structure = structure.replaceFirst(this.currentWorkspace.getName() + "/", "./");


        String prompt = PROMPT_TEMPLATE
                .replace("<REPLACEME_WITH_REQUEST>", description)
                .replace("<REPLACEME_WITH_STRUCTURE>", structure)
                .replace("<REPLACEME_WITH_FILES>", formatEnabledFiles());

        String response = openAIProvider.request(model, prompt);
        System.out.println(response);

        int indexOfMessage = response.indexOf("MAGIC_MESSAGE_START");

        // Some leeway for whitespace, etc.
        if (indexOfMessage != -1 && indexOfMessage < 10) {
            JOptionPane.showMessageDialog(null, "Message from model: " + response.replace("MAGIC_MESSAGE_START", ""));
        } else if (response.contains("MAGIC_JSON_START")) {
            handleJsonResponse(response.replace("MAGIC_JSON_START", ""));
        }

        // Refresh the directory tree
        this.mainWindow.getProjectPanel().refreshTree(this.currentWorkspace);
    }

    private void handleJsonResponse(String json) {
        json = json.substring(json.indexOf('['));

        try {
            JSONArray jsonArray = new JSONArray(json);

            File vaiDir = FileUtils.getWorkspaceVaiDir(this.currentWorkspace);
            File backupDirectory = new File(vaiDir, Constants.VAI_BACKUP_DIR + "/" + getNextIncrementalBackupNumber());

            while (backupDirectory.exists()) {
                backupDirectory = new File(vaiDir, Constants.VAI_BACKUP_DIR + "/" + getNextIncrementalBackupNumber());
            }

            boolean mkdirs = backupDirectory.mkdirs();
            if (!mkdirs) {
                JOptionPane.showMessageDialog(null, "Failed to create backup directory: " + backupDirectory.getAbsolutePath());
                return;
            }

            Path workspacePath = Paths.get(this.currentWorkspace.getAbsolutePath());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String fileName = jsonObject.getString("fileName");
                String newContents = jsonObject.getString("new_contents");

                System.out.println("Writing to " + fileName);

                // 1. Setup backup directory, Constants.VAI_BACKUP_DIR/<incremental number>/
                // 2. Copy existing file to the backup directory, matching the path structure.
                // 3. Write the new contents to the file.
                // 4. Launch a diff tool to compare the original file with the new file. (meld)

                // We'll only set up the backup in json response, since we don't want to create a backup if the request is impossible.

                // 2. Copy file to that path in the backup directory, with relative path.

                File targetFile = new File(workspacePath + "/" + fileName);
                File backupFile = new File(backupDirectory.getAbsolutePath() + "/" + fileName);
                boolean b = backupFile.getParentFile().mkdirs();
                if (!b) {
                    System.out.println("[WARNING][Backup] Failed to create parent directory for " + backupFile.getAbsolutePath());
                }

                // Copy the file to the backup directory
                if (!targetFile.exists()) {
                    System.out.println("Origin file did not exist. That's okay.");
                } else {
                    try {
                        Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Write the new contents to the file
                if (!targetFile.exists()) {
                    boolean b1 = targetFile.getParentFile().mkdirs();
                    if (!b1) {
                        System.out.println("[WARNING][Backup] Failed to create parent directory for target " + targetFile.getAbsolutePath());
                    }
                    boolean b2 = targetFile.createNewFile();
                    if (!b2) {
                        System.out.println("[WARNING][Backup] Failed to create target file " + targetFile.getAbsolutePath());
                    }

                }
                FileUtils.writeStringToFile(targetFile, newContents);

                // Launch diff tool (meld)
                MeldLauncher.launchMeld(backupFile.toPath(), targetFile.toPath());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String formatEnabledFiles() {
        StringBuilder sb = new StringBuilder();
        Path workspacePath = Paths.get(this.currentWorkspace.getAbsolutePath());

        for (File file : enabledFiles) {
            String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);

            Path relativePath = workspacePath.relativize(Paths.get(file.getAbsolutePath()));
    //            String relativePathString = relativePath.toString();
    //             Replace first directory with a dot
    //            relativePathString = relativePathString.replaceFirst(relativePath.getName(0) + "/", ".");

            sb.append("== ").append(relativePath).append(" ==\n");
            sb.append("```").append(extension).append("\n");
            sb.append(FileUtils.readFileToString(file));
            sb.append("\n```\n");
        }
        return sb.toString();
    }
}
