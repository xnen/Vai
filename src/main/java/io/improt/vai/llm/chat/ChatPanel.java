package io.improt.vai.llm.chat;

import javax.swing.*;
import java.awt.*;

/**
 * A panel that displays chat bubbles in a vertical stack using absolute positioning.
 * This example is kept simple, but a custom layout or BoxLayout might be more flexible.
 */
public class ChatPanel extends JPanel {
    private int nextY = 10; // vertical spacing marker for chat bubbles

    public ChatPanel() {
        // Use a null layout for absolute positioning.
        // One could consider using a more flexible layout if alignment changes are desired.
        setLayout(null);
        // Make the panel fully opaque and give it a dark background, so it stands out
        // from the bottom input area.
        setOpaque(true);
        setBackground(new Color(40, 40, 40));
        // Initial size (width from ChatWindow, height will grow as needed).
        setPreferredSize(new Dimension(660, 700));
    }

    /**
     * Adds a chat bubble with manual absolute positioning in a vertical flow.
     * 
     * @param bubble the chat bubble to add
     * @param isUser true if the bubble is a user bubble
     */
    public void addChatBubble(ChatBubble bubble, boolean isUser) {
        // Use the bubble's preferred size directly (the bubble itself limits width).
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
}
