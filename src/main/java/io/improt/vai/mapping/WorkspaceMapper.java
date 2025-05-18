package io.improt.vai.mapping;

import com.openai.models.ReasoningEffort;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import io.improt.vai.llm.providers.O3MiniProvider;
import io.improt.vai.llm.providers.O4MiniProvider;
import io.improt.vai.util.FileUtils;

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

    private static final String MAPPINGS_FILENAME = "class_mappings.json";

    private final File currentWorkspace;
    private final Map<String, ClassMapping> mappings;
    private final ExecutorService mappingExecutor = Executors.newFixedThreadPool(30);

    public WorkspaceMapper(File workspace) {
        this.currentWorkspace = workspace;
        this.mappings = new HashMap<>();
        loadMappings();
        cullMappings();
    }

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

        public boolean isUpToDate() {
            return mapping != null && !mapping.isEmpty() && md5sum != null && md5sum.equals(lastMappingMd5sum);
        }
    }

    public static String computeMD5(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] contentBytes = Files.readAllBytes(file.toPath());
            byte[] hashBytes = md.digest(contentBytes);
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

    public void addFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        String filePath = file.getAbsolutePath();
        String md5 = computeMD5(file);
        if (mappings.containsKey(filePath)) {
            ClassMapping cm = mappings.get(filePath);
            cm.setMd5sum(md5); // Update MD5, mapFile will decide if re-mapping is needed
        } else {
            ClassMapping cm = new ClassMapping(filePath, md5);
            mappings.put(filePath, cm);
        }
        persistMappings(); // Persist immediately as file is now tracked
    }

    public static boolean hasValidExtension(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".cs")
                || lowerFileName.endsWith(".java")
                || lowerFileName.endsWith(".ts")
                || lowerFileName.endsWith(".md")
                || lowerFileName.endsWith(".js")
                || lowerFileName.endsWith(".css")
                || lowerFileName.endsWith(".html")
                || lowerFileName.endsWith(".py")
                || lowerFileName.endsWith(".schema");
    }

    public void addDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }

        List<File> files = listFilesRecursively(directory);
        for (File file : files) {
            if (hasValidExtension(file.getName())) {
                addFile(file); // addFile now handles persistence and basic tracking
            }
        }
    }

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

    public void removeFile(String filePath) {
        if (filePath == null) return;
        if (mappings.containsKey(filePath)) {
            mappings.remove(filePath);
            persistMappings();
        }
    }

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

    private List<File> listFilesRecursively(File directory) {
        List<File> fileList = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) return fileList;
        for (File file : files) {
            if (file.isDirectory()) {
                // TODO: Add configuration to ignore certain directories like .git, .idea, node_modules, target etc.
                String dirName = file.getName();
                if (dirName.equals(".git") || dirName.equals(".idea") || dirName.equals("node_modules") || dirName.equals("target") || dirName.equals("build") || dirName.equals(".vscode")) {
                    continue;
                }
                fileList.addAll(listFilesRecursively(file));
            } else if (file.isFile()) {
                fileList.add(file);
            }
        }
        return fileList;
    }

    private synchronized void persistMappings() {
        if (currentWorkspace == null) {
            System.err.println("[WorkspaceMapper] Cannot persist mappings, currentWorkspace is null.");
            return;
        }
        JSONArray jsonArray = new JSONArray();
        for (ClassMapping cm : mappings.values()) {
            JSONObject obj = new JSONObject();
            obj.put("path", cm.getPath());
            obj.put("md5sum", cm.getMd5sum());
            obj.put("mapping", cm.getMapping());
            obj.put("lastMappingMd5sum", cm.getLastMappingMd5sum());
            jsonArray.put(obj);
        }
        File vaiDir = FileUtils.getWorkspaceVaiDir(currentWorkspace);
        File mappingFile = new File(vaiDir, MAPPINGS_FILENAME);
        FileUtils.writeStringToFile(mappingFile, jsonArray.toString(4));
    }

    private void loadMappings() {
        if (this.currentWorkspace == null) {
             System.err.println("[WorkspaceMapper] Cannot load mappings, currentWorkspace is null.");
            return;
        }
        File vaiDir = FileUtils.getWorkspaceVaiDir(this.currentWorkspace);
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
                    String md5 = computeMD5(file); // Recompute current MD5 on load
                    String mapping = obj.optString("mapping", "");
                    String lastMappingMd5sum = obj.optString("lastMappingMd5sum", "");
                    ClassMapping cm = new ClassMapping(path, md5);
                    cm.setMapping(mapping);
                    cm.setLastMappingMd5sum(lastMappingMd5sum);
                    mappings.put(path, cm);
                } else {
                    System.out.println("File path '" + path + "' from mappings didn't exist. Not adding!");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getAllMappingsConcatenated() {
        StringBuilder sb = new StringBuilder();
        File workspace = this.currentWorkspace;
        if (workspace != null) {
            Path workspacePath = Paths.get(workspace.getAbsolutePath());
            for (ClassMapping cm : mappings.values()) {
                if (cm.getMapping() == null || cm.getMapping().isEmpty()) continue; // Skip unmapped files
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
            for (ClassMapping cm : mappings.values()) {
                 if (cm.getMapping() == null || cm.getMapping().isEmpty()) continue;
                sb.append("PATH: ").append(cm.getPath()).append("\n");
                sb.append(cm.getMapping()).append("\n");
            }
        }
        return sb.toString();
    }
    
    public String getConcatenatedMappingsForPaths(List<String> filePaths) {
        StringBuilder sb = new StringBuilder();
        File workspace = this.currentWorkspace;
        Path workspacePath = (workspace != null) ? Paths.get(workspace.getAbsolutePath()) : null;

        for (String path : filePaths) {
            ClassMapping cm = mappings.get(path);
            if (cm != null && cm.getMapping() != null && !cm.getMapping().isEmpty()) { // Ensure mapping exists
                try {
                    Path filePathObj = Paths.get(cm.getPath());
                    Path relativePath = (workspacePath != null) ? workspacePath.relativize(filePathObj) : filePathObj;
                    sb.append("PATH: ").append(relativePath).append("\n");
                } catch (Exception e) {
                    sb.append("PATH: ").append(cm.getPath()).append("\n");
                }
                sb.append(cm.getMapping()).append("\n\n");
            }
        }
        return sb.toString();
    }
    
    public void mapFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        String filePath = file.getAbsolutePath();
        String currentMd5 = computeMD5(file);
        
        ClassMapping cm = mappings.get(filePath);
        if (cm != null) {
            // File is already tracked
            if (currentMd5.equals(cm.getLastMappingMd5sum()) && cm.getMapping() != null && !cm.getMapping().isEmpty()) {
                // Mapping is up-to-date and exists, so skip re-mapping.
                return;
            }
            cm.setMd5sum(currentMd5); // Update current file checksum.
        } else {
            // File is not tracked yet. Create new mapping entry.
            cm = new ClassMapping(filePath, currentMd5);
            mappings.put(filePath, cm);
            // Persist now that it's tracked, even before LLM mapping
            persistMappings();
        }
        // Enqueue asynchronous mapping update.
        generateMapping(file, cm, currentMd5);
    }

    private void cullMappings() {
        if (mappings.isEmpty() || currentWorkspace == null) return;
        List<String> mappingsToRemove = new ArrayList<>();
        for (ClassMapping mapping : mappings.values()) {
            String path = mapping.getPath();
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("Mapping for '" + path + "' points to a non-existent file. Removing.");
                mappingsToRemove.add(path);
            }
        }
        if (!mappingsToRemove.isEmpty()) {
            for (String s : mappingsToRemove) {
                mappings.remove(s);
            }
            persistMappings();
        }
    }

    private String generateMapping(File file, ClassMapping classMapping, String currentMd5ForWorker) {
        // The cm parameter might be stale if mapFile was called multiple times quickly for the same file
        // before the first worker started. Always get the latest from the map.
        ClassMapping currentCmState = mappings.get(file.getAbsolutePath());
        if (currentCmState == null) {
             System.err.println("[WorkspaceMapper] Tried to generate mapping for untracked file: " + file.getAbsolutePath());
            return ""; // Should not happen if mapFile logic is correct
        }
        
        System.out.println("[WorkspaceMapper] Queuing mapping generation for: " + file.getName());
        MappingWorker worker = new MappingWorker(file, currentCmState, currentMd5ForWorker);
        mappingExecutor.submit(worker);
        return ""; // Returns immediately
    }
    
    public void mapDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        List<File> files = listFilesRecursively(directory);
        for (File file : files) {
            if (hasValidExtension(file.getName())) {
                mapFile(file); // mapFile handles all logic including adding if new, and queuing for mapping
            }
        }
    }
    
    public void mapAllOutdated() {
        // Create a snapshot of mappings to iterate over to avoid ConcurrentModificationException
        // if mappings are modified by workers.
        List<ClassMapping> snapshot = new ArrayList<>(mappings.values());
        for (ClassMapping cm : snapshot) {
            if (!cm.isUpToDate()) {
                File file = new File(cm.getPath());
                if (file.exists()) { // Ensure file still exists before trying to map
                    mapFile(file);
                } else {
                     // File doesn't exist, remove it from mappings
                    mappings.remove(cm.getPath());
                    System.out.println("Removed mapping for non-existent file during mapAllOutdated: " + cm.getPath());
                }
            }
        }
        persistMappings(); // Persist any removals
    }
    
    public List<ClassMapping> getMappings() {
        return new ArrayList<>(mappings.values());
    }
    
    private class MappingWorker implements Runnable {
        private final File file;
        private final ClassMapping classMappingStateAtQueueTime; // The state when worker was created
        private final String md5AtQueueTime;
        
        public MappingWorker(File file, ClassMapping classMapping, String md5AtQueueTime) {
            this.file = file;
            this.classMappingStateAtQueueTime = classMapping; // Capture the specific CM instance
            this.md5AtQueueTime = md5AtQueueTime;
        }
        
        @Override
        public void run() {
            // Critical: Re-fetch the ClassMapping from the main map to ensure we're working with the latest instance.
            // This helps if multiple mapFile calls happened for the same file before this worker started.
            ClassMapping currentMainMapCm = mappings.get(file.getAbsolutePath());
            if (currentMainMapCm == null) {
                System.err.println("[MappingWorker] File " + file.getName() + " no longer tracked in main mappings. Aborting worker.");
                return;
            }

            // Double check if another worker has already updated this mapping for the same content version
            // or if the file has changed again since this worker was queued.
            String latestMd5InMap = currentMainMapCm.getMd5sum(); // Current MD5 of the file as per map
            String actualCurrentFileMd5 = computeMD5(file); // MD5 of file on disk right now

            if (!md5AtQueueTime.equals(actualCurrentFileMd5) || !md5AtQueueTime.equals(latestMd5InMap)) {
                 System.out.println("[MappingWorker] File " + file.getName() + " changed since worker was queued or MD5 mismatch. Aborting stale worker.");
                 // Another mapFile call would have updated md5sum and queued a newer worker.
                 return;
            }
            // Also check if lastMappingMd5sum is already set to this md5AtQueueTime (meaning already processed by another worker)
            if(md5AtQueueTime.equals(currentMainMapCm.getLastMappingMd5sum()) && currentMainMapCm.getMapping() != null && !currentMainMapCm.getMapping().isEmpty()){
                System.out.println("[MappingWorker] File " + file.getName() + " with MD5 " + md5AtQueueTime + " already mapped. Aborting redundant worker.");
                return;
            }


            System.out.println("[MappingWorker] Starting LLM mapping for: " + file.getName() + " (MD5: " + md5AtQueueTime + ")");
            String fileContents = FileUtils.readFileToString(file);
            if (fileContents == null) {
                fileContents = "";
            }
            
            String prompt =
                "Goal: Reduce file contents for overview.\n" +
                "Take files and reduce their content to a somewhat primitive state, for the purpose of overview by developers looking to quickly find what classes, methods, fields, etc are relevant to complete tasks.\n" +
                "\n" +
                "Format (if a standard class):\n" +
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
                "If no fields, don't include fields block. If no methods, don't include methods block.\n" +
                "Write a full overview of the file as well afterward, explaining the purpose and some details about the file. DO NOT INFER OR GUESS WHAT SOMETHING DOES OR IS. SIMPLY WRITE A BRIEF ABSTRACT OUTLINE OF WHAT YOU BELIEVE THE PURPOSE OF THE CLASS IS.\n\n" +
                "Developers should have a clear understanding of all fields, methods, and the general purpose aspect by viewing the result you produce.";
            
            O4MiniProvider miniProvider = new O4MiniProvider();
            try {
                ChatCompletionCreateParams simpleParams = miniProvider.simpleSystemUserRequest(prompt, fileContents, ReasoningEffort.LOW);
                String llmResponse = miniProvider.blockingCompletion(simpleParams);

                synchronized (WorkspaceMapper.this) {
                    // Re-fetch CM again inside synchronized block to ensure atomicity of check-then-set
                    ClassMapping finalCheckCm = mappings.get(file.getAbsolutePath());
                    if (finalCheckCm != null && finalCheckCm.getMd5sum().equals(md5AtQueueTime)) {
                        // Only update if the md5sum in the map still matches the one this worker processed
                        // This prevents a stale worker from overwriting a newer mapping if the file changed rapidly.
                        finalCheckCm.setMapping(llmResponse);
                        finalCheckCm.setLastMappingMd5sum(md5AtQueueTime); // Set last mapped MD5 to the one processed
                        System.out.println("[MappingWorker] Successfully mapped: " + file.getName());
                        persistMappings();
                    } else {
                         System.out.println("[MappingWorker] MD5 changed or file untracked before update for: " + file.getName() + ". LLM result discarded.");
                    }
                }
            } catch (Exception e) {
                System.err.println("[MappingWorker] Error during LLM mapping for " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
