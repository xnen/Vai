package io.improt.vai.backend.plugin.impl;

import io.improt.vai.backend.plugin.AbstractPlugin;
import io.improt.vai.util.VaiUtils;

import javax.swing.*;

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
        JScrollPane scrollPane = VaiUtils.createMessageDialog(actionBody);
        JOptionPane.showMessageDialog(null, scrollPane, "Message from model", JOptionPane.INFORMATION_MESSAGE);
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
                "!EOF";
    }

    @Override
    public String getFeatureDescription() {
        return "Allow LLM to show you a message in a dialog.";
    }


}
