package io.improt.vai.llm.chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

/**
 * Displays an animated typing indicator with dots, styled for the modern theme.
 */
public class TypingBubble extends JPanel {
    private Timer animationTimer;
    private final int dotCount = 3;
    private int animationFrame = 0;
    private final Color[] dotColors;
    private final Color bubbleColor = new Color(74, 74, 74); // Match assistant bubble color
    private final Color dotBaseColor = new Color(120, 120, 120);
    private final Color dotHighlightColor = new Color(180, 180, 180);
    private static final int BUBBLE_ARC = 25; // Match ChatBubble roundness

    public TypingBubble() {
        setOpaque(false); // Panel is transparent, background painted
        dotColors = new Color[dotCount];
        initDotColors();

        animationTimer = new Timer(250, e -> { // Faster animation
            updateDotColors();
            animationFrame++;
            repaint(); // Repaint the panel to show updated dots
        });
        animationTimer.start();

        // Set preferred size based on dots and padding
        setPreferredSize(new Dimension(80, 45)); // Fixed preferred size
    }

    private void initDotColors() {
        for (int i = 0; i < dotCount; i++) {
            dotColors[i] = dotBaseColor;
        }
    }

    private void updateDotColors() {
        // Simple sequential highlight animation
        int highlightIndex = animationFrame % dotCount;
        for (int i = 0; i < dotCount; i++) {
            dotColors[i] = (i == highlightIndex) ? dotHighlightColor : dotBaseColor;
        }
    }

    public void stopAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        // Provide a fixed preferred size for consistent layout
        return new Dimension(80, 40); // Width for 3 dots + spacing, standard height
    }

     @Override
    public Dimension getMaximumSize() {
        // Allow it to shrink but not grow beyond preferred size vertically
        return new Dimension(super.getMaximumSize().width, getPreferredSize().height);
    }


    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the rounded background bubble
        g2.setColor(bubbleColor);
        g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), BUBBLE_ARC, BUBBLE_ARC));

        // Draw the animated dots centered within the bubble
        int dotDiameter = 8;
        int spacing = 6;
        int totalDotsWidth = (dotDiameter * dotCount) + (spacing * (dotCount - 1));
        int startX = (getWidth() - totalDotsWidth) / 2;
        int centerY = getHeight() / 2;

        for (int i = 0; i < dotCount; i++) {
            g2.setColor(dotColors[i]);
            int x = startX + (i * (dotDiameter + spacing));
            int y = centerY - (dotDiameter / 2);
            g2.fillOval(x, y, dotDiameter, dotDiameter);
        }

        g2.dispose();
        // No super.paintComponent(g) as we handle all painting
    }
}
