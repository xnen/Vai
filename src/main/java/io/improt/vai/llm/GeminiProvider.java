package io.improt.vai.llm;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GeminiProvider implements LLMProvider {

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
            System.out.println("[GeminiProvider] API key not found in environment variable GOOGLE_API_KEY. Gemini will be disabled.");
        } else {
            System.out.println("[GeminiProvider] Gemini API key found.");
        }
        File scriptFile = new File(pythonScriptPath);
        if (!scriptFile.exists()) {
            System.err.println("[GeminiProvider] Python script not found at: " + pythonScriptPath);
        } else {
            System.out.println("[GeminiProvider] Python script found at: " + pythonScriptPath);
        }
    }

    @Override
    public String request(String model, String prompt, List<File> files) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("[GeminiProvider] Gemini API key is not configured. Cannot make request.");
            return null;
        }

        if (!new File(pythonScriptPath).exists()) {
            System.err.println("[GeminiProvider] Python script not found at: " + pythonScriptPath + ". Cannot make request.");
            return null;
        }

        List<String> commandList = new ArrayList<>();
        commandList.add("python3");
        commandList.add(pythonScriptPath);
        commandList.add(model);
        commandList.add(prompt);
        commandList.addAll(files.stream().map(File::getAbsolutePath).collect(Collectors.toList()));

        ProcessBuilder pb = new ProcessBuilder(commandList);
        pb.redirectErrorStream(false); // Separate error stream

        try {
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
                System.err.println("[GeminiProvider] Python script execution failed with exit code: " + exitCode);
                System.err.println("[GeminiProvider] Script stdout: " + stdout);
                System.err.println("[GeminiProvider] Script stderr: " + stderr); // Log stderr
                return null;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[GeminiProvider] Error executing Python script: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    private String parseResponse(String responseBody) {
        // Basic parsing - assumes the simplest successful response structure from Gemini API docs.
        // Robust error handling and more detailed parsing might be needed based on full API spec.
        try {
            // The python script directly returns the text, so no need to parse JSON anymore.
            return responseBody.trim();
        } catch (Exception e) {
            System.err.println("[GeminiProvider] Error parsing Gemini API response: " + e.getMessage());
            System.err.println("[GeminiProvider] Response body: " + responseBody); // Log the response body for debugging
            e.printStackTrace();
        }
        return null; // Or throw an exception if parsing failure should halt process.
    }
}
