package io.improt.vai.frame.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AnimationPanel extends JPanel {

    private static final int PANEL_WIDTH = 480;
    private static final int PANEL_HEIGHT = 150;
    private static final int FOLDER_WIDTH = 60;
    private static final int FOLDER_HEIGHT = 50;
    private static final int PAPER_WIDTH = 20;
    private static final int PAPER_HEIGHT = 25;
    private static final int CLOUD_WIDTH = 70;
    private static final int CLOUD_HEIGHT = 50;

    private final Point sourceFolderPos = new Point(30, PANEL_HEIGHT / 2 - FOLDER_HEIGHT / 2);
    private final Point targetCloudPos = new Point(PANEL_WIDTH - 30 - CLOUD_WIDTH, PANEL_HEIGHT / 2 - CLOUD_HEIGHT / 2);

    private final List<PaperAnimation> activePapers = Collections.synchronizedList(new ArrayList<>());
    private final Timer animationTimer;

    private Image paperImage; // Cache the drawn paper

    public AnimationPanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(230, 230, 230));

        animationTimer = new Timer(30, e -> {
            updateAnimations();
            repaint();
        });
        createPaperImage();
    }

    private void createPaperImage() {
        paperImage = createImage(PAPER_WIDTH, PAPER_HEIGHT);
        if (paperImage != null) {
            Graphics g = paperImage.getGraphics();
            if (g instanceof Graphics2D) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, PAPER_WIDTH, PAPER_HEIGHT);
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawRect(0, 0, PAPER_WIDTH - 1, PAPER_HEIGHT - 1);
                // Tiny lines for text illusion
                g2d.setColor(new Color(200, 200, 200));
                for (int i = 0; i < 4; i++) {
                    g2d.drawLine(3, 5 + i * 5, PAPER_WIDTH - 4, 5 + i * 5);
                }
            }
            g.dispose();
        }
    }


    public void startProcessingFile(String filePath) {
        SwingUtilities.invokeLater(() -> {
            activePapers.add(new PaperAnimation(filePath, sourceFolderPos, targetCloudPos));
            if (!animationTimer.isRunning()) {
                animationTimer.start();
            }
        });
    }

    public void finishProcessingFile(String filePath, boolean success) {
         SwingUtilities.invokeLater(() -> {
            for (PaperAnimation paper : activePapers) {
                if (Objects.equals(paper.filePath, filePath) && paper.isInFlight()) {
                    paper.markForCompletion(success); // Paper will animate to target then fade
                    break;
                }
            }
        });
    }

    private void updateAnimations() {
        boolean hasActiveAnimation = false;
        synchronized (activePapers) {
            List<PaperAnimation> toRemove = new ArrayList<>();
            for (PaperAnimation paper : activePapers) {
                paper.update();
                if (!paper.isDone()) {
                    hasActiveAnimation = true;
                } else {
                    toRemove.add(paper);
                }
            }
            activePapers.removeAll(toRemove);
        }

        if (!hasActiveAnimation && animationTimer.isRunning()) {
            // Optional: Stop timer if no active animations, but dialog closure might handle this
            // animationTimer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawFolderIcon(g2d, sourceFolderPos.x, sourceFolderPos.y, FOLDER_WIDTH, FOLDER_HEIGHT, new Color(255, 215, 100));
        drawCloudIcon(g2d, targetCloudPos.x, targetCloudPos.y, CLOUD_WIDTH, CLOUD_HEIGHT, new Color(170, 210, 255));

        synchronized (activePapers) {
            for (PaperAnimation paper : activePapers) {
                paper.draw(g2d);
            }
        }
        g2d.dispose();
    }

    private void drawFolderIcon(Graphics2D g, int x, int y, int w, int h, Color color) {
        g.setColor(color);
        g.fill(new RoundRectangle2D.Double(x, y + h * 0.1, w, h * 0.9, 10, 10));
        g.fill(new RoundRectangle2D.Double(x + w * 0.05, y, w * 0.4, h * 0.2, 8, 8)); // Tab
        g.setColor(color.darker());
        g.draw(new RoundRectangle2D.Double(x, y + h * 0.1, w, h * 0.9, 10, 10));
        g.draw(new RoundRectangle2D.Double(x + w * 0.05, y, w * 0.4, h * 0.2, 8, 8));
    }

    private void drawCloudIcon(Graphics2D g, int x, int y, int w, int h, Color color) {
        g.setColor(color);
        g.fillOval(x, y + h / 3, w / 2, h * 2 / 3);
        g.fillOval(x + w / 4, y, w / 2, h * 2 / 3);
        g.fillOval(x + w / 2, y + h / 3, w / 2, h * 2 / 3);
        g.setColor(color.darker());
        g.drawOval(x, y + h / 3, w / 2, h * 2 / 3);
        g.drawOval(x + w / 4, y, w / 2, h * 2 / 3);
        g.drawOval(x + w / 2, y + h / 3, w / 2, h * 2 / 3);
    }

    private class PaperAnimation {
        String filePath; // Full path, for identification
        String displayName; // Short name for drawing
        double currentX, currentY;
        double targetX, targetY;
        double angle; // For rotation effect
        double speed = 4.0;
        float alpha = 1.0f;

        boolean inFlight = true;
        boolean markedForCompletion = false;
        boolean successState; // True if successful completion
        boolean atTarget = false;
        int fadeDelay = 30; // frames to wait at target before fading
        int fadeCounter = 0;

        PaperAnimation(String filePath, Point start, Point end) {
            this.filePath = filePath;
            this.displayName = new java.io.File(filePath).getName();
            if (displayName.length() > 10) {
                displayName = displayName.substring(0, 7) + "...";
            }

            this.currentX = start.x + FOLDER_WIDTH / 2.0 - PAPER_WIDTH / 2.0;
            this.currentY = start.y + FOLDER_HEIGHT / 2.0 - PAPER_HEIGHT / 2.0;
            this.targetX = end.x + CLOUD_WIDTH / 2.0 - PAPER_WIDTH / 2.0;
            this.targetY = end.y + CLOUD_HEIGHT / 2.0 - PAPER_HEIGHT / 2.0;
            this.angle = (Math.random() - 0.5) * 0.5; // Slight initial tilt
        }

        void markForCompletion(boolean success) {
            this.markedForCompletion = true;
            this.successState = success;
        }

        boolean isInFlight() {
            return inFlight;
        }

        void update() {
            if (!inFlight) { // Fading out or done
                if (fadeCounter > 0) {
                    fadeCounter--;
                    alpha = Math.max(0f, (float) fadeCounter / fadeDelay);
                }
                return;
            }

            double dx = targetX - currentX;
            double dy = targetY - currentY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < speed) {
                currentX = targetX;
                currentY = targetY;
                atTarget = true;
                if (markedForCompletion) {
                    inFlight = false; // Stop moving, start fade delay
                    fadeCounter = fadeDelay;
                } // else, it waits at target until marked for completion (e.g. if mapping takes longer than animation)
            } else {
                currentX += (dx / distance) * speed;
                currentY += (dy / distance) * speed;
                // Add a little wobble/rotation
                angle += (Math.random() - 0.5) * 0.05;
                if (angle > 0.3) angle = 0.3;
                if (angle < -0.3) angle = -0.3;
            }
        }

        boolean isDone() {
            return !inFlight && fadeCounter <= 0;
        }

        void draw(Graphics2D g2d) {
            if (alpha <= 0.01f) return;

            AffineTransform oldTransform = g2d.getTransform();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            
            // Rotate paper
            g2d.translate(currentX + PAPER_WIDTH / 2.0, currentY + PAPER_HEIGHT / 2.0);
            g2d.rotate(angle);
            g2d.translate(-(currentX + PAPER_WIDTH / 2.0), -(currentY + PAPER_HEIGHT / 2.0));


            if (paperImage != null) {
                g2d.drawImage(paperImage, (int)currentX, (int)currentY, null);
            } else { // Fallback drawing
                g2d.setColor(Color.WHITE);
                g2d.fillRect((int) currentX, (int) currentY, PAPER_WIDTH, PAPER_HEIGHT);
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawRect((int) currentX, (int) currentY, PAPER_WIDTH -1 , PAPER_HEIGHT -1);
            }

            // Change border color at target based on success
            if (atTarget && markedForCompletion) {
                g2d.setColor(successState ? new Color(0,150,0, (int)(alpha*255)) : new Color(150,0,0, (int)(alpha*255)));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect((int) currentX, (int) currentY, PAPER_WIDTH - 1, PAPER_HEIGHT - 1);
            }

            g2d.setTransform(oldTransform);
            g2d.setComposite(AlphaComposite.SrcOver); // Reset composite
        }
    }
}