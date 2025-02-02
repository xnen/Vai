package io.improt.vai.frame.dialogs;

import io.improt.vai.util.MessageHistoryManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A non-modal, resizable dialog that displays model response messages.
 * It includes a top navigation bar with flat-designed navigation buttons ("<" for previous and ">" for next)
 * and a label indicating the current message number in the history (e.g., "Message 2 of 2").
 * The remainder of the dialog is occupied by an HTML canvas (JEditorPane) displaying the message.
 */
public class ResizableMessageHistoryDialog extends JDialog {
    private final MessageHistoryManager messageHistoryManager;
    private final JEditorPane messageDisplay;
    private final JLabel messageIndexLabel;

    public ResizableMessageHistoryDialog(MessageHistoryManager historyManager) {
        // Non-modal dialog so it doesn't block the main application.
        super((Frame) null, "Message from Model", false);
        this.messageHistoryManager = historyManager;
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        // Update default dialog dimensions.
        setSize(770, 475);
        setLocationRelativeTo(null);
        setResizable(true);
        getContentPane().setBackground(Color.decode("#F1F3F4"));

        // Create the top navigation bar with fixed height.
        JPanel navigationBar = new JPanel(new BorderLayout());
        navigationBar.setBackground(Color.decode("#E0E0E0"));
        navigationBar.setPreferredSize(new Dimension(0, 25));

        // Create navigation buttons with flat design.
        JButton previousButton = new JButton("<");
        JButton nextButton = new JButton(">");
        
        styleNavButton(previousButton);
        styleNavButton(nextButton);

        // Create label to display message index info.
        messageIndexLabel = new JLabel("", SwingConstants.CENTER);
        messageIndexLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageIndexLabel.setForeground(Color.decode("#202124"));

        // Add components to navigation bar.
        navigationBar.add(previousButton, BorderLayout.WEST);
        navigationBar.add(messageIndexLabel, BorderLayout.CENTER);
        navigationBar.add(nextButton, BorderLayout.EAST);
        add(navigationBar, BorderLayout.NORTH);

        // Create an editor pane for displaying messages.
        messageDisplay = new JEditorPane();
        messageDisplay.setContentType("text/html");
        messageDisplay.setEditable(false);
        // Set initial content.
        updateDisplayedMessage(messageHistoryManager.getCurrentMessage());

        // Use a scroll pane for the message display; it spans the entire remaining area.
        JScrollPane scrollPane = new JScrollPane(messageDisplay);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // Navigation button action listeners.
        previousButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String previousMessage = messageHistoryManager.getPreviousMessage();
                updateDisplayedMessage(previousMessage);
            }
        });

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nextMessage = messageHistoryManager.getNextMessage();
                updateDisplayedMessage(nextMessage);
            }
        });
    }

    /**
     * Applies flat styling to navigation buttons.
     * @param button the JButton to style.
     */
    private void styleNavButton(JButton button) {
        button.setFocusPainted(false);
        button.setBackground(Color.decode("#FFFFFF"));
        button.setForeground(Color.decode("#202124"));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        // Adjusted padding for a smaller bar.
        button.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
    }

    /**
     * Updates the editor pane to display the provided message,
     * and updates the navigation label to indicate the current message position.
     *
     * @param message the message to display.
     */
    private void updateDisplayedMessage(String message) {
        resetHTMLStyling(messageDisplay);
        messageDisplay.setCaretPosition(0);
        messageDisplay.setText(message);
        updateMessageIndexLabel();
    }

    /**
     * Updates the message index label to show current message position in the history.
     * Assumes that MessageHistoryManager provides getCurrentIndex() (0-based) and getTotalMessages() methods.
     */
    private void updateMessageIndexLabel() {
        // Adjust the displayed index to be 1-based.
        int current = messageHistoryManager.getCurrentIndex() + 1;
        int total = messageHistoryManager.getTotalMessages();
        messageIndexLabel.setText("Message " + current + " of " + total);
    }

    public static void resetHTMLStyling(JEditorPane editorPane) {
        // Create a blank HTMLDocument (resets all CSS and styling)
        HTMLDocument blankDocument = (HTMLDocument) new HTMLEditorKit().createDefaultDocument();
        editorPane.setDocument(blankDocument); // Attach the new document
        editorPane.setText(""); // Clear any content
    }
}
