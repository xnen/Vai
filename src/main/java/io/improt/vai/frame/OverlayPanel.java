package io.improt.vai.frame;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class OverlayPanel extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
        // Draw background with drop shadow.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int shadowGap = 4;
        int arc = 30;
        // Draw drop shadow.
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(shadowGap, shadowGap, getWidth() - shadowGap, getHeight() - shadowGap, arc, arc);
        // Draw main background.
        g2.setColor(new Color(40, 40, 40, 220));
        g2.fillRoundRect(0, 0, getWidth() - shadowGap, getHeight() - shadowGap, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
