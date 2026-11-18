package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.PixelImage;
import com.parallelvisionlab.model.ConvolutionKernel;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/** Integer reference: gray = (299R + 587G + 114B + 500) / 1000; preserve alpha. */
final class RowFilters {
    private RowFilters() { }
    /** Sobel on reference grayscale, replicated borders, Euclidean magnitude clamped to 255. */
    static void sobel(PixelImage source, PixelImage gray, int[] output, int start, int end) {
        int width = gray.width(), height = gray.height();
        int[] kx = {-1, 0, 1, -2, 0, 2, -1, 0, 1};
        int[] ky = {-1, -2, -1, 0, 0, 0, 1, 2, 1};
        for (int y = start; y < end; y++) {
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
    }
    /** Correlation with replicated borders, independently filtered straight RGB, unchanged center alpha. */
    static void convolve(PixelImage source, ConvolutionKernel kernel, int[] output, int start, int end) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(kernel, "kernel");
        int width = source.width(), height = source.height(), radius = kernel.size() / 2;
        for (int y = start; y < end; y++) {
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
    }

    private static int channel(double value) { return (int) Math.max(0, Math.min(255, Math.round(value))); }
    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) { throw new CancellationException("Processing interrupted"); }
    }
    static void grayscale(PixelImage source, int[] pixels, int start, int end) {
        Objects.requireNonNull(source, "source");
        for (int i = start * source.width(); i < end * source.width(); i++) {
            if (i % 4096 == 0 && Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Processing interrupted");
            }
            int argb = source.argbAt(i % source.width(), i / source.width());
            int gray = (299 * ((argb >>> 16) & 255) + 587 * ((argb >>> 8) & 255)
                    + 114 * (argb & 255) + 500) / 1000;
            pixels[i] = (argb & 0xff000000) | (gray << 16) | (gray << 8) | gray;
        }
    }
}
