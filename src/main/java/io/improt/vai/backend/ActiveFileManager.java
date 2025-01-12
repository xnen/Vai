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
        // Iterate paths.
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
     * Toggles the enabled state of a file. Adds it if it's not enabled, removes it otherwise.
     *
     * @param file The file to toggle.
     */
    public void toggleFile(File file) {
        if (enabledFiles.contains(file)) {
            enabledFiles.remove(file);
        } else {
            enabledFiles.add(file);
            addToRecentlyActive(file);
        }

        FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);

        // Notify listeners about the enabled files change
        notifyEnabledFilesChanged();
    }

    /**
     * Adds a new file to the enabled files list.
     *
     * @param file The file to add.
     */
    public void addFile(File file) {
        if (file != null && file.exists() && file.isFile()) {
            String newFilePath = file.getAbsolutePath();
            for (File enabledFile : enabledFiles) {
                if (enabledFile.getAbsolutePath().equals(newFilePath)) {
                    // File already exists in enabledFiles
                    return;
                }
            }
            enabledFiles.add(file);
            // Save the updated enabled files list
            FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);

            // Also add to recently active files
            addToRecentlyActive(file);

            // Notify listeners about the enabled files change
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
        // Save the updated enabled files list
        FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);

        // Notify listeners about the enabled files change
        notifyEnabledFilesChanged();
    }

    public boolean removeFile(File file) {
        // Not sure if the File object is usable within remove. Let's iterate and remove with the path!
        // GPTODO: The file object passed here is a new object, not necessarily the one in the enabledFiles list.
        //         This is why im doing this. If this isn't needed, please correct this.

        boolean removed = false;
        List<File> newEnabledFiles = new ArrayList<>(this.enabledFiles);
        for (File enabledFile : newEnabledFiles) {
            if (enabledFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                enabledFiles.remove(enabledFile);
                removed = true;
                break;
            }
        }

        // Save the updated enabled files list
        FileUtils.saveEnabledFiles(this.enabledFiles, currentWorkspace);

        // Notify listeners about the enabled files change
        notifyEnabledFilesChanged();

        return removed;
    }

    /**
     * Clears all enabled files.
     */
    public void clearActiveFiles() {
        enabledFiles.clear();
        FileUtils.saveEnabledFiles(enabledFiles, currentWorkspace);

        // Notify listeners about the enabled files change
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
     * Formats the enabled files into a structured string representation.
     *
     * @return The formatted string of enabled files.
     */
    public String formatEnabledFiles() {
        StringBuilder sb = new StringBuilder();
        Path workspacePath = Paths.get(this.currentWorkspace.getAbsolutePath());

        for (File file : enabledFiles) {
            String extension = "";

            int dotIndex = file.getName().lastIndexOf('.');
            if (dotIndex != -1 && dotIndex < file.getName().length() - 1) {
                extension = file.getName().substring(dotIndex + 1);
            }

            Path relativePath = workspacePath.relativize(Paths.get(file.getAbsolutePath()));

            sb.append("== ").append(relativePath).append(" ==\n");
            sb.append("```").append(extension).append("\n");
            sb.append(FileUtils.readFileToString(file));
            sb.append("\n```\n");
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

        // Refresh the recent active files panel
        RecentActiveFilesPanel panel = App.getInstance().getClient().getRecentActiveFilesPanel();
        if (panel != null) {
            panel.refresh();
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
