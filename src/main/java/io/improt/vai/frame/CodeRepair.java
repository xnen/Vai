package io.improt.vai.frame;

import javax.swing.*;
import java.awt.*;

public class CodeRepair extends JDialog {
    private final JTextArea originalCodeArea;
    private final JTextArea correctedCodeArea;
    private String correctedJson = null;
    
    public CodeRepair(JFrame parent, String jsonString, String exceptionMessage) {
        super(parent, "formatting Repair", true);
        setLayout(new BorderLayout());
        
        // Exception Label
        JTextArea exceptionLabel = new JTextArea("The LLM did not output valid formatting, please correct: " + exceptionMessage);
        exceptionLabel.setEditable(false);
        exceptionLabel.setLineWrap(true);
        exceptionLabel.setWrapStyleWord(true);
        exceptionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(exceptionLabel, BorderLayout.NORTH);
        
        // Split Pane for code areas
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        
        // Original formatting Area (Non-editable)
        originalCodeArea = new JTextArea(jsonString);
        originalCodeArea.setEditable(false);
        originalCodeArea.setLineWrap(true);
        originalCodeArea.setWrapStyleWord(true);
        JScrollPane originalScrollPane = new JScrollPane(originalCodeArea);
        originalScrollPane.setBorder(BorderFactory.createTitledBorder("Original formatting"));
        splitPane.setTopComponent(originalScrollPane);
        
        // Corrected formatting Area (Editable)
        correctedCodeArea = new JTextArea();
        correctedCodeArea.setLineWrap(true);
        correctedCodeArea.setWrapStyleWord(true);
        JScrollPane correctedScrollPane = new JScrollPane(correctedCodeArea);
        correctedScrollPane.setBorder(BorderFactory.createTitledBorder("Corrected formatting"));
        splitPane.setBottomComponent(correctedScrollPane);
        
        add(splitPane, BorderLayout.CENTER);
        
        // Buttons Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            correctedJson = correctedCodeArea.getText().trim();
            if (!correctedJson.isEmpty()) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Corrected text cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
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
    
    public String getCorrectedCode() {
        return correctedJson;
    }
}
