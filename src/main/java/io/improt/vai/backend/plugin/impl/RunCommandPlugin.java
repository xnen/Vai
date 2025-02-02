package io.improt.vai.backend.plugin.impl;

import io.improt.vai.backend.App;
import io.improt.vai.backend.plugin.AbstractPlugin;
import io.improt.vai.frame.ClientFrame;
import io.improt.vai.util.VaiUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class RunCommandPlugin extends AbstractPlugin {
    @Override
    protected String getIdentifier() {
        return "RUN_COMMAND";
    }

    @Override
    protected String getExtension() {
        return "run";
    }

    @Override
    protected void actionPerformed(String actionBody) {
        handleRunCommand(actionBody);
    }
    
    @Override
    public String getFeaturePrompt() {
        return "You can suggest a shell command (with multiple newline commands allowed) to run, by utilizing the path [RUN_COMMAND] with the `run` lang. The dialog will allow you to review, edit and confirm the commands, while displaying the current working directory. On Linux, the command will launch in a pop-up terminal window for live output.\n" +
                "\n" +
                "EXAMPLE:\n" +
                "[RUN_COMMAND]\n" +
                "```run\n" +
                "echo \"Hello, world!\"\n" +
                "echo \"This is a second command\"\n" +
                "```\n" +
                "!EOF\n" +
                "You MUST write comments explaining each of the commands you're running.";
    }

    @Override
    public String getFeatureDescription() {
        return "Allow LLM to run shell commands (with approval)";
    }

    /**
     * Handles the RUN_COMMAND functionality by prompting the user and executing the command if approved.
     *
     * @param command The shell command(s) to execute.
     */
    private void handleRunCommand(String command) {
        ClientFrame mainWindow = App.getInstance().getClient();

        // Create a panel with a multi-line text area for command editing and display current working directory
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        JLabel instructionLabel = new JLabel("The LLM would like to run the following command(s):");
        panel.add(instructionLabel, BorderLayout.NORTH);
        
        JTextArea commandArea = new JTextArea(command, 8, 40);
        commandArea.setLineWrap(true);
        commandArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(commandArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        String cwd = System.getProperty("user.dir");
        JLabel cwdLabel = new JLabel("Current Working Directory: " + cwd);
        panel.add(cwdLabel, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(
                mainWindow,
                panel,
                "Run Command Approval",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            String updatedCommand = commandArea.getText();
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                // On Linux, launch a terminal window for live command execution with a pause at the end.
                try {
                    // Append commands to pause the terminal until the user presses Enter.
                    String finalCommand = updatedCommand + "\necho ''\nread -p 'Press Enter to exit...'";
                    ProcessBuilder pb = new ProcessBuilder("x-terminal-emulator", "-e", "bash", "-ic", finalCommand);
                    pb.start();
                } catch (IOException e) {
                    // If launching the terminal fails, show an error dialog.
                    JOptionPane.showMessageDialog(
                            mainWindow,
                            "Failed to launch terminal for command execution:\n" + e.getMessage(),
                            "Execution Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            } else {
                // For non-Linux systems, execute command and show output in a scrollable dialog
                String output = VaiUtils.executeShellCommand(updatedCommand);
                JTextArea outputArea = new JTextArea(output);
                outputArea.setEditable(false);
                outputArea.setLineWrap(true);
                outputArea.setWrapStyleWord(true);
                JScrollPane outputScroll = new JScrollPane(outputArea);
                outputScroll.setPreferredSize(new Dimension(600, 400));
                JOptionPane.showMessageDialog(
                        mainWindow,
                        outputScroll,
                        "Command Output",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        } else {
            // User denied the command execution
            JScrollPane denialScroll = VaiUtils.createMessageDialog("Command execution was denied by the user.");
            JOptionPane.showMessageDialog(mainWindow, denialScroll, "Command Denied", JOptionPane.WARNING_MESSAGE);
        }
    }
}
