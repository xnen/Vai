package io.improt.vai.frame;

import javax.swing.JPanel;
import java.awt.*;

public class ResourceGridPanel extends JPanel {
    public ResourceGridPanel() {
        setOpaque(false);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int shadowGap = 2;
        int arc = 20;
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillRoundRect(shadowGap, shadowGap, getWidth() - shadowGap, getHeight() - shadowGap, arc, arc);
        g2.setColor(new Color(50, 50, 50, 220));
        g2.fillRoundRect(0, 0, getWidth() - shadowGap, getHeight() - shadowGap, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
