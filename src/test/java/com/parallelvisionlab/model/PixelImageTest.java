package com.parallelvisionlab.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PixelImageTest {
    @Test void ownsInputAndReturnedArrays() {
        int[] pixels = {0xff123456};
        PixelImage image = new PixelImage(1, 1, pixels);
        pixels[0] = 0;
        image.copyPixels()[0] = 0;
        assertEquals(0xff123456, image.argbAt(0, 0));
    }
    @Test void rejectsInvalidShapesAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> new PixelImage(0, 1, new int[0]));
        assertThrows(IllegalArgumentException.class, () -> new PixelImage(-1, 1, new int[0]));
        assertThrows(IllegalArgumentException.class, () -> new PixelImage(2, 2, new int[3]));
        assertThrows(IllegalArgumentException.class, () -> new PixelImage(65536, 65536, new int[0]));
        assertThrows(NullPointerException.class, () -> new PixelImage(1, 1, null));
    }
    @Test void usesRowMajorCoordinatesAndChecksEachAxis() {
        PixelImage image = new PixelImage(2, 2, new int[]{1, 2, 3, 4});
        assertEquals(3, image.argbAt(0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> image.argbAt(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> image.argbAt(-1, 1));
    }
}
