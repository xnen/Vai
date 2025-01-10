package io.improt.vai.frame;

import javax.swing.*;
import java.awt.*;

public class JsonRepair extends JDialog {
    private final JTextArea originalJsonArea;
    private final JTextArea correctedJsonArea;
    private String correctedJson = null;
    
    public JsonRepair(JFrame parent, String jsonString, String exceptionMessage) {
        super(parent, "JSON Repair", true);
        setLayout(new BorderLayout());
        
        // Exception Label
        JLabel exceptionLabel = new JLabel("The LLM did not output valid JSON, please correct: " + exceptionMessage);
        exceptionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(exceptionLabel, BorderLayout.NORTH);
        
        // Split Pane for JSON areas
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        
        // Original JSON Area (Non-editable)
        originalJsonArea = new JTextArea(jsonString);
        originalJsonArea.setEditable(false);
        originalJsonArea.setLineWrap(true);
        originalJsonArea.setWrapStyleWord(true);
        JScrollPane originalScrollPane = new JScrollPane(originalJsonArea);
        originalScrollPane.setBorder(BorderFactory.createTitledBorder("Original JSON"));
        splitPane.setTopComponent(originalScrollPane);
        
        // Corrected JSON Area (Editable)
        correctedJsonArea = new JTextArea();
        correctedJsonArea.setLineWrap(true);
        correctedJsonArea.setWrapStyleWord(true);
        JScrollPane correctedScrollPane = new JScrollPane(correctedJsonArea);
        correctedScrollPane.setBorder(BorderFactory.createTitledBorder("Corrected JSON"));
        splitPane.setBottomComponent(correctedScrollPane);
        
        add(splitPane, BorderLayout.CENTER);
        
        // Buttons Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            correctedJson = correctedJsonArea.getText().trim();
            if (!correctedJson.isEmpty()) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Corrected JSON cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            correctedJson = null;
            dispose();
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        setSize(600, 400);
        setLocationRelativeTo(parent);
    }
    
    public String getCorrectedJson() {
        return correctedJson;
    }
}
