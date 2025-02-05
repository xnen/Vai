package io.improt.vai.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class ImageUtils {
    public static BufferedImage cropAndResizeImage(BufferedImage source, int targetSize) {
        int width = source.getWidth();
        int height = source.getHeight();
        int minDim = Math.min(width, height);
        int x = (width - minDim) / 2;
        int y = (height - minDim) / 2;
        BufferedImage cropped = source.getSubimage(x, y, minDim, minDim);
        BufferedImage resized = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(cropped, 0, 0, targetSize, targetSize, null);
        g2.dispose();
        return resized;
    }
}
