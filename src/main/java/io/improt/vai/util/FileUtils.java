package io.improt.vai.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FileUtils {
    private static final Map<String, String> workspaceUuidMap = new HashMap<>(); // Maps uuid to path
    private static final Map<String, String> workspacePathToUuidMap = new HashMap<>(); // Maps path to uuid

    static {
        // Ensure that VAI_HOME_DIR exists
        File vaiHomeDir = new File(Constants.VAI_HOME_DIR);
        if (!vaiHomeDir.exists()) {
            boolean b = vaiHomeDir.mkdirs();
            if (!b) {
                System.out.println("[WARNING] Failed to create VAI_HOME_DIR");
            }
        }
        loadWorkspaceMappings();
    }

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
                    boolean b = parent.mkdirs();
                    if (!b) {
                        System.out.println("[WARNING] Failed to create parent directory for " + file.getAbsolutePath());
                    }
                }
                boolean b = file.createNewFile();
                if (!b) {
                    System.out.println("[WARNING] Failed to create file " + file.getAbsolutePath());
                }
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

    // New methods added to handle workspace mappings
    public static void loadWorkspaceMappings() {
        File file = new File(Constants.WORKSPACES_FILE);
        if (file.exists()) {
            String jsonContent = readFileToString(file);
            if (jsonContent != null && !jsonContent.isEmpty()) {
                try {
                    JSONObject jsonObject = new JSONObject(jsonContent);
                    for (String uuid : jsonObject.keySet()) {
                        String path = jsonObject.getString(uuid);
                        workspaceUuidMap.put(uuid, path);
                        workspacePathToUuidMap.put(path, uuid);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void saveWorkspaceMappings() {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, String> entry : workspaceUuidMap.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        writeStringToFile(new File(Constants.WORKSPACES_FILE), jsonObject.toString(4));
    }

    public static String getWorkspaceUUID(File workspace) {
        String path = workspace.getAbsolutePath();
        if (workspacePathToUuidMap.containsKey(path)) {
            return workspacePathToUuidMap.get(path);
        } else {
            String uuid = UUID.randomUUID().toString();
            workspaceUuidMap.put(uuid, path);
            workspacePathToUuidMap.put(path, uuid);
            saveWorkspaceMappings();
            return uuid;
        }
    }

    public static File getWorkspaceVaiDir(File workspace) {
        String uuid = getWorkspaceUUID(workspace);
        String vaiDirPath = Paths.get(Constants.VAI_HOME_DIR, uuid).toString();
        File vaiDir = new File(vaiDirPath);
        if (!vaiDir.exists()) {
            boolean b = vaiDir.mkdirs();
            if (!b) {
                System.out.println("[WARNING] Failed to create VAI directory for workspace " + workspace.getAbsolutePath());
            }
        }
        return vaiDir;
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

    public static int loadIncrementalBackupNumber(File workspace) {
        File vaiDir = getWorkspaceVaiDir(workspace);
        File file = new File(vaiDir, Constants.LAST_INCREMENTAL_BACKUP_NUMBER_FILE);
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
            System.out.println("No workspace to save incremental backup number.");
            return;
        }

        File vaiDir = getWorkspaceVaiDir(workspace);
        File file = new File(vaiDir, Constants.LAST_INCREMENTAL_BACKUP_NUMBER_FILE);
        writeStringToFile(file, String.valueOf(number));
    }

    // New methods for handling enabled files
    public static List<File> loadEnabledFiles(File workspace) {
        List<File> enabledFiles = new ArrayList<>();
        if (workspace == null) {
            return enabledFiles;
        }
        File vaiDir = getWorkspaceVaiDir(workspace);
        File enabledFilesFile = new File(vaiDir, Constants.ENABLED_FILES_FILE);
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
        File vaiDir = getWorkspaceVaiDir(workspace);
        File enabledFilesFile = new File(vaiDir, Constants.ENABLED_FILES_FILE);
        writeStringToFile(enabledFilesFile, jsonArray.toString(4)); // Pretty print with indentation
    }

    public static List<String> readVaiignore(File workspace) {
        File vaiDir = getWorkspaceVaiDir(workspace);
        File vaiignore = new File(vaiDir, Constants.VAIIGNORE_FILE);
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
        File vaiDir = getWorkspaceVaiDir(workspace);
        File vaiignore = new File(vaiDir, Constants.VAIIGNORE_FILE);
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

    // New methods for handling recent projects
    public static List<String> loadRecentProjects() {
        File recentFile = new File(Constants.RECENT_PROJECTS_FILE);
        if (!recentFile.exists()) {
            return new ArrayList<>();
        }
        String jsonContent = readFileToString(recentFile);
        if (jsonContent == null || jsonContent.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            JSONArray jsonArray = new JSONArray(jsonContent);
            List<String> recentProjects = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                recentProjects.add(jsonArray.getString(i));
            }
            return recentProjects;
        } catch (JSONException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveRecentProjects(List<String> recentProjects) {
        JSONArray jsonArray = new JSONArray(recentProjects);
        writeStringToFile(new File(Constants.RECENT_PROJECTS_FILE), jsonArray.toString(4));
    }

    public static void addRecentProject(String path) {
        List<String> recentProjects = loadRecentProjects();
        recentProjects.remove(path); // Remove if duplicate
        recentProjects.add(0, path); // Add to front
        // Optionally limit to 10 items
        if (recentProjects.size() > 10) {
            recentProjects = recentProjects.subList(0, 10);
        }
        saveRecentProjects(recentProjects);
    }

    // New methods to handle recentlyActive files
    public static List<String> loadRecentlyActiveFiles(File workspace) {
        File vaiDir = getWorkspaceVaiDir(workspace);
        File recentlyActiveFile = new File(vaiDir, Constants.RECENTLY_ACTIVE_FILES);
        if (!recentlyActiveFile.exists()) {
            return new ArrayList<>();
        }
        String jsonContent = readFileToString(recentlyActiveFile);
        if (jsonContent == null || jsonContent.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            JSONArray jsonArray = new JSONArray(jsonContent);
            List<String> recentFiles = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                recentFiles.add(jsonArray.getString(i));
            }
            return recentFiles;
        } catch (JSONException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveRecentlyActiveFiles(List<String> recentFiles, File workspace) {
        JSONArray jsonArray = new JSONArray();
        for (String path : recentFiles) {
            jsonArray.put(path);
        }
        File vaiDir = getWorkspaceVaiDir(workspace);
        File recentlyActiveFile = new File(vaiDir, Constants.RECENTLY_ACTIVE_FILES);
        writeStringToFile(recentlyActiveFile, jsonArray.toString(4)); // Pretty print with indent.
    }
    
    // New methods for handling tree configuration
    public static List<String> loadTreeConfig(File workspace) {
        if (workspace == null) {
            return new ArrayList<>();
        }
        File treeConfigFile = new File(getWorkspaceVaiDir(workspace), Constants.TREE_CONFIG_FILE);
        if (!treeConfigFile.exists()) {
            return new ArrayList<>();
        }
        String jsonContent = readFileToString(treeConfigFile);
        if (jsonContent == null || jsonContent.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            JSONArray jsonArray = new JSONArray(jsonContent);
            List<String> expandedPaths = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                expandedPaths.add(jsonArray.getString(i));
            }
            return expandedPaths;
        } catch (JSONException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveTreeConfig(List<String> expandedPaths, File workspace) {
        System.out.println("Saving tree config");
        if (workspace == null) {
            System.out.println("No workspace to save tree config.");
            return;
        }
        JSONArray jsonArray = new JSONArray();
        for (String path : expandedPaths) {
            jsonArray.put(path);
        }
        File treeConfigFile = new File(getWorkspaceVaiDir(workspace), Constants.TREE_CONFIG_FILE);
        writeStringToFile(treeConfigFile, jsonArray.toString(4)); // Pretty print with indent
    }
}
