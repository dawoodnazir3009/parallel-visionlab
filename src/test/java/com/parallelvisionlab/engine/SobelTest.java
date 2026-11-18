package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.PixelImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SobelTest {
    private final SequentialProcessor processor = new SequentialProcessor();
    @Test void constantSinglePixelHasNoEdgeAndPreservesAlpha() {
        assertEquals(0x80000000, processor.sobel(new PixelImage(1, 1, new int[]{0x804080c0})).argbAt(0, 0));
    }
    @Test void narrowRampHasHandCalculatedBorderAndCenterMagnitude() {
        var source = new PixelImage(3, 1, new int[]{0xff000000, 0xff0a0a0a, 0xff141414});
        assertArrayEquals(new int[]{0xff282828, 0xff505050, 0xff282828}, processor.sobel(source).copyPixels());
        var vertical = new PixelImage(1, 3, source.copyPixels());
        assertArrayEquals(new int[]{0xff282828, 0xff505050, 0xff282828}, processor.sobel(vertical).copyPixels());
    }
    @Test void strongEdgesClampWithoutMutatingSource() {
        var source = new PixelImage(2, 1, new int[]{0xff000000, 0xffffffff});
        assertArrayEquals(new int[]{0xffffffff, 0xffffffff}, processor.sobel(source).copyPixels());
        assertEquals(0xff000000, source.argbAt(0, 0));
    }
}
