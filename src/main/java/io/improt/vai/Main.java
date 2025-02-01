package io.improt.vai;

import io.improt.vai.frame.ClientFrame;
import javax.swing.*;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;

public class Main {
    public static void main(String[] args) {
        try {
            // Set the modern flat look and feel
            UIManager.setLookAndFeel(new FlatLightLaf());
            // Global UIManager customizations for a sleek, modern appearance
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("Component.focusWidth", 2);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("Menu.font", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("MenuBar.background", Color.decode("#FFFFFF"));
            UIManager.put("MenuBar.foreground", Color.decode("#202124"));
            
            // Override default selection highlight colors with a nice lighter blue (#B3D4FC)
            Color lightBlue = Color.decode("#B3D4FC");
            UIManager.put("Table.selectionBackground", lightBlue);
            UIManager.put("List.selectionBackground", lightBlue);
            UIManager.put("Tree.selectionBackground", lightBlue);
            UIManager.put("ComboBox.selectionBackground", lightBlue);
            UIManager.put("TextField.selectionBackground", lightBlue);
            UIManager.put("TextArea.selectionBackground", lightBlue);
            UIManager.put("EditorPane.selectionBackground", lightBlue);
            UIManager.put("TextPane.selectionBackground", lightBlue);
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex.getMessage());
        }
        SwingUtilities.invokeLater(ClientFrame::new);
    }
}
