package io.improt.vai.frame.actions;

import io.improt.vai.backend.App;
import io.improt.vai.frame.ClientFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class OpenPathAction implements ActionListener {
    private final ClientFrame client;

    public OpenPathAction(ClientFrame client) {
        this.client = client;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        App backend = App.getInstance();
        String path = JOptionPane.showInputDialog(client, "Enter workspace path:", "Open Path", JOptionPane.PLAIN_MESSAGE);
        if (path != null && !path.trim().isEmpty()) {
            File workspace = new File(path.trim());
            if (workspace.exists() && workspace.isDirectory()) {
                backend.openDirectory(workspace);
                client.getProjectPanel().refreshTree(backend.getCurrentWorkspace());
                client.populateRecentMenu();
            } else {
                JOptionPane.showMessageDialog(client, "Invalid path. Please enter a valid directory.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
