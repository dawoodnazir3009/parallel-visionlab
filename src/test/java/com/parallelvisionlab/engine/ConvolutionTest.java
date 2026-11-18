package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConvolutionTest {
    private final SequentialProcessor processor = new SequentialProcessor();
    @Test void convolutionAndSobelHonorInterruption() {
        var source = new PixelImage(1, 1, new int[]{0});
        Thread.currentThread().interrupt();
        try {
            assertThrows(java.util.concurrent.CancellationException.class, () -> processor.convolve(source, ConvolutionKernel.sharpen()));
            assertThrows(java.util.concurrent.CancellationException.class, () -> processor.sobel(source));
        } finally { Thread.interrupted(); }
    }
    @Test void asymmetricKernelFixesOrientationAndReplicatedBorders() {
        var source = new PixelImage(3, 1, new int[]{0xff0a0a0a, 0xff141414, 0xff1e1e1e});
        var right = new ConvolutionKernel(3, new double[]{0, 0, 0, 0, 0, 1, 0, 0, 0});
        assertArrayEquals(new int[]{0xff141414, 0xff1e1e1e, 0xff1e1e1e}, processor.convolve(source, right).copyPixels());
        assertEquals(0xff0a0a0a, source.argbAt(0, 0));
    }
    @Test void filtersRgbButPreservesCenterAlphaIncludingTransparentRgb() {
        var source = new PixelImage(1, 1, new int[]{0x00112233});
        assertEquals(0x00112233, processor.convolve(source, ConvolutionKernel.sharpen()).argbAt(0, 0));
    }
    @Test void clampsNegativeAndOverflowAndRoundsHalfUp() {
        var source = new PixelImage(1, 1, new int[]{0x801020ff});
        assertEquals(0x80000000, processor.convolve(source, new ConvolutionKernel(1, new double[]{-1})).argbAt(0, 0));
        assertEquals(0x802040ff, processor.convolve(source, new ConvolutionKernel(1, new double[]{2})).argbAt(0, 0));
        assertEquals(0x80081080, processor.convolve(source, new ConvolutionKernel(1, new double[]{.5})).argbAt(0, 0));
    }
    @Test void boxFilterMatchesHandComputedCenterAndCorner() {
        var source = new PixelImage(3, 3, new int[]{0xff000000,0xff000000,0xff000000,0xff000000,0xff5a5a5a,0xff000000,0xff000000,0xff000000,0xff000000});
        double[] weights = new double[9];
        java.util.Arrays.fill(weights, 1.0 / 9);
        var result = processor.convolve(source, new ConvolutionKernel(3, weights));
        assertEquals(0xff0a0a0a, result.argbAt(1, 1));
        assertEquals(0xff0a0a0a, result.argbAt(0, 0));
    }
}
