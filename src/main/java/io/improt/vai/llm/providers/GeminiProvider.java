package io.improt.vai.llm.providers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GeminiProvider implements IModelProvider {

    private String apiKey;
    private final String pythonScriptPath; // Path to the python script

    public GeminiProvider() {
        // Assuming the python script is in a 'python' directory relative to the project root.
        // You might need to adjust this based on your actual project structure.
        this.pythonScriptPath = "./python/gemini_client.py";
    }

    @Override
    public void init() {
        this.apiKey = System.getenv("GOOGLE_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("[GeminiProvider] API key not found in environment variable GOOGLE_API_KEY. Gemini will be disabled.");
        } else {
            System.out.println("[GeminiProvider] Gemini API key found.");
        }
        File scriptFile = new File(pythonScriptPath);
        if (!scriptFile.exists()) {
            throw new RuntimeException("[GeminiProvider] Python script not found at: " + pythonScriptPath);
        } else {
            System.out.println("[GeminiProvider] Python script found at: " + pythonScriptPath);
        }
    }

    @Override
    public String request(String model, String prompt, String userRequest, List<File> files) {
        // Ensure necessary configuration is present
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("[GeminiProvider] Gemini API key is not configured. Cannot make request.");
        }
        if (!new File(pythonScriptPath).exists()) {
            throw new RuntimeException("[GeminiProvider] Python script not found at: " + pythonScriptPath + ". Cannot make request.");
        }

        File tempFile = null;
        File promptTempFile = null; // New temp file for prompt
        try {
            // Create temp file listing file paths
            tempFile = File.createTempFile("file_paths", ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                for (File file : files) {
                    writer.write(file.getAbsolutePath());
                    writer.newLine();
                }
            }

            // Create temp file for prompt text
            promptTempFile = File.createTempFile("prompt", ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(promptTempFile))) {
                writer.write(prompt);
            }

            List<String> commandList = new ArrayList<>();
            commandList.add("python3");
            commandList.add(pythonScriptPath);
            commandList.add(model);
            commandList.add(promptTempFile.getAbsolutePath()); // Pass prompt temp file path
            commandList.add(tempFile.getAbsolutePath()); // Pass file paths temp file path

            ProcessBuilder pb = new ProcessBuilder(commandList);
            pb.redirectErrorStream(false); // Separate error stream

            Process process = pb.start();

            // Capture stdout
            BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder stdoutBuilder = new StringBuilder();
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                stdoutBuilder.append(line).append("\n");
            }

            // Capture stderr
            BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder stderrBuilder = new StringBuilder();
            while ((line = stderrReader.readLine()) != null) {
                stderrBuilder.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            String stdout = stdoutBuilder.toString();
            String stderr = stderrBuilder.toString();

            if (exitCode == 0) {
                return parseResponse(stdout);
            } else {
                String errorMsg = "[GeminiProvider] Python script execution failed with exit code: " + exitCode +
                        "\nStderr: " + stderr;
                throw new RuntimeException(errorMsg);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("[GeminiProvider] Error executing Python script: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                tempFile.delete(); // Delete temp file
            }
            if (promptTempFile != null) {
                promptTempFile.delete(); // Delete prompt temp file
            }
        }
    }

    private String parseResponse(String responseBody) {
        try {
            // The python script directly returns the text.
            return responseBody.trim();
        } catch (Exception e) {
            System.err.println("[GeminiProvider] Error parsing Gemini API response: " + e.getMessage());
            System.err.println("[GeminiProvider] Response body: " + responseBody);
            e.printStackTrace();
            throw new RuntimeException("Failed to parse Gemini API response.", e);
        }
    }

    @Override
    public boolean supportsAudio() {
        return true;
    }

    @Override
    public boolean supportsVideo() {
        return true;
    }

    @Override
    public boolean supportsVision() {
        return true;
    }
}
