package com.parallelvisionlab.service;

import com.parallelvisionlab.model.PixelImage;
import java.io.IOException;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PngExporterTest {
    @TempDir Path directory;
    @Test void everyFilterExportsTheComputedPixelsWithoutPreviewResampling() throws IOException {
        var source = new PixelImage(3, 1, new int[]{0x80ff0000, 0xff00ff00, 0xff0000ff});
        var engine = new com.parallelvisionlab.engine.SequentialProcessor();
        for (var operation : com.parallelvisionlab.model.FilterOperation.values()) {
            var output = engine.process(source, new com.parallelvisionlab.model.ProcessingRequest(operation, 1, 1));
            Path file = directory.resolve(operation.fileSuffix() + ".png");
            new PngExporter().save(output, file);
            var decoded = ImageIO.read(file.toFile());
            assertArrayEquals(output.copyPixels(), decoded.getRGB(0, 0, 3, 1, null, 0, 3));
        }
    }
    @Test void roundTripsFullResolutionAndStraightAlphaPixels() throws IOException {
        PixelImage source = new PixelImage(2, 2, new int[]{0xff4c4c4c, 0x80969696, 0x001d1d1d, 0xffffffff});
        Path file = Files.writeString(directory.resolve("result.png"), "previous output");
        new PngExporter().save(source, file);
        var decoded = ImageIO.read(file.toFile());
        assertEquals(2, decoded.getWidth());
        assertEquals(2, decoded.getHeight());
        assertArrayEquals(source.copyPixels(), decoded.getRGB(0, 0, 2, 2, null, 0, 2));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
    @Test void missingParentFailsWithoutCreatingDestination() {
        Path destination = directory.resolve("missing/result.png");
        assertThrows(IOException.class, () -> new PngExporter().save(new PixelImage(1, 1, new int[]{0}), destination));
        assertFalse(Files.exists(destination));
    }
}
