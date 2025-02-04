package io.improt.vai.llm.chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * TypingBubble displays three animated circular dots to indicate that the assistant is “typing”.
 * It is shown while the model request is in progress and is removed when the response arrives.
 */
public class TypingBubble extends JPanel {
    private Timer animationTimer;
    private int dotCount = 3; // Always show 3 dots
    private int animationFrame = 0; // Frame for animation cycle
    private Color[] dotColors;

    public TypingBubble() {
        setLayout(new BorderLayout());
        setOpaque(false);
        initDotColors();

        // Start the animation timer to update the dot colors every 300ms.
        animationTimer = new Timer(300, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateDotColors();
                animationFrame++;
            }
        });
        animationTimer.start();
    }

    private void initDotColors() {
        dotColors = new Color[dotCount];
        for (int i = 0; i < dotCount; i++) {
            dotColors[i] = new Color(120, 120, 120); // Initial dark grey color
        }
    }

    private void updateDotColors() {
        for (int i = 0; i < dotCount; i++) {
            // Cycle through shades of grey/white for each dot with an offset
            int offset = i * 5; // Offset for each dot to create a wave effect
            int cyclePos = (animationFrame + offset) % 20; // Cycle length of 20 frames
            int shade = 120 + (int) (Math.sin(cyclePos * Math.PI / 10) * 80); // Sine wave for smooth animation
            shade = Math.max(100, Math.min(200, shade)); // Clamp value to prevent going too dark or too bright
            dotColors[i] = new Color(shade, shade, shade);
        }
        repaint();
    }

    /**
     * Stops the dot animation.
     */
    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        int dotDiameter = 20;
        int spacing = 10;
        int totalWidth = (dotDiameter * dotCount) + (spacing * (dotCount - 1)) + 20; // Add some padding
        int totalHeight = dotDiameter + 20; // Add some padding
        return new Dimension(totalWidth, totalHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Draw a rounded bubble background using the assistant-style color.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bubbleColor = new Color(80, 80, 80);
        g2.setColor(bubbleColor);
        int arc = 15;
        int width = getWidth();
        int height = getHeight();
        g2.fillRoundRect(0, 0, width, height, arc, arc);


        // Draw the circular dots
        int dotDiameter = 20;
        int spacing = 10;
        int startX = (width - (dotDiameter * dotCount + spacing * (dotCount - 1))) / 2; // Center dots horizontally
        int centerY = height / 2;

        for (int i = 0; i < dotCount; i++) {
            g2.setColor(dotColors[i]);
            int x = startX + (i * (dotDiameter + spacing));
            int y = centerY - (dotDiameter / 2);
            g2.fillOval(x, y, dotDiameter, dotDiameter);
        }


        g2.dispose();
        super.paintComponent(g);
    }
}
