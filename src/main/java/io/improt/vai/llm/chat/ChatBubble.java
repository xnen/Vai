package io.improt.vai.llm.chat;

import io.improt.vai.util.UICommons;
import javax.swing.*;
import java.awt.*;

/**
 * A chat bubble component that can display either text or an image, with a rounded, painted background.
 */
public class ChatBubble extends JPanel {
    private final boolean isImage;
    private final boolean isUser;
    private final JLabel contentLabel;

    /**
     * Constructs a text-based chat bubble.
     * 
     * @param message the message content.
     * @param isUser  true if this bubble represents a user message.
     */
    public ChatBubble(String message, boolean isUser) {
        this.isImage = false;
        this.isUser = isUser;
        setLayout(new BorderLayout());
        setOpaque(false);

        // Wrap message in HTML with a max-width for text wrapping.
        String htmlMessage = "<html><div style='max-width:400px;'>"
                + message.replaceAll("\n", "<br>")
                + "</div></html>";
        contentLabel = new JLabel(htmlMessage);
        contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentLabel.setOpaque(false);
        contentLabel.setForeground(Color.WHITE);

        // Provide some padding inside the bubble.
        JPanel labelContainer = new JPanel(new BorderLayout());
        labelContainer.setOpaque(false);
        labelContainer.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        labelContainer.add(contentLabel, BorderLayout.CENTER);

        add(labelContainer, BorderLayout.CENTER);
    }

    /**
     * Constructs an image chat bubble (displays a 100x100 preview).
     *
     * @param imageIcon the image icon to display.
     * @param isUser    true if this bubble represents a user message.
     */
    public ChatBubble(ImageIcon imageIcon, boolean isUser) {
        this.isImage = true;
        this.isUser = isUser;
        setLayout(new BorderLayout());
        setOpaque(false);

        contentLabel = new JLabel(imageIcon);
        contentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentLabel.setVerticalAlignment(SwingConstants.CENTER);
        contentLabel.setOpaque(false);

        // Provide a slight padding around the image.
        JPanel labelContainer = new JPanel(new BorderLayout());
        labelContainer.setOpaque(false);
        labelContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        labelContainer.add(contentLabel, BorderLayout.CENTER);

        add(labelContainer, BorderLayout.CENTER);

        // Force the image bubble to remain 100x100 for the icon area.
        setPreferredSize(new Dimension(100, 100));
    }

    /**
     * Override to paint a rounded-rectangle background for the bubble.
     */
    @Override
    protected void paintComponent(Graphics g) {
        // Use anti-aliased drawing for smoother corners.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Choose a different background color if it's the user's bubble vs. assistant.
        // Just slightly differentiate them.
        Color bubbleColor = isUser ? new Color(70, 70, 90) : new Color(80, 80, 80);
        g2.setColor(bubbleColor);

        int arc = 15;
        int width = getWidth();
        int height = getHeight();
        g2.fillRoundRect(0, 0, width, height, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Provide a preferred size that accounts for the label's wrapping and some padding.
     * This fix constrains the label's width when recalculating its preferred size, ensuring
     * that very long text correctly wraps and the bubble's height grows.
     */
    @Override
    public Dimension getPreferredSize() {
        if (isImage) {
            // If it's an image bubble, keep the existing forced size.
            return super.getPreferredSize();
        } else {
            // For text bubbles, compute size based on a maximum width.
            // These values match the padding applied in the constructor.
            int horizontalPadding = 12 + 12; // left and right padding
            int verticalPadding = 10 + 10;   // top and bottom padding
            int maxBubbleWidth = 400;
            int maxTextWidth = maxBubbleWidth - horizontalPadding;
            
            // Force the content label to use the max width for proper text wrapping.
            contentLabel.setSize(new Dimension(maxTextWidth, Short.MAX_VALUE));
            Dimension labelPref = contentLabel.getPreferredSize();
            
            int w = Math.min(labelPref.width + horizontalPadding, maxBubbleWidth);
            int h = labelPref.height + verticalPadding;
            return new Dimension(w, h);
        }
    }
}
