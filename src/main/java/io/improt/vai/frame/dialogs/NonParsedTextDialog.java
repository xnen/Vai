package io.improt.vai.frame.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * A non-blocking, resizable, and aesthetically styled dialog that displays
 * the non-parsed (ignored) portion of the LLM response.
 */
public class NonParsedTextDialog extends JDialog {

    public NonParsedTextDialog(Frame parent, String text) {
        super(parent, "Ignored LLM Response", false); // Non-modal dialog
        initComponents(text);
    }

    private void initComponents(String text) {
        // Create header panel with icon and header text.
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        headerPanel.setBackground(new Color(245, 245, 245));
        
        // You can change or add an icon as needed.
        Icon infoIcon = UIManager.getIcon("OptionPane.informationIcon");
        JLabel iconLabel = new JLabel(infoIcon);
        JLabel headerLabel = new JLabel("Additional LLM response(s)");
        headerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        headerPanel.add(iconLabel);
        headerPanel.add(headerLabel);

        // Create a padded text area for displaying the content.
        JTextArea textArea = new JTextArea(text);
        textArea.setBackground(Color.white);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setBackground(new Color(255, 255, 255));
        textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Embed the text area within a scroll pane.
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        // Create a panel for the close button.
        JButton closeButton = new JButton("Close");
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(245, 245, 245));
        buttonPanel.add(closeButton);

        // Setup the dialog layout.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Final dialog settings.
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        pack();
        setLocationRelativeTo(getParent());
    }

    /**
     * Convenience method to display the dialog.
     *
     * @param text   The non-parsed text to show.
     * @param parent The parent frame; can be null.
     */
    public static void showDialog(String text, Frame parent) {
        NonParsedTextDialog dialog = new NonParsedTextDialog(parent, text);
        dialog.setVisible(true);
    }
}
