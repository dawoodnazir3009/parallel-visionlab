package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.PixelImage;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/** Integer reference: gray = (299R + 587G + 114B + 500) / 1000; preserve alpha. */
public final class SequentialProcessor implements ImageProcessor {
    @Override
    public PixelImage grayscale(PixelImage source) {
        Objects.requireNonNull(source, "source");
        int[] pixels = source.copyPixels();
        for (int i = 0; i < pixels.length; i++) {
            if (i % 4096 == 0 && Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Processing interrupted");
            }
            int argb = pixels[i];
            int gray = (299 * ((argb >>> 16) & 255) + 587 * ((argb >>> 8) & 255)
                    + 114 * (argb & 255) + 500) / 1000;
            pixels[i] = (argb & 0xff000000) | (gray << 16) | (gray << 8) | gray;
        }
        return new PixelImage(source.width(), source.height(), pixels);
    }
}
