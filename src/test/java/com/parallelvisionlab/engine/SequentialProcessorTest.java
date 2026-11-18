package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.PixelImage;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SequentialProcessorTest {
    private final ImageProcessor processor = new SequentialProcessor();
    @Test void convertsKnownColorsWithFixedRounding() {
        PixelImage source = new PixelImage(5, 1, new int[]{0xffff0000, 0xff00ff00, 0xff0000ff, 0xff000000, 0xffffffff});
        assertArrayEquals(new int[]{0xff4c4c4c, 0xff969696, 0xff1d1d1d, 0xff000000, 0xffffffff}, processor.grayscale(source).copyPixels());
        assertEquals(0xffff0000, source.argbAt(0, 0));
    }
    @Test void preservesAlphaAndShapeForNarrowImage() {
        PixelImage result = processor.grayscale(new PixelImage(1, 3, new int[]{0x80ff0000, 0x0000ff00, 0x120000ff}));
        assertEquals(1, result.width());
        assertEquals(3, result.height());
        assertArrayEquals(new int[]{0x804c4c4c, 0x00969696, 0x121d1d1d}, result.copyPixels());
    }
    @Test void grayscaleIsIdempotent() {
        PixelImage source = new PixelImage(1, 1, new int[]{0xff123456});
        PixelImage gray = processor.grayscale(source);
        assertArrayEquals(gray.copyPixels(), processor.grayscale(gray).copyPixels());
    }
    @Test void supportsInterruptionWithoutConsumingInterruptFlag() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> processor.grayscale(new PixelImage(1, 1, new int[]{0})));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }
}
