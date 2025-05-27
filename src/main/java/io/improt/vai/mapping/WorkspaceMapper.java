package io.improt.vai.mapping;

import com.openai.models.ReasoningEffort;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import io.improt.vai.frame.dialogs.MappingProgressDialog;
import io.improt.vai.llm.providers.O3MiniProvider;
import io.improt.vai.llm.providers.O4MiniProvider;
import io.improt.vai.util.FileUtils;

import javax.swing.*;
import java.awt.*;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WorkspaceMapper {

    public static final String MAPPINGS_FILENAME = "class_mappings.json";

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
                || lowerFileName.endsWith(".schema")
                || lowerFileName.endsWith(".json")
                || lowerFileName.endsWith(".yaml")
                || lowerFileName.endsWith(".yml");
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
        
        List<ClassMapping> loadedCms = loadClassMappingsFromFile(mappingFile, true);
        mappings.clear();
        for(ClassMapping cm : loadedCms) {
            mappings.put(cm.getPath(), cm);
        }
    }

    public static List<ClassMapping> loadClassMappingsFromFile(File classMappingsJsonFile, boolean recomputeMd5IfFileExists) {
        List<ClassMapping> loadedMappings = new ArrayList<>();
        if (classMappingsJsonFile == null || !classMappingsJsonFile.exists()) {
            System.err.println("[WorkspaceMapper] Class mappings file does not exist or is null: " + (classMappingsJsonFile != null ? classMappingsJsonFile.getAbsolutePath() : "null"));
            return loadedMappings;
        }
        String jsonContent = FileUtils.readFileToString(classMappingsJsonFile);
        if (jsonContent == null || jsonContent.isEmpty()) {
            System.err.println("[WorkspaceMapper] Class mappings file is empty: " + classMappingsJsonFile.getAbsolutePath());
            return loadedMappings;
        }
        try {
            JSONArray jsonArray = new JSONArray(jsonContent);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String path = obj.getString("path");
                String md5sumInJson = obj.getString("md5sum");
                String mapping = obj.optString("mapping", "");
                String lastMappingMd5sum = obj.optString("lastMappingMd5sum", "");

                File fileOnDisk = new File(path);
                String currentMd5 = md5sumInJson; 

                if (recomputeMd5IfFileExists && fileOnDisk.exists()) {
                    currentMd5 = computeMD5(fileOnDisk); 
                } else if (!fileOnDisk.exists() && recomputeMd5IfFileExists) {
                     System.out.println("[WorkspaceMapper] File path '" + path + "' from mappings ("+classMappingsJsonFile.getName()+") didn't exist. Using MD5 from JSON.");
                }

                ClassMapping cm = new ClassMapping(path, currentMd5);
                cm.setMapping(mapping);
                cm.setLastMappingMd5sum(lastMappingMd5sum); 
                
                if (!recomputeMd5IfFileExists) {
                    cm.setMd5sum(md5sumInJson);
                }

                if (!recomputeMd5IfFileExists || fileOnDisk.exists()) {
                    loadedMappings.add(cm);
                } else {
                     System.out.println("[WorkspaceMapper] File path '" + path + "' from mappings ("+classMappingsJsonFile.getName()+") didn't exist. Not adding to active map!");
                }
            }
        } catch (JSONException e) {
            System.err.println("[WorkspaceMapper] Error parsing class mappings from " + classMappingsJsonFile.getAbsolutePath() + ": " + e.getMessage());
        }
        return loadedMappings;
    }


    public String getAllMappingsConcatenated() {
        return getConcatenatedMappingsForClassMappingList(new ArrayList<>(mappings.values()), this.currentWorkspace);
    }
    
    public String getConcatenatedMappingsForPaths(List<String> filePaths) {
        List<ClassMapping> selectedCms = new ArrayList<>();
        for (String path : filePaths) {
            ClassMapping cm = mappings.get(path);
            if (cm != null) {
                selectedCms.add(cm);
            }
        }
        return getConcatenatedMappingsForClassMappingList(selectedCms, this.currentWorkspace);
    }

    public static String getConcatenatedMappingsForClassMappingList(List<ClassMapping> classMappingList, File referenceWorkspaceForRelativePaths) {
        StringBuilder sb = new StringBuilder();
        Path referenceWorkspacePath = (referenceWorkspaceForRelativePaths != null) ? Paths.get(referenceWorkspaceForRelativePaths.getAbsolutePath()) : null;

        for (ClassMapping cm : classMappingList) {
            if (cm.getMapping() == null || cm.getMapping().isEmpty()) continue;

            String displayPath = cm.getPath();
            if (referenceWorkspacePath != null) {
                try {
                    Path filePathObj = Paths.get(cm.getPath());
                    if (filePathObj.startsWith(referenceWorkspacePath)) {
                        displayPath = referenceWorkspacePath.relativize(filePathObj).toString();
                    } else {
                        displayPath = filePathObj.toAbsolutePath().toString();
                    }
                } catch (Exception e) {
                    displayPath = Paths.get(cm.getPath()).toAbsolutePath().toString();
                }
            } else {
                 displayPath = Paths.get(cm.getPath()).toAbsolutePath().toString();
            }

            sb.append("PATH: ").append(displayPath).append("\n");
            sb.append(cm.getMapping()).append("\n\n");
        }
        return sb.toString();
    }
    
    public void mapFile(File file) {
        mapFile(file, null); // Overload for calls without a listener
    }

    public void mapFile(File file, MappingProgressListener progressListener) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        String filePath = file.getAbsolutePath();
        String currentMd5 = computeMD5(file);
        
        ClassMapping cm = mappings.get(filePath);
        if (cm != null) {
            if (currentMd5.equals(cm.getLastMappingMd5sum()) && cm.getMapping() != null && !cm.getMapping().isEmpty()) {
                 if (progressListener != null) { // If called with listener, still notify completion for this file
                    progressListener.fileMappingCompleted(filePath, true, "Already up-to-date");
                }
                return;
            }
            cm.setMd5sum(currentMd5);
        } else {
            cm = new ClassMapping(filePath, currentMd5);
            mappings.put(filePath, cm);
            persistMappings();
        }
        generateMapping(file, cm, currentMd5, progressListener);
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

    private void generateMapping(File file, ClassMapping classMapping, String currentMd5ForWorker, MappingProgressListener progressListener) {
        ClassMapping currentCmState = mappings.get(file.getAbsolutePath());
        if (currentCmState == null) {
             System.err.println("[WorkspaceMapper] Tried to generate mapping for untracked file: " + file.getAbsolutePath());
             if (progressListener != null) {
                progressListener.fileMappingCompleted(file.getAbsolutePath(), false, "File untracked");
            }
            return;
        }
        
        System.out.println("[WorkspaceMapper] Queuing mapping generation for: " + file.getName());
        MappingWorker worker = new MappingWorker(file, currentCmState, currentMd5ForWorker, progressListener);
        mappingExecutor.submit(worker);
    }
    
    public void mapDirectory(File directory, Window owner) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        List<File> filesInDir = listFilesRecursively(directory);
        List<ClassMapping> mappingsToProcess = new ArrayList<>();

        for (File file : filesInDir) {
            if (hasValidExtension(file.getName())) {
                String filePath = file.getAbsolutePath();
                String currentMd5 = computeMD5(file);
                ClassMapping cm = mappings.get(filePath);
                if (cm == null) { // New file or untracked
                    cm = new ClassMapping(filePath, currentMd5);
                    mappings.put(filePath, cm); // Track it
                    mappingsToProcess.add(cm);
                } else {
                    cm.setMd5sum(currentMd5); // Update MD5
                    if (!cm.isUpToDate()) {
                        mappingsToProcess.add(cm);
                    }
                }
            }
        }
        persistMappings(); // Persist any newly tracked files

        if (!mappingsToProcess.isEmpty()) {
            MappingProgressDialog dialog = new MappingProgressDialog(owner, new ArrayList<>(mappingsToProcess), currentWorkspace); // Pass copy
            SwingUtilities.invokeLater(() -> dialog.setVisible(true));
            
            AtomicInteger tasksSubmitted = new AtomicInteger(0);
            for (ClassMapping cmToProcess : mappingsToProcess) {
                File fileToProcess = new File(cmToProcess.getPath());
                 // mapFile will call generateMapping which submits the worker with the listener
                mapFile(fileToProcess, dialog);
                tasksSubmitted.incrementAndGet();
            }
             // Check if all tasks submitted are actually going to run (some might be up-to-date)
             // The dialog's allFilesProcessed should be called after all workers complete.
             // We need a way to call dialog.allFilesProcessed() when this specific batch is done.
             // This can be handled by having the dialog itself count completions against its initial list.
             // For now, the dialog handles its own lifecycle based on file completions.
             // Let's add a final notification from here IF no tasks were actually run by mapFile due to being up-to-date
            if(tasksSubmitted.get() == 0 && dialog != null){ // If all were surprisingly up-to-date
                 dialog.allFilesProcessed();
            }
            // The dialog should get a final allFilesProcessed() call when the batch of mapFile calls here is known to be done.
            // This is complex with async workers. The dialog needs to manage its own "all done" state.

        } else {
            JOptionPane.showMessageDialog(owner, "All files in the directory are already up-to-date.", "Directory Mapping", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    public void mapAllOutdated(Window owner) {
        List<ClassMapping> snapshot = new ArrayList<>(mappings.values());
        List<ClassMapping> outdatedMappings = snapshot.stream()
                .filter(cm -> {
                    File f = new File(cm.getPath());
                    if (!f.exists()) { // Cull non-existent files during this check
                        mappings.remove(cm.getPath());
                        System.out.println("Removed mapping for non-existent file during mapAllOutdated scan: " + cm.getPath());
                        return false;
                    }
                    // Recompute MD5 just in case it changed since last load/add
                    String currentMd5 = computeMD5(f);
                    cm.setMd5sum(currentMd5);
                    return !cm.isUpToDate();
                })
                .collect(Collectors.toList());

        if (!outdatedMappings.isEmpty()) {
            MappingProgressDialog dialog = new MappingProgressDialog(owner, outdatedMappings, currentWorkspace);
            SwingUtilities.invokeLater(() -> dialog.setVisible(true));

            AtomicInteger tasksSubmitted = new AtomicInteger(0);
            for (ClassMapping cm : outdatedMappings) {
                File file = new File(cm.getPath());
                 // mapFile will call generateMapping which submits the worker with the listener
                mapFile(file, dialog);
                tasksSubmitted.incrementAndGet();
            }
             if(tasksSubmitted.get() == 0 && dialog != null){
                 dialog.allFilesProcessed();
            }
        } else {
            JOptionPane.showMessageDialog(owner, "All tracked files are already up-to-date.", "Update All Mappings", JOptionPane.INFORMATION_MESSAGE);
        }
        persistMappings(); // Persist any removals or MD5 updates
    }
    
    public List<ClassMapping> getMappings() {
        return new ArrayList<>(mappings.values());
    }
    
    private class MappingWorker implements Runnable {
        private final File file;
        private final ClassMapping classMappingStateAtQueueTime;
        private final String md5AtQueueTime;
        private final MappingProgressListener progressListener;
        
        public MappingWorker(File file, ClassMapping classMapping, String md5AtQueueTime, MappingProgressListener listener) {
            this.file = file;
            this.classMappingStateAtQueueTime = classMapping;
            this.md5AtQueueTime = md5AtQueueTime;
            this.progressListener = listener;
        }
        
        @Override
        public void run() {
            ClassMapping currentMainMapCm = mappings.get(file.getAbsolutePath());
            if (currentMainMapCm == null) {
                System.err.println("[MappingWorker] File " + file.getName() + " no longer tracked. Aborting.");
                if (progressListener != null) {
                    progressListener.fileMappingCompleted(file.getAbsolutePath(), false, "File no longer tracked");
                }
                return;
            }

            if (progressListener != null) {
                progressListener.fileMappingStarted(file.getAbsolutePath());
            }

            String actualCurrentFileMd5 = computeMD5(file);
            if (!md5AtQueueTime.equals(actualCurrentFileMd5) || !md5AtQueueTime.equals(currentMainMapCm.getMd5sum())) {
                 System.out.println("[MappingWorker] File " + file.getName() + " changed. Aborting stale worker.");
                 if (progressListener != null) {
                    progressListener.fileMappingCompleted(file.getAbsolutePath(), false, "File changed, stale worker");
                }
                 return;
            }
            if(md5AtQueueTime.equals(currentMainMapCm.getLastMappingMd5sum()) && currentMainMapCm.getMapping() != null && !currentMainMapCm.getMapping().isEmpty()){
                System.out.println("[MappingWorker] File " + file.getName() + " MD5 " + md5AtQueueTime + " already mapped. Aborting redundant worker.");
                 if (progressListener != null) {
                    progressListener.fileMappingCompleted(file.getAbsolutePath(), true, "Already mapped by another worker");
                }
                return;
            }

            System.out.println("[MappingWorker] Starting LLM mapping for: " + file.getName() + " (MD5: " + md5AtQueueTime + ")");
            String fileContents = FileUtils.readFileToString(file);
            if (fileContents == null) fileContents = "";
            
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
                    ClassMapping finalCheckCm = mappings.get(file.getAbsolutePath());
                    if (finalCheckCm != null && finalCheckCm.getMd5sum().equals(md5AtQueueTime)) {
                        finalCheckCm.setMapping(llmResponse);
                        finalCheckCm.setLastMappingMd5sum(md5AtQueueTime);
                        System.out.println("[MappingWorker] Successfully mapped: " + file.getName());
                        persistMappings();
                        if (progressListener != null) {
                            progressListener.fileMappingCompleted(file.getAbsolutePath(), true, "Successfully mapped");
                        }
                    } else {
                         System.out.println("[MappingWorker] MD5 changed or file untracked before update for: " + file.getName() + ". LLM result discarded.");
                         if (progressListener != null) {
                            progressListener.fileMappingCompleted(file.getAbsolutePath(), false, "MD5 changed or untracked before update");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[MappingWorker] Error during LLM mapping for " + file.getName() + ": " + e.getMessage());
                // e.printStackTrace(); // Keep console less cluttered for dialog testing
                 if (progressListener != null) {
                    progressListener.fileMappingCompleted(file.getAbsolutePath(), false, "LLM Error: " + e.getMessage().substring(0, Math.min(e.getMessage().length(), 50)));
                }
            }
        }
    }
}