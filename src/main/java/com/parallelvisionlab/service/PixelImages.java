package com.parallelvisionlab.service;

import com.parallelvisionlab.model.PixelImage;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/** Boundary adapter; processing engines never depend on JavaFX. */
public final class PixelImages {
    private PixelImages() { }
    public static PixelImage fromFx(Image image) {
        if (image.isError() || image.getPixelReader() == null) {
            throw new IllegalArgumentException("Image has no readable pixels.");
        }
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int[] pixels = new int[Math.multiplyExact(width, height)];
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return new PixelImage(width, height, pixels);
    }
    public static WritableImage toFx(PixelImage image) {
        WritableImage output = new WritableImage(image.width(), image.height());
        output.getPixelWriter().setPixels(0, 0, image.width(), image.height(),
                PixelFormat.getIntArgbInstance(), image.copyPixels(), 0, image.width());
        return output;
    }
}
