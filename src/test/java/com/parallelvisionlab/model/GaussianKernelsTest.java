package com.parallelvisionlab.model;

import com.parallelvisionlab.engine.SequentialProcessor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GaussianKernelsTest {
    @Test void weightsAreNormalizedSymmetricAndPositive() {
        var kernel = GaussianKernels.create(2, 1.2);
        double sum = 0;
        for (int y = 0; y < kernel.size(); y++) for (int x = 0; x < kernel.size(); x++) {
            double weight = kernel.weightAt(x, y);
            sum += weight;
            assertTrue(weight > 0);
            assertEquals(weight, kernel.weightAt(4 - x, 4 - y), 1e-15);
        }
        assertEquals(1, sum, 1e-12);
    }
    @Test void preservesConstantImageAndSpreadsImpulse() {
        var processor = new SequentialProcessor();
        var kernel = GaussianKernels.create(1, 1);
        assertEquals(0x804080c0, processor.convolve(new PixelImage(1, 1, new int[]{0x804080c0}), kernel).argbAt(0, 0));
        int[] pixels = new int[25];
        java.util.Arrays.fill(pixels, 0xff000000);
        pixels[12] = 0xffffffff;
        var result = processor.convolve(new PixelImage(5, 5, pixels), kernel);
        assertEquals(0xff343434, result.argbAt(2, 2));
        assertEquals(0xff202020, result.argbAt(1, 2));
        assertEquals(0xff000000, result.argbAt(0, 0));
    }
    @Test void rejectsInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> GaussianKernels.create(0, 1));
        assertThrows(IllegalArgumentException.class, () -> GaussianKernels.create(8, 1));
        assertThrows(IllegalArgumentException.class, () -> GaussianKernels.create(1, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> GaussianKernels.create(1, 0));
    }
}
