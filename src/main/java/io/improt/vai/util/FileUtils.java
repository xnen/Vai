package io.improt.vai.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.improt.vai.mapping.SubWorkspace;
import org.jetbrains.annotations.NotNull;
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
                    workspaceUuidMap.clear(); // Clear before loading
                    workspacePathToUuidMap.clear(); // Clear before loading
                    for (String uuid : jsonObject.keySet()) {
                        String path = jsonObject.getString(uuid);
                        workspaceUuidMap.put(uuid, path);
                        workspacePathToUuidMap.put(path, uuid);
                    }
                } catch (JSONException e) {
                    System.err.println("[FileUtils] Error parsing workspace mappings: " + e.getMessage());
                    // e.printStackTrace(); // Potentially noisy
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

    public static Map<String, String> getWorkspacePathToUuidMap() {
        // Ensure mappings are loaded if map is empty, could happen if no other method triggered load yet
        if (workspacePathToUuidMap.isEmpty() && new File(Constants.WORKSPACES_FILE).exists()) {
            loadWorkspaceMappings();
        }
        return new HashMap<>(workspacePathToUuidMap); // Return a copy
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
        if (workspace == null) {
            System.out.println("No workspace found for backups.");
            return 0;
        }

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
            System.out.println("No workspace to load enabled files from.");
            return enabledFiles;
        }
        File vaiDir = getWorkspaceVaiDir(workspace);
        File enabledFilesFile = new File(vaiDir, Constants.ENABLED_FILES_FILE);
        if (!enabledFilesFile.exists()) {
            System.out.println("No enabled files file found.");
            return enabledFiles;
        }
        String jsonContent = readFileToString(enabledFilesFile);
        if (jsonContent == null || jsonContent.isEmpty()) {
            System.out.println("Empty JSON content.");
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
                } else {
                    System.out.println("File " + filePath + " does not exist.");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println("Successfully loaded " + enabledFiles.size() + " files.");

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
        System.out.println("Saving " + enabledFiles.size() + " files to " + enabledFilesFile.getAbsolutePath());
        writeStringToFile(enabledFilesFile, jsonArray.toString(4)); // Pretty print with indentation
    }

    // New methods for handling recent projects
    public static List<String> loadRecentProjects() {
        File recentFile = new File(Constants.RECENT_PROJECTS_FILE);
        return getStrings(recentFile);
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
        if (workspace == null) {
            System.out.println("No workspace found for recently active files.");
            return new ArrayList<>();
        }

        File vaiDir = getWorkspaceVaiDir(workspace);
        File recentlyActiveFile = new File(vaiDir, Constants.RECENTLY_ACTIVE_FILES);
        return getStrings(recentlyActiveFile);
    }

    @NotNull
    private static List<String> getStrings(File recentlyActiveFile) {
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
        return getStrings(treeConfigFile);
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

    // New methods for handling subworkspaces
    public static List<SubWorkspace> loadSubWorkspaces(File workspace) {
        List<SubWorkspace> subWorkspaces = new ArrayList<>();
        if (workspace == null) {
            System.out.println("[FileUtils] No workspace to load subworkspaces from.");
            return subWorkspaces;
        }
        File vaiDir = getWorkspaceVaiDir(workspace);
        File subWorkspacesFile = new File(vaiDir, Constants.SUBWORKSPACE_DEFINITIONS_FILE);

        if (!subWorkspacesFile.exists()) {
            System.out.println("[FileUtils] No subworkspace definitions file found at " + subWorkspacesFile.getAbsolutePath());
            return subWorkspaces;
        }

        String jsonContent = readFileToString(subWorkspacesFile);
        if (jsonContent == null || jsonContent.isEmpty()) {
            System.out.println("[FileUtils] Empty subworkspace definitions JSON content.");
            return subWorkspaces;
        }

        try {
            JSONArray jsonArray = new JSONArray(jsonContent);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String name = obj.getString("name");
                JSONArray pathsArray = obj.getJSONArray("filePaths");
                List<String> filePaths = new ArrayList<>();
                for (int j = 0; j < pathsArray.length(); j++) {
                    filePaths.add(pathsArray.getString(j));
                }
                subWorkspaces.add(new SubWorkspace(name, filePaths));
            }
        } catch (JSONException e) {
            System.err.println("[FileUtils] Error parsing subworkspace definitions: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("[FileUtils] Loaded " + subWorkspaces.size() + " subworkspaces for " + workspace.getName());
        return subWorkspaces;
    }

    public static void saveSubWorkspaces(List<SubWorkspace> subWorkspaces, File workspace) {
        if (workspace == null) {
            System.out.println("[FileUtils] No workspace to save subworkspaces to.");
            return;
        }
        JSONArray jsonArray = new JSONArray();
        for (SubWorkspace sw : subWorkspaces) {
            JSONObject obj = new JSONObject();
            obj.put("name", sw.getName());
            obj.put("filePaths", new JSONArray(sw.getFilePaths()));
            jsonArray.put(obj);
        }
        File vaiDir = getWorkspaceVaiDir(workspace);
        File subWorkspacesFile = new File(vaiDir, Constants.SUBWORKSPACE_DEFINITIONS_FILE);
        System.out.println("[FileUtils] Saving " + subWorkspaces.size() + " subworkspaces to " + subWorkspacesFile.getAbsolutePath());
        writeStringToFile(subWorkspacesFile, jsonArray.toString(4)); // Pretty print with indentation
    }
}
