package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.PixelImage;
import com.parallelvisionlab.model.ConvolutionKernel;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/** Integer reference: gray = (299R + 587G + 114B + 500) / 1000; preserve alpha. */
public final class SequentialProcessor implements ImageProcessor {
    /** Correlation with replicated borders, independently filtered straight RGB, unchanged center alpha. */
    @Override
    public PixelImage convolve(PixelImage source, ConvolutionKernel kernel) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(kernel, "kernel");
        int width = source.width(), height = source.height(), radius = kernel.size() / 2;
        int[] output = new int[width * height];
        for (int y = 0; y < height; y++) {
            checkInterrupted();
            for (int x = 0; x < width; x++) {
                double red = 0, green = 0, blue = 0;
                for (int ky = 0; ky < kernel.size(); ky++) {
                    int sy = Math.max(0, Math.min(height - 1, y + ky - radius));
                    for (int kx = 0; kx < kernel.size(); kx++) {
                        int sx = Math.max(0, Math.min(width - 1, x + kx - radius));
                        int argb = source.argbAt(sx, sy);
                        double weight = kernel.weightAt(kx, ky);
                        red += ((argb >>> 16) & 255) * weight;
                        green += ((argb >>> 8) & 255) * weight;
                        blue += (argb & 255) * weight;
                    }
                }
                output[y * width + x] = (source.argbAt(x, y) & 0xff000000)
                        | (channel(red) << 16) | (channel(green) << 8) | channel(blue);
            }
        }
        return new PixelImage(width, height, output);
    }

    private static int channel(double value) { return (int) Math.max(0, Math.min(255, Math.round(value))); }
    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) { throw new CancellationException("Processing interrupted"); }
    }
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
