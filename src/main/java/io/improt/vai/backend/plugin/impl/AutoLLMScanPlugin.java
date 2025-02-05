package io.improt.vai.backend.plugin.impl;

import com.openai.models.ChatCompletionReasoningEffort;
import io.improt.vai.backend.App;
import io.improt.vai.backend.plugin.AbstractPlugin;
import io.improt.vai.frame.ClientFrame;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plugin to automatically scan the current workspace for files containing "// TODO: <prompt>"
 * commands in *.cs, *.java, or *.ts files. When found, the plugin replaces "VAI" with "TODO"
 * in the comment, saves the updated file, and submits the extracted prompt automatically
 * via the client frame.
 *
 * The scanning runs in a separate daemon thread with a 2-second delay between scans.
 */
public class AutoLLMScanPlugin extends AbstractPlugin {
    // Volatile flag to signal whether the plugin is active.
    private volatile boolean active;
    private Thread scannerThread;
    private final long delayMillis = 3500; // Delay in milliseconds between scans.
    private static final Pattern VAI_PATTERN = Pattern.compile("//\\s*VAI:\\s*(.*)");

    public AutoLLMScanPlugin() {
        this.active = false;
    }

    @Override
    public void setActive(boolean active) {
        System.out.println("AUTO LLM ACTIVE = " + active);
        // Trigger start/stop only when the state changes.
        if (this.active != active) {
            this.active = active;
            if (this.active) {
                startScanning();
            } else {
                stopScanning();
            }
        }
        super.setActive(active);
    }

    @Override
    protected String getIdentifier() {
        return "AutoLLMScan";
    }

    @Override
    protected String getExtension() {
        return "undefined";
    }

    @Override
    protected void actionPerformed(String actionBody) {

    }

    @Override
    public String getFeaturePrompt() {
        return "";
    }

    static boolean paused;

    private void startScanning() {
        scannerThread = new Thread(() -> {
            while (this.active) {
                if (paused) {
                    try {
                        Thread.sleep(100L);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                // Retrieve the current workspace from the App instance.
                File workspace = App.getInstance().getCurrentWorkspace();
                if (workspace != null && workspace.isDirectory()) {
                    try {
                        // Recursively iterate through all files in the workspace.
                        Files.walk(workspace.toPath())
                                .filter(Files::isRegularFile)
                                .forEach(path -> {
                                    String filePath = path.toString().toLowerCase();
                                    // Process only .cs, .java, or .ts files.
                                    if (filePath.endsWith(".cs") || filePath.endsWith(".java") || filePath.endsWith(".ts")) {
                                        try {
                                            List<String> lines = Files.readAllLines(path);
                                            boolean modified = false;
                                            for (int i = 0; i < lines.size(); i++) {
                                                String line = lines.get(i);
                                                Matcher matcher = VAI_PATTERN.matcher(line);
                                                if (matcher.find()) {
                                                    String prompt = matcher.group(1).trim();
                                                    System.out.println("Prompting '" + prompt + "'.");

                                                    // Auto add to context.
                                                    String updatedLine = line.replaceFirst("VAI:", "TODOVAI:");
                                                    lines.set(i, updatedLine);
                                                    modified = true;
                                                    // On the EDT, set the client frame prompt and submit automatically.
                                                    String finalPrompt = prompt;
                                                    SwingUtilities.invokeLater(() -> {
                                                        ClientFrame client = App.getInstance().getClient();
                                                        if (client != null) {
                                                            client.setLLMPrompt("Replace the `//TODOVAI: " + finalPrompt + "` with the code relevant to it's request.");
                                                            paused = true;
                                                            App.getInstance().getActiveFileManager().forceTemporaryContext(path.toFile());
                                                            client.submit(() -> paused = false);
                                                        }
                                                    });
                                                }
                                            }
                                            if (modified) {
                                                Files.write(path, lines);
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        scannerThread.setDaemon(true);
        scannerThread.start();
        System.out.println("[AutoLLMScanPlugin] Started scanning thread.");
    }

    private void stopScanning() {
        System.out.println("[AutoLLMScanPlugin] Stopping scanning thread.");
        if (scannerThread != null) {
            scannerThread.interrupt();
            try {
                scannerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        scannerThread = null;
    }

    @Override
    public String getFeatureDescription() {
        return "Auto-submit VAI comments to the LLM.";
    }
}
