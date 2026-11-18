package com.parallelvisionlab.service;

import com.parallelvisionlab.model.PixelImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Encode first, then replace the destination to avoid partially written output. */
public final class PngExporter {
    public void save(PixelImage image, Path destination) throws IOException {
        Path absolute = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(absolute.getParent(), ".visionlab-", ".png");
        try {
            BufferedImage buffered = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_ARGB);
            buffered.setRGB(0, 0, image.width(), image.height(), image.copyPixels(), 0, image.width());
            if (!ImageIO.write(buffered, "png", temporary.toFile())) { throw new IOException("PNG encoder unavailable."); }
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
