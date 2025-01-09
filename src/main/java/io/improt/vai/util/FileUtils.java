package io.improt.vai.util;

import io.improt.vai.util.Constants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FileUtils {
    public static String readFileToString(File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeStringToFile(File file, String newContents) {
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        try {
            Files.write(file.toPath(), newContents.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // New methods added to handle filesystem operations
    public static File loadLastWorkspace() {
        File file = new File(Constants.LAST_WORKSPACE_FILE);
        if (file.exists()) {
            String path = readFileToString(file);
            if (path != null && !path.isEmpty()) {
                File workspace = new File(path);
                if (workspace.exists() && workspace.isDirectory()) {
                    return workspace;
                }
            }
        }
        return null;
    }

    public static void saveLastWorkspace(File workspace) {
        if (workspace == null) {
            return;
        }
        writeStringToFile(new File(Constants.LAST_WORKSPACE_FILE), workspace.getAbsolutePath());
    }

    public static int loadIncrementalBackupNumber() {
        File file = new File(Constants.LAST_INCREMENTAL_BACKUP_NUMBER_FILE);
        String content = readFileToString(file);
        if (content == null || content.isEmpty()) {
            return 0;
        } else {
            try {
                return Integer.parseInt(content);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return 0;
            }
        }
    }

    public static void saveIncrementalBackupNumber(int number, File workspace) {
        if (workspace == null) {
            System.out.println("No workspace to save incremental backup number to.");
            return;
        }
        
        File incrementalBackupNumberFile = new File(workspace.getAbsolutePath() + "/" + Constants.LAST_INCREMENTAL_BACKUP_NUMBER_FILE);
        writeStringToFile(incrementalBackupNumberFile, String.valueOf(number));
    }
    
    // New methods for handling enabled files
    public static List<File> loadEnabledFiles(File workspace) {
        List<File> enabledFiles = new ArrayList<>();
        if (workspace == null) {
            return enabledFiles;
        }
        File enabledFilesFile = new File(workspace, Constants.ENABLED_FILES_FILE);
        if (!enabledFilesFile.exists()) {
            return enabledFiles;
        }
        String jsonContent = readFileToString(enabledFilesFile);
        if (jsonContent == null || jsonContent.isEmpty()) {
            return enabledFiles;
        }
        try {
            JSONArray jsonArray = new JSONArray(jsonContent);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String filePath = obj.getString("path");
                File file = new File(filePath);
                if (file.exists()) {
                    enabledFiles.add(file);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return enabledFiles;
    }

    public static void saveEnabledFiles(List<File> enabledFiles, File workspace) {
        if (workspace == null) {
            System.out.println("No workspace to save enabled files.");
            return;
        }
        JSONArray jsonArray = new JSONArray();
        for (File file : enabledFiles) {
            JSONObject obj = new JSONObject();
            obj.put("path", file.getAbsolutePath());
            jsonArray.put(obj);
        }
        File enabledFilesFile = new File(workspace, Constants.ENABLED_FILES_FILE);
        writeStringToFile(enabledFilesFile, jsonArray.toString(4)); // Pretty print with indentation
    }

    public static List<String> readVaiignore(File workspace) {
        File vaiignore = new File(workspace, Constants.VAIIGNORE_FILE);
        if (!vaiignore.exists()) {
            return new ArrayList<>();
        }
        try {
            return Files.readAllLines(vaiignore.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void createDefaultVaiignore(File workspace) {
        File vaiignore = new File(workspace, Constants.VAIIGNORE_FILE);
        if (vaiignore.exists()) {
            return;
        }
        List<String> defaults = Arrays.asList(
            Constants.VAI_BACKUP_DIR,
            Constants.LAST_INCREMENTAL_BACKUP_NUMBER_FILE,
            Constants.ENABLED_FILES_FILE,
            Constants.VAIIGNORE_FILE
        );
        try {
            Files.write(vaiignore.toPath(), defaults, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
