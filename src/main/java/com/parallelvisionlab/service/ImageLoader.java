package com.parallelvisionlab.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.image.Image;

/** Decodes images off the JavaFX application thread; owns and closes the input stream. */
public final class ImageLoader {
    public Image load(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            Image image = new Image(input);
            if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IOException("This file could not be decoded as an image.", image.getException());
            }
            return image;
        }
    }
}
