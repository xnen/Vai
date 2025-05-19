package io.improt.vai.mapping;

import io.improt.vai.backend.App;
import io.improt.vai.mapping.WorkspaceMapper; // For hasValidExtension

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SubWorkspace {
    private String name;
    private List<String> filePaths; // Used for file-based sub-workspaces
    private List<String> monitoredDirectoryPaths; // Used for directory-based sub-workspaces
    private boolean isDirectoryBased;

    // For JSON deserialization
    public SubWorkspace() {
        this.filePaths = new ArrayList<>();
        this.monitoredDirectoryPaths = new ArrayList<>();
        this.isDirectoryBased = false;
    }

    // Constructor for file-based or initially empty directory-based sub-workspace
    public SubWorkspace(String name) {
        this.name = name;
        this.filePaths = new ArrayList<>();
        this.monitoredDirectoryPaths = new ArrayList<>();
        this.isDirectoryBased = false; // Default to file-based, can be changed
    }

    // Constructor for file-based sub-workspace with initial files
    public SubWorkspace(String name, List<String> filePaths) {
        this.name = name;
        this.filePaths = new ArrayList<>(filePaths);
        this.monitoredDirectoryPaths = new ArrayList<>();
        this.isDirectoryBased = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFilePaths() {
        if (isDirectoryBased) {
            return scanMonitoredDirectories();
        }
        // Return a copy to prevent external modification for file-based
        return new ArrayList<>(filePaths);
    }

    public void setFilePaths(List<String> filePaths) {
        if (isDirectoryBased) {
            System.err.println("Cannot directly set file paths for a directory-based SubWorkspace. Manage monitored directories instead.");
            return;
        }
        this.filePaths = new ArrayList<>(filePaths);
    }

    public void addFilePath(String path) {
        if (isDirectoryBased) {
            System.err.println("Cannot add individual file path to a directory-based SubWorkspace. Manage monitored directories instead.");
            return;
        }
        if (!this.filePaths.contains(path)) {
            this.filePaths.add(path);
        }
    }

    public void removeFilePath(String path) {
        if (isDirectoryBased) {
            System.err.println("Cannot remove individual file path from a directory-based SubWorkspace. Manage monitored directories instead.");
            return;
        }
        this.filePaths.remove(path);
    }

    public List<String> getMonitoredDirectoryPaths() {
        return new ArrayList<>(monitoredDirectoryPaths);
    }

    public void setMonitoredDirectoryPaths(List<String> paths) {
        if (!isDirectoryBased && !paths.isEmpty()) {
            // If not directory based but trying to set paths, implicitly convert?
            // For now, assume this is mainly for deserialization where isDirectoryBased is already set.
            System.err.println("Warning: Setting monitored directory paths on a SubWorkspace that is not marked as directory-based. Ensure isDirectoryBased is true.");
        }
        this.monitoredDirectoryPaths = new ArrayList<>(paths);
    }

    public void addMonitoredDirectoryPath(String path) {
        if (!isDirectoryBased) {
             System.err.println("Cannot add monitored directory path to a file-based SubWorkspace. Set to directory-based first.");
            return;
        }
        if (path != null && !path.trim().isEmpty() && !this.monitoredDirectoryPaths.contains(path)) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                this.monitoredDirectoryPaths.add(path);
            } else {
                System.err.println("Cannot add monitored directory path: '" + path + "'. It does not exist or is not a directory.");
            }
        }
    }

    public void removeMonitoredDirectoryPath(String path) {
        if (!isDirectoryBased) {
            System.err.println("Cannot remove monitored directory path from a file-based SubWorkspace.");
            return;
        }
        this.monitoredDirectoryPaths.remove(path);
    }


    public boolean isDirectoryBased() {
        return isDirectoryBased;
    }

    public void setDirectoryBased(boolean directoryBased) {
        this.isDirectoryBased = directoryBased;
        if (directoryBased && (this.monitoredDirectoryPaths == null || this.monitoredDirectoryPaths.isEmpty())) {
            System.err.println("Warning: SubWorkspace '" + name + "' set to directory-based but has no monitored directory paths specified yet.");
        }
        if (!directoryBased) {
            // If switching from directory-based to file-based, what to do with monitoredDirectoryPaths?
            // Option 1: Clear them. Option 2: Keep them (in case user switches back).
            // For now, keep them. If it becomes file-based, they are simply ignored by getFilePaths().
            // Also, filePaths might be empty. User would need to add files manually.
        }
    }

    private List<String> scanMonitoredDirectories() {
        List<String> collectedPaths = new ArrayList<>();
        if (this.monitoredDirectoryPaths == null || this.monitoredDirectoryPaths.isEmpty()) {
            // This is not an error if it's intentionally empty.
            // System.err.println("No monitored directories set for SubWorkspace: " + name);
            return collectedPaths;
        }

        Set<String> uniquePaths = new HashSet<>();
        for (String dirPath : this.monitoredDirectoryPaths) {
            File dir = new File(dirPath);
            if (!dir.exists() || !dir.isDirectory()) {
                System.err.println("Monitored directory does not exist or is not a directory: " + dirPath + " for SubWorkspace: " + name);
                continue;
            }
            collectFilesRecursively(dir, uniquePaths);
        }
        collectedPaths.addAll(uniquePaths);
        Collections.sort(collectedPaths); // For consistent ordering
        return collectedPaths;
    }

    private void collectFilesRecursively(File currentFile, Set<String> collectedPaths) {
        if (currentFile.isFile()) {
            if (WorkspaceMapper.hasValidExtension(currentFile.getName().toLowerCase())) {
                collectedPaths.add(currentFile.getAbsolutePath());
            }
        } else if (currentFile.isDirectory()) {
            // Skip common non-code directories, could be configurable
            String dirName = currentFile.getName().toLowerCase();
            if (dirName.equals(".git") || dirName.equals(".idea") || dirName.equals(".vscode") || dirName.equals("node_modules") || dirName.equals("target") || dirName.equals("build")) {
                return;
            }
            File[] children = currentFile.listFiles();
            if (children != null) {
                for (File child : children) {
                    collectFilesRecursively(child, collectedPaths);
                }
            }
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubWorkspace that = (SubWorkspace) o;
        return Objects.equals(name, that.name); // Equality is based on name only
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + (isDirectoryBased ? " (Dir)" : "");
    }

    // Deprecated getter, kept for a brief transition if anything external still uses it.
    /** @deprecated Use {@link #getMonitoredDirectoryPaths()} instead for directory-based sub-workspaces. */
    @Deprecated
    public String getDirectoryPath() {
        if (isDirectoryBased && monitoredDirectoryPaths != null && !monitoredDirectoryPaths.isEmpty()) {
            return monitoredDirectoryPaths.get(0); // Return first one as a placeholder if needed
        }
        return null;
    }

    // Deprecated setter, kept for a brief transition.
    /** @deprecated Use {@link #addMonitoredDirectoryPath(String)} or {@link #setMonitoredDirectoryPaths(List)} instead and ensure {@link #setDirectoryBased(boolean)} is true. */
    @Deprecated
    public void setDirectoryPath(String directoryPath) {
        if (isDirectoryBased) {
            System.err.println("setDirectoryPath is deprecated. Use addMonitoredDirectoryPath or setMonitoredDirectoryPaths.");
            if (this.monitoredDirectoryPaths == null) {
                this.monitoredDirectoryPaths = new ArrayList<>();
            }
            if (directoryPath != null && !directoryPath.trim().isEmpty() && !this.monitoredDirectoryPaths.contains(directoryPath)) {
                 this.monitoredDirectoryPaths.clear(); // Assuming setDirectoryPath implies a single directory
                 this.monitoredDirectoryPaths.add(directoryPath);
            }
        } else {
             System.err.println("setDirectoryPath is deprecated and should only be called on directory-based SubWorkspaces.");
        }
    }
}