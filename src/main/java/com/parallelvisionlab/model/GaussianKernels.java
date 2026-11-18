package com.parallelvisionlab.model;

public final class GaussianKernels {
    private GaussianKernels() { }
    public static ConvolutionKernel create(int radius, double sigma) {
        if (radius < 1 || radius > 7 || !Double.isFinite(sigma) || sigma < 0.1 || sigma > 10) {
            throw new IllegalArgumentException("Gaussian radius must be 1-7 and sigma 0.1-10.");
        }
        int size = 2 * radius + 1;
        double[] weights = new double[size * size];
        double sum = 0;
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                double weight = Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                weights[(y + radius) * size + x + radius] = weight;
                sum += weight;
            }
        }
        for (int i = 0; i < weights.length; i++) { weights[i] /= sum; }
        return new ConvolutionKernel(size, weights);
    }
}
