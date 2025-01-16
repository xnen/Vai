package io.improt.vai.maps;

import io.improt.vai.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WorkspaceMapper {

    /**
     * Generates a workspace map by recursively iterating through the project directory,
     * mapping each .java file, and writing the mappings to WorkspaceMap.dat.
     *
     * @param workspace The root directory of the workspace.
     * @throws IOException If an I/O error occurs during file processing.
     */
    public static void generateWorkspaceMap(File workspace) throws IOException {
        System.out.println("*** Workspace mapper not yet implemented.");
//        if (workspace == null || !workspace.exists() || !workspace.isDirectory()) {
//            System.out.println("[WorkspaceMapper] Invalid workspace directory.");
//            return;
//        }
//
//        List<File> javaFiles = listJavaFiles(workspace);
//        List<String> mappings = new ArrayList<>();
//        JavaMapper mapper = new JavaMapper();
//
//        System.out.println("[WorkspaceMapper] Mapping " + javaFiles.size() + " Java files...");
//
//        for (File javaFile : javaFiles) {
//            try {
//                String map = mapper.mapFileToString(javaFile);
//                mappings.add(map);
//            } catch (IOException e) {
//                System.out.println("[WorkspaceMapper] Failed to map file: " + javaFile.getAbsolutePath());
//                e.printStackTrace();
//            }
//        }
//
//        // Convert the list of mappings to a String array
//        String[] mappingsArray = mappings.toArray(new String[0]);
//
//        // Join the array into a single string with newlines
//        String joinedMappings = String.join("\n", mappingsArray);
//
//        // Determine the path to WorkspaceMap.dat within the workspace's VAI directory
//        File vaiDir = FileUtils.getWorkspaceVaiDir(workspace);
//        File workspaceMapFile = new File(vaiDir, "WorkspaceMap.dat");
//
//        // Write the mappings to WorkspaceMap.dat
//        FileUtils.writeStringToFile(workspaceMapFile, joinedMappings);
//
//        System.out.println("[WorkspaceMapper] Workspace map generated at: " + workspaceMapFile.getAbsolutePath());
    }

    /**
     * Recursively lists all .java files within the given directory.
     *
     * @param directory The root directory to search.
     * @return A list of Java files.
     * @throws IOException If an I/O error occurs during file traversal.
     */
    private static List<File> listJavaFiles(File directory) throws IOException {
        return Files.walk(directory.toPath())
                .filter(path -> path.toString().endsWith(".java"))
                .map(Path::toFile)
                .collect(Collectors.toList());
    }
}
