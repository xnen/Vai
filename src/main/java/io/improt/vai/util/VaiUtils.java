package io.improt.vai.util;

import io.improt.vai.backend.App;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Random methods that just kinda do things.
 */
public class VaiUtils {

    public static boolean isExecutable(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".sh") || name.endsWith(".exe") || name.endsWith(".bat"); // Extend as needed
    }

    /**
     * Creates a JScrollPane containing the provided message for display purposes.
     *
     * @param newContents The message content.
     * @return A JScrollPane with the message.
     */
    @NotNull
    public static JScrollPane createMessageDialog(String newContents) {
        JTextArea messageArea = new JTextArea(newContents);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        return scrollPane;
    }

    public static boolean doSecurityValidation(File targetFile, String contents) {
        try {
            String workspaceCanonicalPath = App.getInstance().getCurrentWorkspace().getCanonicalPath();
            String targetCanonicalPath = targetFile.getCanonicalPath();

            if (!targetCanonicalPath.startsWith(workspaceCanonicalPath)) {
                // Prompt the user for permission to create the file outside the project directory
                JPanel panel = createSecurityPanel(targetCanonicalPath, contents);

                int result = JOptionPane.showConfirmDialog(
                        App.getInstance().getClient(),
                        panel,
                        "File Creation Approval",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (result != JOptionPane.YES_OPTION) {
                    // User declined, skip writing this file
                    return false;
                }
            }
        } catch (IOException e) {
            // On error, log the exception and skip writing
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @NotNull
    private static JPanel createSecurityPanel(String targetCanonicalPath, String newContents) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel("Allow file creation to '" + targetCanonicalPath + "'?");

        // Text area with the new contents, initially collapsed
        JTextArea textArea = new JTextArea(newContents);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVisible(false);

        JButton toggleButton = new JButton("Show File Contents");
        toggleButton.addActionListener(e -> {
            boolean isVisible = scrollPane.isVisible();
            scrollPane.setVisible(!isVisible);
            toggleButton.setText(isVisible ? "Show File Contents" : "Hide File Contents");
            panel.revalidate();
            panel.repaint();
        });

        panel.add(label, BorderLayout.NORTH);
        panel.add(toggleButton, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Executes a shell command in the current workspace directory and returns the output.
     *
     * @param command The shell command to execute.
     * @return The combined output and error streams from the command execution.
     */
    public static String executeShellCommand(String command) {
        if (command == null || command.isEmpty()) {
            return "";
        }

        StringBuilder outputBuilder = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd.exe", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }
            pb.directory(App.getInstance().getCurrentWorkspace());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            outputBuilder.append("\n").append("Process exited with code ").append(exitCode).append(".");

        } catch (IOException | InterruptedException e) {
            outputBuilder.append("An error occurred while executing the command: ").append(e.getMessage());
        }
        return outputBuilder.toString();
    }
}
