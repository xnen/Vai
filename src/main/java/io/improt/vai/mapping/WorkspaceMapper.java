package io.improt.vai.mapping;

import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionReasoningEffort;
import io.improt.vai.llm.providers.O3MiniProvider;
import io.improt.vai.util.FileUtils;
import io.improt.vai.backend.App;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WorkspaceMapper {

    // This constant is used to store the mappings JSON file in the workspace's VAI directory.
    private static final String MAPPINGS_FILENAME = "class_mappings.json";

    private final File currentWorkspace;
    private final Map<String, ClassMapping> mappings;
    // Single-thread executor to allow only one worker thread at a time.
    private final ExecutorService mappingExecutor = Executors.newSingleThreadExecutor();

    /**
     * Constructs a WorkspaceMapper utilizing the current workspace from the App instance.
     */
    public WorkspaceMapper() {
        this.currentWorkspace = App.getInstance().getCurrentWorkspace();
        this.mappings = new HashMap<>();
        loadMappings();
        cullMappings();
    }

    /**
     * Represents a mapping entry for a class file.
     *
     * Attributes:
     *  - path:        The absolute path of the file.
     *  - md5sum:      The current MD5 checksum of the file content.
     *  - mapping:     The mapping content generated by an LLM.
     *  - lastMappingMd5sum: The MD5 sum computed at the last mapping generation.
     *
     * The method isUpToDate() checks if the file has remained unchanged 
     * since the mapping was generated.
     */
    public static class ClassMapping {
        private String path;
        private String md5sum;
        private String mapping;
        private String lastMappingMd5sum;

        public ClassMapping(String path, String md5sum) {
            this.path = path;
            this.md5sum = md5sum;
            this.mapping = "";
            this.lastMappingMd5sum = "";
        }

        public String getPath() {
            return path;
        }

        public String getMd5sum() {
            return md5sum;
        }

        public void setMd5sum(String md5sum) {
            this.md5sum = md5sum;
        }

        public String getMapping() {
            return mapping;
        }

        public void setMapping(String mapping) {
            this.mapping = mapping;
        }

        public String getLastMappingMd5sum() {
            return lastMappingMd5sum;
        }

        public void setLastMappingMd5sum(String lastMappingMd5sum) {
            this.lastMappingMd5sum = lastMappingMd5sum;
        }

        /**
         * Checks if the mapping is up-to-date by comparing the current file MD5 and the
         * MD5 recorded at the last mapping generation.
         *
         * @return true if the mapping is up-to-date, false otherwise.
         */
        public boolean isUpToDate() {
            return mapping != null && md5sum != null && md5sum.equals(lastMappingMd5sum);
        }
    }

    /**
     * Computes the MD5 checksum of a file.
     *
     * @param file The file whose MD5 sum is to be computed.
     * @return The MD5 checksum as a hexadecimal string.
     */
    public static String computeMD5(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] contentBytes = Files.readAllBytes(file.toPath());
            byte[] hashBytes = md.digest(contentBytes);
            // Convert the hash bytes into a hex string.
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Adds a single file to the mapping dictionary.
     *
     * @param file The file to be mapped.
     */
    public void addFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        String filePath = file.getAbsolutePath();
        String md5 = computeMD5(file);
        // Update the mapping if it already exists, or create a new one.
        if (mappings.containsKey(filePath)) {
            ClassMapping cm = mappings.get(filePath);
            cm.setMd5sum(md5);
        } else {
            ClassMapping cm = new ClassMapping(filePath, md5);
            mappings.put(filePath, cm);
        }
        persistMappings();
    }

    /**
     * Adds all files within the specified directory (recursively) to the mapping dictionary.
     *
     * @param directory The directory whose files are to be mapped.
     */
    public void addDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        List<File> files = listFilesRecursively(directory);
        for (File file : files) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".cs") || fileName.endsWith(".java") || fileName.endsWith(".ts")) {
                addFile(file);
            }
        }
    }

    /**
     * Removes the mapping of a specific file.
     *
     * @param file The file to remove from the mappings.
     */
    public void removeFile(File file) {
        if (file == null) {
            return;
        }
        String filePath = file.getAbsolutePath();
        if (mappings.containsKey(filePath)) {
            mappings.remove(filePath);
            persistMappings();
        }
    }

    /**
     * Removes the mapping of a file given its path.
     *
     * @param filePath The file path of the mapping to remove.
     */
    public void removeFile(String filePath) {
        if (filePath == null) return;
        if (mappings.containsKey(filePath)) {
            mappings.remove(filePath);
            persistMappings();
        }
    }

    /**
     * Removes all mappings for files located in the given directory (recursively).
     *
     * @param directory The directory whose file mappings should be removed.
     */
    public void removeDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        String dirPath = directory.getAbsolutePath();
        boolean removed = false;
        Iterator<String> iterator = mappings.keySet().iterator();
        while (iterator.hasNext()) {
            String filePath = iterator.next();
            if (filePath.startsWith(dirPath)) {
                iterator.remove();
                removed = true;
            }
        }
        if (removed) {
            persistMappings();
        }
    }

    /**
     * Recursively lists all files in a directory.
     *
     * @param directory The directory to search.
     * @return A list of files found within the directory.
     */
    private List<File> listFilesRecursively(File directory) {
        List<File> fileList = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) return fileList;
        for (File file : files) {
            if (file.isDirectory()) {
                fileList.addAll(listFilesRecursively(file));
            } else if (file.isFile()) {
                fileList.add(file);
            }
        }
        return fileList;
    }

    /**
     * Persists the mapping dictionary to JSON in the workspace's VAI directory.
     */
    private synchronized void persistMappings() {
        JSONArray jsonArray = new JSONArray();
        for (ClassMapping cm : mappings.values()) {
            JSONObject obj = new JSONObject();
            obj.put("path", cm.getPath());
            obj.put("md5sum", cm.getMd5sum());
            obj.put("mapping", cm.getMapping());
            obj.put("lastMappingMd5sum", cm.getLastMappingMd5sum());
            jsonArray.put(obj);
        }
        // Save the JSON array into the "class_mappings.json" file in the workspace specific VAI directory.
        File vaiDir = FileUtils.getWorkspaceVaiDir(currentWorkspace);
        File mappingFile = new File(vaiDir, MAPPINGS_FILENAME);
        FileUtils.writeStringToFile(mappingFile, jsonArray.toString(4));
    }

    /**
     * Loads the mapping dictionary from the JSON file in the workspace's VAI directory.
     */
    private void loadMappings() {
        File vaiDir = FileUtils.getWorkspaceVaiDir(currentWorkspace);
        File mappingFile = new File(vaiDir, MAPPINGS_FILENAME);
        if (!mappingFile.exists()) {
            return;
        }
        String jsonContent = FileUtils.readFileToString(mappingFile);
        if (jsonContent == null || jsonContent.isEmpty()) {
            return;
        }
        try {
            JSONArray jsonArray = new JSONArray(jsonContent);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String path = obj.getString("path");
                File file = new File(path);
                if (file.exists()) {
                    String md5 = computeMD5(file);
                    String mapping = obj.optString("mapping", "");
                    String lastMappingMd5sum = obj.optString("lastMappingMd5sum", "");
                    ClassMapping cm = new ClassMapping(path, md5);
                    cm.setMapping(mapping);
                    cm.setLastMappingMd5sum(lastMappingMd5sum);
                    mappings.put(path, cm);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a copy of the current mapping dictionary as a single concatenated String.
     * Each file path is displayed as a relative path from the current workspace.
     *
     * @return Concatenated mapping String.
     */
    public String getAllMappingsConcatenated() {
        StringBuilder sb = new StringBuilder();
        File workspace = App.getInstance().getCurrentWorkspace();
        if (workspace != null) {
            Path workspacePath = Paths.get(workspace.getAbsolutePath());
            for (ClassMapping cm : mappings.values()) {
                try {
                    Path filePath = Paths.get(cm.getPath());
                    Path relativePath = workspacePath.relativize(filePath);
                    sb.append("PATH: ").append(relativePath).append("\n");
                } catch (Exception e) {
                    sb.append("PATH: ").append(cm.getPath()).append("\n");
                }
                sb.append(cm.getMapping()).append("\n\n");
            }
        } else {
            // Fallback if workspace is not available
            for (ClassMapping cm : mappings.values()) {
                sb.append("PATH: ").append(cm.getPath()).append("\n");
                sb.append(cm.getMapping()).append("\n");
            }
        }
        return sb.toString();
    }
    
    /**
     * Maps a given file if its mapping is outdated.
     * This method will enqueue a mapping generation task to ensure that only one
     * mapping worker thread executes at a time.
     *
     * @param file The file to be mapped.
     */
    public void mapFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        String filePath = file.getAbsolutePath();
        String currentMd5 = computeMD5(file);
        
        if (mappings.containsKey(filePath)) {
            ClassMapping cm = mappings.get(filePath);
            if (currentMd5.equals(cm.getLastMappingMd5sum())) {
                // Mapping is up-to-date, so skip re-mapping.
                return;
            }
            // Update current file checksum.
            cm.setMd5sum(currentMd5);
            // Enqueue asynchronous mapping update.
            generateMapping(file);
        } else {
            // File is not mapped yet. Create new mapping.
            ClassMapping cm = new ClassMapping(filePath, currentMd5);
            mappings.put(filePath, cm);
            generateMapping(file);
        }
        // Note: persistMappings() will be invoked in the worker once the LLM completes processing.
    }

    private void cullMappings() {
        // Figure out what files don't exist!
        List<String> mappingsToRemove = new ArrayList<>();
        for (ClassMapping mapping : mappings.values()) {
            String path = mapping.getPath();
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("Mapping '" + path + "' does not exist anymore.");
                mappingsToRemove.add(path);
            }
        }
        for (String s : mappingsToRemove) {
            mappings.remove(s);
        }
        persistMappings();
    }

    /**
     * Spawns a worker thread to generate the mapping asynchronously using LLM,
     * but ensures only one worker thread is active at a time by using a single-thread executor.
     *
     * @param file The file to generate a mapping for.
     * @return Returns an empty string immediately (result will be handled asynchronously).
     */
    private String generateMapping(File file) {
        ClassMapping cm = mappings.get(file.getAbsolutePath());
        if (cm == null) {
            return "";
        }
        String currentMd5 = computeMD5(file);
        System.out.println("[WorkspaceMapper] Created a worker thread.");
        MappingWorker worker = new MappingWorker(file, cm, currentMd5);
        mappingExecutor.submit(worker);
        return "";
    }
    
    /**
     * Iterates over all files within the specified directory (recursively) and maps each file
     * only if its mapping is outdated.
     *
     * @param directory The directory whose files are to be mapped.
     */
    public void mapDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        List<File> files = listFilesRecursively(directory);
        for (File file : files) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".cs") || fileName.endsWith(".java") || fileName.endsWith(".ts")) {
                mapFile(file);
            }
        }
    }
    
    /**
     * Iterates over all mappings and re-maps files that are outdated.
     */
    public void mapAllOutdated() {
        for (ClassMapping cm : mappings.values()) {
            if (!cm.isUpToDate()) {
                File file = new File(cm.getPath());
                mapFile(file);
            }
        }
    }
    
    /**
     * Returns the list of all class mappings.
     *
     * @return a List of ClassMapping objects.
     */
    public List<ClassMapping> getMappings() {
        return new ArrayList<>(mappings.values());
    }
    
    /**
     * Private worker class responsible for generating the mapping for a given file.
     * This worker reads the file content, builds a prompt for the LLM, submits the request,
     * and updates the corresponding ClassMapping instance. Upon finishing, it calls persistMappings()
     * inside a synchronized block to ensure thread safety.
     */
    private class MappingWorker implements Runnable {
        private final File file;
        private final ClassMapping classMapping;
        private final String currentMd5;
        
        public MappingWorker(File file, ClassMapping classMapping, String currentMd5) {
            this.file = file;
            this.classMapping = classMapping;
            this.currentMd5 = currentMd5;
        }
        
        @Override
        public void run() {
            // Read file contents.
            String fileContents = FileUtils.readFileToString(file);
            if (fileContents == null) {
                fileContents = "";
            }
            
            String prompt =
                "Goal: Reduce class length for overview.\n" +
                "Take classes and reduce them to as primitive of an outline as possible, for the purpose of quick overview by developers looking to identify which classes may be relevant to reference for a certain task.\n" +
                "\n" +
                "Format:\n" +
                "<namespace/package>\n" +
                "class <classname> : <any inheritances>\n" +
                "    fields: \n" +
                "        field1: int;\n" +
                "        field2: string;\n" +
                "        field3: Object;\n" +
                "    methods:\n" +
                "        methodOne(): string; <description of methodOne>\n" +
                "        methodTwo(obj: Object); <description of methodTwo>\n" +
                "\n" +
                "If no fields, don't include fields block. If no methods, don't include methods block. Modifiers are not relevant (i.e. final/readonly public private etc).";
            
            O3MiniProvider miniProvider = new O3MiniProvider();
            try {
                ChatCompletionCreateParams simpleParams = miniProvider.simpleSystemUserRequest(prompt, fileContents, ChatCompletionReasoningEffort.LOW);
                String llmResponse = miniProvider.blockingCompletion(simpleParams);

                // Update the mapping in a thread-safe manner and persist the change.
                synchronized (WorkspaceMapper.this) {
                    classMapping.setMapping(llmResponse);
                    classMapping.setLastMappingMd5sum(currentMd5);
                    persistMappings();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
