package io.improt.vai.llm.chat;

import javax.swing.*;
import java.awt.*;

/**
 * A panel that displays chat bubbles in a vertical stack using absolute positioning.
 * This example is kept simple, but a custom layout or BoxLayout might be more flexible.
 */
public class ChatPanel extends JPanel {
    private int nextY = 10; // vertical spacing marker for chat bubbles
    // New field to hold the model name overlay.
    private String modelName = "";

    public ChatPanel() {
        // Use a null layout for absolute positioning.
        // One could consider using a more flexible layout if alignment changes are desired.
        setLayout(null);
        // Make the panel fully opaque and give it a dark background so it stands out
        // from the bottom input area.
        setOpaque(true);
        setBackground(new Color(40, 40, 40));
        // Initial size (width from ChatWindow, height will grow as needed).
        setPreferredSize(new Dimension(660, 700));
    }

    /**
     * Setter for model name to be displayed as an overlay.
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
        repaint();
    }
    
    /**
     * Adds a generic bubble component with manual absolute positioning in a vertical flow.
     *
     * @param bubble the bubble component to add.
     * @param isUser true if the bubble is a user bubble.
     */
    public void addBubble(JComponent bubble, boolean isUser) {
        Dimension pref = bubble.getPreferredSize();
        int bubbleWidth = pref.width;
        int bubbleHeight = pref.height;
        bubble.setSize(bubbleWidth, bubbleHeight);

        int panelWidth = getWidth() > 0 ? getWidth() : 660; // fallback if not set
        int margin = 10;
        // Right-align user messages, left-align assistant messages.
        int x = isUser ? (panelWidth - bubbleWidth - margin) : margin;

        bubble.setBounds(x, nextY, bubbleWidth, bubbleHeight);
        add(bubble);

        nextY += bubbleHeight + 10; // space between bubbles
        setPreferredSize(new Dimension(panelWidth, nextY));

        revalidate();
        repaint();
    }

    /**
     * Adds a chat bubble with manual absolute positioning in a vertical flow.
     *
     * @param bubble the chat bubble to add.
     * @param isUser true if the bubble is a user bubble.
     */
    public void addChatBubble(ChatBubble bubble, boolean isUser) {
        addBubble(bubble, isUser);
    }

    /**
     * Override the paint method to draw the overlay after all children are painted,
     * ensuring the model name appears on top.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (modelName != null && !modelName.isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setFont(new Font("Clash Grotesk Light", Font.PLAIN, 12));
            g2.setColor(new Color(60, 60, 60));
            // Draw the model name at position (10, 20)
            g2.drawString(modelName, 10, 20);
            g2.dispose();
        }
    }
    
    /**
     * Recalculates the layout of all bubbles, starting the positioning from y = 10.
     * This method repositions every bubble (ChatBubble or TypingBubble) and updates the internal nextY accordingly.
     */
    public void recalcLayout() {
        int y = 10;
        int panelWidth = getWidth() > 0 ? getWidth() : 660;
        int margin = 10;
        for (Component comp : getComponents()) {
            boolean isUser = false;
            if (comp instanceof ChatBubble) {
                isUser = ((ChatBubble) comp).isUserMessage();
            } else if (comp instanceof TypingBubble) {
                isUser = false; // TypingBubble is always assistant aligned.
            } else {
                continue;
            }
            Dimension pref = comp.getPreferredSize();
            int bubbleWidth = pref.width;
            comp.setBounds(isUser ? (panelWidth - bubbleWidth - margin) : margin, y, bubbleWidth, pref.height);
            y += pref.height + 10;
        }
        this.nextY = y;
        setPreferredSize(new Dimension(panelWidth, y));
        revalidate();
        repaint();
    }
}
