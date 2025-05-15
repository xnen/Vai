package io.improt.vai.llm.chat;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A panel that displays chat bubbles vertically, aligning user bubbles to the right
 * and assistant/image bubbles to the left. Uses BoxLayout for stacking.
 * Relies on the bubble component itself (ChatBubble with CSS max-width)
 * to constrain its width.
 */
public class ChatPanel extends JPanel {
    private final Box verticalBox;
    private final JPanel wrapperPanel; // Panel to hold the verticalBox
    private static final Color BG_DARK = new Color(43, 43, 43); // Match ChatWindow background
    private final List<Component> bubbles = new ArrayList<>(); // Keep track of added bubbles

    public ChatPanel() {
        setLayout(new BorderLayout()); // Main layout remains BorderLayout
        setOpaque(true);
        setBackground(BG_DARK);

        verticalBox = Box.createVerticalBox();

        // Wrapper panel to control the horizontal alignment and width of the verticalBox
        wrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); // Center the box horizontally
        wrapperPanel.setOpaque(true);
        wrapperPanel.setBackground(BG_DARK); // Match background
        wrapperPanel.add(verticalBox);

        // Add wrapperPanel to the NORTH of the BorderLayout. This allows the verticalBox
        // to grow downwards naturally, while FlowLayout keeps it centered horizontally.
        add(wrapperPanel, BorderLayout.NORTH);

        // Add some padding around the entire chat area
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    /**
     * Adds a chat bubble, managing alignment and spacing.
     *
     * @param bubble the chat bubble to add.
     * @param isUser true if the bubble is a user bubble.
     */
    public void addChatBubble(ChatBubble bubble, boolean isUser) {
        addBubble(bubble, isUser);
    }

    /**
     * Adds a generic bubble component (like TypingBubble), handling alignment and spacing.
     *
     * @param bubble the bubble component to add.
     * @param isUser true if the bubble is a user bubble (or should be aligned right).
     */
    public void addBubble(JComponent bubble, boolean isUser) {
        // No longer setting maximum size here - ChatBubble CSS handles text width.
        // Images within ChatBubble might have their own max size set internally.
        // int maxWidth = (int) (getParentWidth() * 0.75); // Max 75% of container width
        // bubble.setMaximumSize(new Dimension(maxWidth, Short.MAX_VALUE)); // REMOVED

        // Create a horizontal box to manage alignment within the vertical stack
        Box horizontalBox = Box.createHorizontalBox();

        if (isUser) {
            horizontalBox.add(Box.createHorizontalGlue()); // Push bubble to the right
            horizontalBox.add(bubble);
            // No strut needed as alignment is handled by glue and panel padding
        } else {
            horizontalBox.add(bubble);
            horizontalBox.add(Box.createHorizontalGlue()); // Push bubble to the left
             // No strut needed
        }

        verticalBox.add(horizontalBox);
        verticalBox.add(Box.createVerticalStrut(12)); // Consistent vertical spacing

        bubbles.add(bubble); // Track the bubble component itself

        revalidate();
        repaint();
    }

    /** Gets the width of the parent container, defaulting if not available. */
    private int getParentWidth() {
         Container parent = getParent();
         // The parent is likely the JViewport of the JScrollPane
         if (parent instanceof JViewport) {
             Container grandParent = parent.getParent(); // Get the JScrollPane itself
             if (grandParent != null) parent = grandParent;
         }
         // Subtract border/padding estimate
         int horizontalPadding = 30; // From ChatPanel border
         JScrollBar verticalScrollBar = null;
         if(parent instanceof JScrollPane) {
             verticalScrollBar = ((JScrollPane) parent).getVerticalScrollBar();
         }
         int scrollbarWidth = (verticalScrollBar != null && verticalScrollBar.isVisible()) ? verticalScrollBar.getWidth() : 0;

         return (parent != null) ? parent.getWidth() - horizontalPadding - scrollbarWidth : 600; // Default guess
    }

    /**
     * Removes a specific bubble component from the panel.
     *
     * @param component The bubble JComponent to remove.
     */
    public void removeBubble(Component component) {
        // Find the horizontal box containing the component
        Component HBoxToRemove = null;
        Component strutToRemove = null;
        int componentIndex = -1;

        Component[] hBoxes = verticalBox.getComponents();
        for (int i = 0; i < hBoxes.length; i++) {
            if (hBoxes[i] instanceof Box) {
                Box hBox = (Box) hBoxes[i];
                for (Component compInHBox : hBox.getComponents()) {
                    if (compInHBox == component) {
                        HBoxToRemove = hBox;
                        componentIndex = i;
                        break;
                    }
                }
            }
            if (HBoxToRemove != null) break;
        }

        // Find the strut following the horizontal box
        if (componentIndex != -1 && componentIndex + 1 < hBoxes.length) {
             if (hBoxes[componentIndex + 1] instanceof Box.Filler) { // Check if it's a strut
                 strutToRemove = hBoxes[componentIndex + 1];
             }
        }

        // Remove the components on the EDT
        final Component finalHBoxToRemove = HBoxToRemove;
        final Component finalStrutToRemove = strutToRemove;
        SwingUtilities.invokeLater(() -> {
            if (finalStrutToRemove != null) {
                verticalBox.remove(finalStrutToRemove);
            }
            if (finalHBoxToRemove != null) {
                verticalBox.remove(finalHBoxToRemove);
            }
            bubbles.remove(component); // Untrack the bubble
            verticalBox.revalidate();
            verticalBox.repaint();
            this.revalidate();
            this.repaint();
        });
    }

    /** Clears all bubbles from the panel. */
    public void clearBubbles() {
         SwingUtilities.invokeLater(() -> {
             verticalBox.removeAll();
             bubbles.clear();
             verticalBox.revalidate();
             verticalBox.repaint();
             this.revalidate();
             this.repaint();
         });
    }

    /** Recalculates layout - typically involves revalidating the container hierarchy. */
    public void recalcLayout() {
        SwingUtilities.invokeLater(() -> {
             verticalBox.revalidate();
             verticalBox.repaint();
             wrapperPanel.revalidate();
             wrapperPanel.repaint();
             this.revalidate();
             this.repaint();
         });
    }
}
