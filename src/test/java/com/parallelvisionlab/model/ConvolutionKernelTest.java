package com.parallelvisionlab.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConvolutionKernelTest {
    @Test void rejectsInvalidDimensionsAndNonFiniteOrExcessiveWeights() {
        assertThrows(IllegalArgumentException.class, () -> new ConvolutionKernel(2, new double[4]));
        assertThrows(IllegalArgumentException.class, () -> new ConvolutionKernel(33, new double[1089]));
        assertThrows(IllegalArgumentException.class, () -> new ConvolutionKernel(3, new double[8]));
        assertThrows(IllegalArgumentException.class, () -> new ConvolutionKernel(1, new double[]{Double.NaN}));
        assertThrows(IllegalArgumentException.class, () -> new ConvolutionKernel(1, new double[]{Double.POSITIVE_INFINITY}));
        assertThrows(IllegalArgumentException.class, () -> new ConvolutionKernel(1, new double[]{1_000_001}));
    }
    @Test void ownsWeightsAndChecksCoordinates() {
        double[] weights = {1};
        ConvolutionKernel kernel = new ConvolutionKernel(1, weights);
        weights[0] = 0;
        assertEquals(1, kernel.weightAt(0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> kernel.weightAt(-1, 0));
    }
}
