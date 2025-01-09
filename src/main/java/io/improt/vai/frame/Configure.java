package io.improt.vai.frame;

import io.improt.vai.backend.App;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class Configure extends JFrame {
    private JTextField apiKeyField;
    private JButton saveButton;

    public Configure(Frame parent) {
        super("Configure");
        setSize(300, 120);
        setLayout(new FlowLayout());

        apiKeyField = new JTextField(20);
        saveButton = new JButton("Save");

        // Load API key from file
        try {
            File file = new File(App.API_KEY);
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String apiKey = br.readLine();
                apiKeyField.setText(apiKey);
                br.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        saveButton.addActionListener(e -> {
            try {
                FileWriter fw = new FileWriter(new File(App.API_KEY));
                fw.write(apiKeyField.getText());
                fw.close();
                dispose();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        add(new JLabel("OpenAI API Key:"));
        add(apiKeyField);
        add(saveButton);

        setLocationRelativeTo(parent);
        setVisible(true);
    }
}