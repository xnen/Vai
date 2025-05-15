package io.improt.vai.util;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

public class UICommons {

    /**
     * Applies rounded corners to the given Window if supported by the platform.
     * Note: This might not work consistently across all OS and Java versions.
     * Consider using libraries like FlatLaf for more robust custom window decorations.
     *
     * @param window the window to modify.
     * @param arcWidth the horizontal diameter of the arc.
     * @param arcHeight the vertical diameter of the arc.
     */
    public static void applyRoundedCorners(Window window, int arcWidth, int arcHeight) {
        // Ensure window is displayable and not native decorated if possible
        if (window.isDisplayable() && window instanceof Frame && ((Frame) window).isUndecorated() || window instanceof JDialog && ((JDialog) window).isUndecorated()) {
            try {
                 // AWTUtilities is non-standard API, checking existence first (though deprecated/removed)
                 // Class<?> awtUtils = Class.forName("com.sun.awt.AWTUtilities");
                 // Method setWindowShape = awtUtils.getMethod("setWindowShape", Window.class, Shape.class);
                 // setWindowShape.invoke(null, window, new RoundRectangle2D.Double(0, 0, window.getWidth(), window.getHeight(), arcWidth, arcHeight));

                 // Standard Java 9+ API
//                 if (window.isShapingSupported()) {
                     window.setShape(new RoundRectangle2D.Double(0, 0, window.getWidth(), window.getHeight(), arcWidth, arcHeight));
//                 } else {
//                     System.err.println("Window shaping not supported by the platform or L&F.");
//                 }

            } catch (Exception e) {
                System.err.println("Failed to apply rounded corners: " + e.getMessage());
                // Fallback or ignore if shaping fails
            }
        } else if (!window.isDisplayable()) {
             System.err.println("Cannot apply shape to non-displayable window.");
        } else {
             System.err.println("Cannot apply shape to decorated window.");
        }
    }

    /**
     * A custom border that draws a rounded rectangle outline.
     * Better suited for components *inside* a window.
     */
    public static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;
        private final Insets insets;

        public RoundedBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
            this.insets = new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            // Adjust drawing area to be within the component bounds considering thickness
            int adjustedX = x + thickness / 2;
            int adjustedY = y + thickness / 2;
            int adjustedWidth = width - thickness;
            int adjustedHeight = height - thickness;
            g2.drawRoundRect(adjustedX, adjustedY, adjustedWidth, adjustedHeight, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return getBorderInsets(c, new Insets(0,0,0,0)); // Reuse logic
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            // Provide insets based on radius/thickness to prevent content overlap
            int insetVal = Math.max(thickness, radius / 3) + 2; // Heuristic for padding
            insets.left = insets.right = insets.top = insets.bottom = insetVal;
            return insets;
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
