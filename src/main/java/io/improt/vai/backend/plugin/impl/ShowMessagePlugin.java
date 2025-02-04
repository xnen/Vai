package io.improt.vai.backend.plugin.impl;

import io.improt.vai.backend.App;
import io.improt.vai.backend.plugin.AbstractPlugin;
import io.improt.vai.util.MessageHistoryManager;
import io.improt.vai.frame.dialogs.ResizableMessageHistoryDialog;

import javax.swing.*;
import java.io.File;

/**
 * Updated ShowMessagePlugin to use a resizable and non-blocking message dialog
 * that supports navigation through message history.
 */
public class ShowMessagePlugin extends AbstractPlugin {
    @Override
    protected String getIdentifier() {
        return "SHOW_MESSAGE";
    }

    @Override
    protected String getExtension() {
        return "chat";
    }

    @Override
    protected void actionPerformed(String actionBody) {
        // Ensure a current workspace exists to store message history.
        File workspace = App.getInstance().getCurrentWorkspace();
        if (workspace == null) {
            JOptionPane.showMessageDialog(null, "Cannot show message as a workspace is not open!?", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Initialize the MessageHistoryManager for the current workspace.
        MessageHistoryManager historyManager = new MessageHistoryManager(workspace);
        // Add the new message from the model into history.
        historyManager.addMessage(actionBody);

        // Create and show a custom non-blocking, resizable dialog with navigation.
        SwingUtilities.invokeLater(() -> {
            ResizableMessageHistoryDialog dialog = new ResizableMessageHistoryDialog(historyManager);
            dialog.setVisible(true);
        });
    }
    
    @Override
    public String getFeaturePrompt() {
        return "You can also send message responses, by utilizing the path [SHOW_MESSAGE] rather than a file path, with the `chat` lang.\n" +
                "\n" +
                "EXAMPLE:\n" +
                "[SHOW_MESSAGE]\n" +
                "```chat\n" +
                "Your request is impossible, for the following reason: <reason>\n" +
                "```\n" +
                "!EOF\n" +
                "Your message must be a full HTML document (<html><head>etc...), as it's displayed in a JEditorPane with text/html.";
    }

    @Override
    public String getFeatureDescription() {
        return "Allow LLM to show you a message in a dialog.";
    }
}
