package io.improt.vai.frame.actions;

import io.improt.vai.backend.App;
import io.improt.vai.frame.ClientFrame;
import io.improt.vai.util.Constants;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class TempProjectAction implements ActionListener {

    private final ClientFrame client;

    public TempProjectAction(ClientFrame client) {
        this.client = client;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File parentDir = new File(Constants.TEMP_PROJECT_PATH);
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        // new uuid, so it's unique
        String uuid = java.util.UUID.randomUUID().toString();
        String projectName = "temp-project-" + uuid;
        projectName = projectName.trim();

        File newProject = new File(parentDir, projectName);

        if (!newProject.exists()) {
            boolean created = newProject.mkdirs();

            if (created) {
                App.getInstance().openWorkspace(newProject);
                JOptionPane.showMessageDialog(client, "Project \"" + projectName + "\" created and opened successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(client, "Failed to create project directory.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
