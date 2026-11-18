package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.PixelImage;
import com.parallelvisionlab.model.ConvolutionKernel;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/** Integer reference: gray = (299R + 587G + 114B + 500) / 1000; preserve alpha. */
public final class SequentialProcessor implements ImageProcessor {
    /** Sobel on reference grayscale, replicated borders, Euclidean magnitude clamped to 255. */
    @Override
    public PixelImage sobel(PixelImage source) {
        PixelImage gray = grayscale(source);
        int width = gray.width(), height = gray.height();
        int[] output = new int[width * height];
        int[] kx = {-1, 0, 1, -2, 0, 2, -1, 0, 1};
        int[] ky = {-1, -2, -1, 0, 0, 0, 1, 2, 1};
        for (int y = 0; y < height; y++) {
            checkInterrupted();
            for (int x = 0; x < width; x++) {
                int gx = 0, gy = 0;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int sx = Math.max(0, Math.min(width - 1, x + dx));
                    int sy = Math.max(0, Math.min(height - 1, y + dy));
                    int value = gray.argbAt(sx, sy) & 255;
                    int index = (dy + 1) * 3 + dx + 1;
                    gx += value * kx[index];
                    gy += value * ky[index];
                }
                int edge = channel(Math.hypot(gx, gy));
                output[y * width + x] = (source.argbAt(x, y) & 0xff000000) | (edge << 16) | (edge << 8) | edge;
            }
        }
        return new PixelImage(width, height, output);
    }
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
