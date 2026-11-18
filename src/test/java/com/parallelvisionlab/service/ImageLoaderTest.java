package com.parallelvisionlab.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ImageLoaderTest {
    @Test void rejectsOversizedHeadersBeforePixelAllocation() {
        assertThrows(IOException.class,()->ImageLoader.validateDimensions(100000,100000,4L*1024*1024*1024));
        assertThrows(IOException.class,()->ImageLoader.validateDimensions(1024,1024,1024));
        assertThrows(IOException.class,()->ImageLoader.validateDimensions(0,2,Long.MAX_VALUE));
    }
    @TempDir Path directory;
    private final ImageLoader loader = new ImageLoader();

    @Test void decodesKnownPixelAndClosesFile() throws IOException {
        Path file = directory.resolve("pixel.png");
        Files.write(file, Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII="));
        var image = loader.load(file);
        assertEquals(1, image.getWidth());
        assertEquals(1, image.getHeight());
        assertEquals(0xffffffff, image.getPixelReader().getArgb(0, 0));
        Files.delete(file);
        assertFalse(Files.exists(file));
    }

    @Test void rejectsCorruptImage() throws IOException {
        Path file = Files.writeString(directory.resolve("broken.png"), "not an image");
        assertThrows(IOException.class, () -> loader.load(file));
    }

    @Test void rejectsMissingFile() {
        assertThrows(IOException.class, () -> loader.load(directory.resolve("missing.png")));
    }
}
