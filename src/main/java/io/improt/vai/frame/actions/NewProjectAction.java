package io.improt.vai.frame.actions;

import io.improt.vai.backend.App;
import io.improt.vai.frame.ClientFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class NewProjectAction implements ActionListener {

    private final ClientFrame client;

    public NewProjectAction(ClientFrame client) {
        this.client = client;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Parent Directory for New Project");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(client);
        if (result == JFileChooser.APPROVE_OPTION) {
            File parentDir = chooser.getSelectedFile();
            String projectName = JOptionPane.showInputDialog(client, "Enter new project name:", "New Project", JOptionPane.PLAIN_MESSAGE);

            if (projectName != null) {
                projectName = projectName.trim();
                if (projectName.isEmpty()) {
                    JOptionPane.showMessageDialog(client, "Project name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                File newProject = new File(parentDir, projectName);
                if (newProject.exists()) {
                    JOptionPane.showMessageDialog(client, "Project \"" + projectName + "\" already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    boolean created = newProject.mkdirs();
                    if (created) {
                        App.getInstance().openDirectory(newProject);
                        JOptionPane.showMessageDialog(client, "Project \"" + projectName + "\" created and opened successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(client, "Failed to create project directory.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }
}
