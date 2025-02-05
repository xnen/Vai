package io.improt.vai.frame;

import io.improt.vai.backend.App;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class ConfigureFrame extends JFrame {
    private final JTextField apiKeyField;
    private final JTextField deepseekUrlField;

    public ConfigureFrame(Frame parent) {
        super("Configure");
        // Increase size to comfortably fit two configuration fields
        setSize(400, 180);
        setLayout(new FlowLayout());

        apiKeyField = new JTextField(20);
        deepseekUrlField = new JTextField(20);

        JButton saveButton = new JButton("Save");

        // Load API key from file
        try {
            File file = new File(App.API_KEY);
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String apiKey = br.readLine();
                    apiKeyField.setText(apiKey);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Load DeepSeek Base URL from file, defaulting to current URL if not set
        String defaultDeepSeekUrl = "http://127.0.0.1:11434/v1";
        try {
            File file = new File("deepseek_url.txt");
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String url = br.readLine();
                    if (url != null && !url.isEmpty()) {
                        deepseekUrlField.setText(url);
                    } else {
                        deepseekUrlField.setText(defaultDeepSeekUrl);
                    }
                }
            } else {
                deepseekUrlField.setText(defaultDeepSeekUrl);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            deepseekUrlField.setText(defaultDeepSeekUrl);
        }

        saveButton.addActionListener(e -> {
            // Save API key to file
            try (FileWriter fw = new FileWriter(App.API_KEY)) {
                fw.write(apiKeyField.getText());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // Save DeepSeek Base URL to file
            try (FileWriter fw = new FileWriter("deepseek_url.txt")) {
                fw.write(deepseekUrlField.getText());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            dispose();
        });

        add(new JLabel("OpenAI API Key:"));
        add(apiKeyField);
        add(new JLabel("DeepSeek Base URL:"));
        add(deepseekUrlField);
        add(saveButton);

        setLocationRelativeTo(parent);
        setVisible(true);
    }
}
