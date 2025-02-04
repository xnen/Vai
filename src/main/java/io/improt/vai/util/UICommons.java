package io.improt.vai.util;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

public class UICommons {

    /**
     * Applies rounded corners to the given Window.
     *
     * @param window the window to modify.
     * @param arcWidth the horizontal diameter of the arc.
     * @param arcHeight the vertical diameter of the arc.
     */
    public static void applyRoundedCorners(Window window, int arcWidth, int arcHeight) {
        try {
            window.setShape(new RoundRectangle2D.Double(0, 0, window.getWidth(), window.getHeight(), arcWidth, arcHeight));
        } catch (UnsupportedOperationException e) {
            // Platform does not support shaped windows.
        }
    }

    /**
     * A custom border that draws a rounded rectangle outline.
     */
    public static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        public RoundedBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(color);
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = insets.top = insets.bottom = radius / 2;
            return insets;
        }
    }
}
