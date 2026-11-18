package com.parallelvisionlab.service;

import com.parallelvisionlab.model.PixelImage;
import java.util.concurrent.CancellationException;

/** Compare all ARGB channels; dimensions must agree before pixel errors are meaningful. */
public final class ImageValidation {
    public record Result(boolean dimensionsMatch, long mismatchedPixels, int maximumError, double meanError) {
        public boolean passed() { return dimensionsMatch && mismatchedPixels == 0; }
    }
    private ImageValidation() { }
    public static Result compare(PixelImage expected, PixelImage actual, int tolerance) {
        if (tolerance < 0 || tolerance > 255) { throw new IllegalArgumentException("Tolerance must be 0-255"); }
        if (expected.width() != actual.width() || expected.height() != actual.height()) {
            return new Result(false, -1, -1, Double.NaN);
        }
        int[] left = expected.copyPixels(), right = actual.copyPixels();
        long mismatch = 0, sum = 0;
        int maximum = 0;
        for (int i = 0; i < left.length; i++) {
            if (i % 4096 == 0 && Thread.currentThread().isInterrupted()) { throw new CancellationException(); }
            boolean different = false;
            for (int shift = 0; shift <= 24; shift += 8) {
                int error = Math.abs(((left[i] >>> shift) & 255) - ((right[i] >>> shift) & 255));
                maximum = Math.max(maximum, error);
                sum += error;
                different |= error > tolerance;
            }
            if (different) { mismatch++; }
        }
        return new Result(true, mismatch, maximum, sum / (4.0 * left.length));
    }
}
