package io.improt.vai.backend.plugin.impl;

import io.improt.vai.backend.App;
import io.improt.vai.backend.plugin.AbstractPlugin;
import io.improt.vai.frame.ClientFrame;
import io.improt.vai.util.VaiUtils;

import javax.swing.*;
import java.awt.*;

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
        return "You can suggest a shell command to run, by utilizing the path [RUN_COMMAND] rather than a file path, with the `run` lang. It will prompt the user to run the command, and then respond with the output.\n" +
                "\n" +
                "EXAMPLE:\n" +
                "[RUN_COMMAND]\n" +
                "```run\n" +
                "echo \"Hello, world!\"\n" +
                "```\n" +
                "!EOF";
    }

    @Override
    public String getFeatureDescription() {
        return "Allow LLM to run a command";
    }

    /**
     * Handles the RUN_COMMAND functionality by prompting the user and executing the command if approved.
     *
     * @param command The shell command to execute.
     */
    private void handleRunCommand(String command) {
        ClientFrame mainWindow = App.getInstance().getClient();

        // Prompt the user with the command and options to approve or deny
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel("The LLM would like to run this command:");
        JTextField textField = new JTextField(command);
        textField.setEditable(false);
        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                mainWindow,
                panel,
                "Run Command Approval",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            // Execute the command
            String output = VaiUtils.executeShellCommand(command);
            // Display the output
            JTextArea textArea = new JTextArea(output);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 400));
            JOptionPane.showMessageDialog(
                    mainWindow,
                    scrollPane,
                    "Command Output",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            // User denied the command execution
            JScrollPane scrollPane = VaiUtils.createMessageDialog("Command execution was denied by the user.");
            JOptionPane.showMessageDialog(null, scrollPane, "Command Denied", JOptionPane.WARNING_MESSAGE);
        }
    }
}
