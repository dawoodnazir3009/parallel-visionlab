package com.parallelvisionlab.model;

import java.util.Objects;

/** Immutable row-major, straight-alpha ARGB pixels independent of JavaFX. */
public final class PixelImage {
    private final int width;
    private final int height;
    private final int[] pixels;

    public PixelImage(int width, int height, int[] pixels) {
        Objects.requireNonNull(pixels, "pixels");
        if (width <= 0 || height <= 0 || (long) width * height > Integer.MAX_VALUE
                || (long) width * height != pixels.length) {
            throw new IllegalArgumentException("Positive dimensions must match pixel count without overflow.");
        }
        this.width = width;
        this.height = height;
        this.pixels = pixels.clone();
    }

    public int width() { return width; }
    public int height() { return height; }
    public int[] copyPixels() { return pixels.clone(); }
    public int argbAt(int x, int y) {
        Objects.checkIndex(x, width);
        Objects.checkIndex(y, height);
        return pixels[y * width + x];
    }
}
