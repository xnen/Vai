package io.improt.vai.mapping;

/**
 * Listener interface for receiving updates about the mapping process.
 */
public interface MappingProgressListener {
    /**
     * Called when the mapping process for a specific file starts.
     * @param filePath Absolute path of the file being mapped.
     */
    void fileMappingStarted(String filePath);

    /**
     * Called when the mapping process for a specific file completes.
     * @param filePath Absolute path of the file that was mapped.
     * @param success True if mapping was successful, false otherwise.
     * @param message A message, typically an error message if not successful.
     */
    void fileMappingCompleted(String filePath, boolean success, String message);

    /**
     * Called when the entire batch of files initiated together has been processed.
     */
    void allFilesProcessed();
}