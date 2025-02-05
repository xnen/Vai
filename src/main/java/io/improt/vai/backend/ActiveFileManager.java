package io.improt.vai.backend;

import io.improt.vai.frame.component.RecentActiveFilesPanel;
import io.improt.vai.util.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the active enabled files within the application.
 */
public class ActiveFileManager {

    private final List<File> enabledFiles = new ArrayList<>();
    private final List<File> dynamicFiles = new ArrayList<>();

    private final List<EnabledFilesChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final File currentWorkspace;

    /**
     * Constructs an ActiveFileManager for the specified workspace.
     *
     * @param currentWorkspace The current workspace directory.
     */
    public ActiveFileManager(File currentWorkspace) {
        this.currentWorkspace = currentWorkspace;
        init();
        startFileExistenceWatcher();
    }

    /**
     * Initializes the enabled files by loading them from the workspace.
     */
    private void init() {
        List<File> loadedEnabledFiles = FileUtils.loadEnabledFiles(currentWorkspace);
        enabledFiles.addAll(loadedEnabledFiles);
        for (File file : loadedEnabledFiles) {
            addToRecentlyActive(file);
        }
        notifyEnabledFilesChanged();
    }

    public boolean isFileActive(File file) {
        for (File enabledFile : enabledFiles) {
            if (enabledFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Starts a background thread to watch for the existence of enabled files.
     * Removes files that no longer exist and notifies listeners of changes.
     */
    private void startFileExistenceWatcher() {
        Thread watcherThread = new Thread(() -> {
            while (true) {
                try {
                    synchronized (enabledFiles) {
                        Iterator<File> iterator = enabledFiles.iterator();
                        boolean removed = false;
                        while (iterator.hasNext()) {
                            File file = iterator.next();
                            if (!file.exists()) {
                                iterator.remove();
                                removed = true;
                            }
                        }
                        if (removed) {
                            FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);
                            notifyEnabledFilesChanged();
                        }
                    }
                    // Sleep for 5 seconds before next check
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    /**
     * Adds a new file to the enabled files list.
     *
     * @param file The file to add.
     */
    public void addFile(File file) {
        if (file != null && file.exists() && file.isFile()) {
            String newFilePath = file.getAbsolutePath();
            boolean alreadyExists = enabledFiles.stream()
                    .anyMatch(enabledFile -> enabledFile.getAbsolutePath().equals(newFilePath));
            if (alreadyExists) {
                return;
            }
            enabledFiles.add(file);
            FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);
            addToRecentlyActive(file);
            notifyEnabledFilesChanged();
        }
    }

    /**
     * Removes a file from the enabled files list based on its name.
     *
     * @param selectedFile The name of the file to remove.
     */
    public void removeFile(String selectedFile) {
        enabledFiles.removeIf(file -> file.getName().equals(selectedFile));
        FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);
        notifyEnabledFilesChanged();
    }

    public boolean removeFile(File file) {
        boolean removed = enabledFiles.removeIf(enabledFile ->
                enabledFile.getAbsolutePath().equals(file.getAbsolutePath()));

        if (removed) {
            FileUtils.saveEnabledFiles(this.enabledFiles, currentWorkspace);
            notifyEnabledFilesChanged();
        }

        return removed;
    }

    /**
     * Clears all enabled files.
     */
    public void clearActiveFiles() {
        enabledFiles.clear();
        FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);
        notifyEnabledFilesChanged();
    }

    /**
     * Retrieves an unmodifiable list of currently enabled files.
     *
     * @return The list of enabled files.
     */
    public List<File> getEnabledFiles() {
        return Collections.unmodifiableList(enabledFiles);
    }

    /**
     * Concatenates enabledFiles and dynamicFiles lists into a new list without duplicates.
     * Duplicates are determined based on each File's absolute path.
     *
     * @return A new list containing files from both enabledFiles and dynamicFiles without duplicates.
     */
    List<File> concatenateWithoutDuplicates() {
        Map<String, File> fileMap = new LinkedHashMap<>();
        for (File file : enabledFiles) {
            fileMap.put(file.getAbsolutePath(), file);
        }
        for (File file : dynamicFiles) {
            fileMap.putIfAbsent(file.getAbsolutePath(), file);
        }
        return new ArrayList<>(fileMap.values());
    }

    /**
     * Formats the enabled files into a structured string representation.
     *
     * @return The formatted string of enabled files.
     */
    public String formatEnabledFiles() {
        StringBuilder sb = new StringBuilder();
        Path workspacePath = Paths.get(this.currentWorkspace.getAbsolutePath());
        String[] binaryExtensions = {"png", "jpg", "jpeg", "mp3", "wav", "mp4"};

        List<File> actives = this.concatenateWithoutDuplicates();

        for (File file : actives) {
            String extension = "";
            boolean isBinary = false;

            int dotIndex = file.getName().lastIndexOf('.');
            if (dotIndex != -1 && dotIndex < file.getName().length() - 1) {
                extension = file.getName().substring(dotIndex + 1).toLowerCase();
            }

            for (String binaryExt : binaryExtensions) {
                if (extension.equals(binaryExt)) {
                    isBinary = true;
                    break;
                }
            }

            Path relativePath = workspacePath.relativize(Paths.get(file.getAbsolutePath()));

            sb.append("== ").append(relativePath).append(" ==\n");
            if (!isBinary) {
                sb.append("```").append(extension).append("\n");
                sb.append(FileUtils.readFileToString(file));
                sb.append("\n```\n");
            } else {
                sb.append("The file is provided as an attachment, look there.\n"); // Indicate binary content is omitted
            }
        }
        return sb.toString();
    }

    /**
     * Adds a listener to be notified when enabledFiles changes.
     *
     * @param listener The listener to add.
     */
    public void addEnabledFilesChangeListener(EnabledFilesChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from the notification list.
     *
     * @param listener The listener to remove.
     */
    public void removeEnabledFilesChangeListener(EnabledFilesChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners about a change in enabledFiles.
     */
    private void notifyEnabledFilesChanged() {
        for (EnabledFilesChangeListener listener : listeners) {
            listener.onEnabledFilesChanged(List.copyOf(enabledFiles));
        }
    }

    /**
     * Adds a file to the recently active files list.
     *
     * @param file The file to add.
     */
    private void addToRecentlyActive(File file) {
        if (file == null) return;
        List<String> recentFiles = FileUtils.loadRecentlyActiveFiles(currentWorkspace);
        String filePath = file.getAbsolutePath();
        recentFiles.remove(filePath);
        recentFiles.add(0, filePath);

        if (recentFiles.size() > 100) {
            recentFiles = recentFiles.subList(0, 100);
        }
        FileUtils.saveRecentlyActiveFiles(recentFiles, currentWorkspace);

        RecentActiveFilesPanel panel = App.getInstance().getClient().getRecentActiveFilesPanel();
        if (panel != null) {
            panel.refresh();
        }
    }

    /**
     * For repository mapping -- any file paths that are dynamically introduced by the LLM.
     * The active files are ground truths from the user. These are introduced by the LLM. There may be overlaps.
     *
     */
    public void setupDynamicFiles(List<String> approvedFiles) {
        this.dynamicFiles.clear();
        for (String s : approvedFiles) {
            File file = new File(s);
            if (!file.exists() || file.isDirectory()) {
                System.out.println("Skipping '" + s + "' as it didn't exist or was a directory.");
                continue;
            }

            this.dynamicFiles.add(file);
        }
    }

    /**
     * Interface for listeners interested in changes to enabledFiles.
     */
    public interface EnabledFilesChangeListener {
        /**
         * Called when the list of enabled files has changed.
         *
         * @param updatedEnabledFiles The updated list of enabled files.
         */
        void onEnabledFilesChanged(List<File> updatedEnabledFiles);
    }
}
