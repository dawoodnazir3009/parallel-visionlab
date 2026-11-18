package com.parallelvisionlab.model;

/** Immutable square odd-sized kernel; weights are applied in row-major order (correlation). */
public final class ConvolutionKernel {
    private final int size;
    private final double[] weights;

    public ConvolutionKernel(int size, double[] weights) {
        if (size < 1 || size > 31 || size % 2 == 0 || weights == null || weights.length != size * size) {
            throw new IllegalArgumentException("Kernel must be square, odd-sized, and between 1 and 31.");
        }
        double magnitude = 0;
        for (double weight : weights) {
            if (!Double.isFinite(weight)) { throw new IllegalArgumentException("Kernel weights must be finite."); }
            magnitude += Math.abs(weight);
        }
        if (magnitude > 1_000_000) { throw new IllegalArgumentException("Kernel magnitude is too large."); }
        this.size = size;
        this.weights = weights.clone();
    }

    public int size() { return size; }
    public double weightAt(int x, int y) {
        java.util.Objects.checkIndex(x, size);
        java.util.Objects.checkIndex(y, size);
        return weights[y * size + x];
    }
    public static ConvolutionKernel sharpen() {
        return new ConvolutionKernel(3, new double[]{0, -1, 0, -1, 5, -1, 0, -1, 0});
    }
}
