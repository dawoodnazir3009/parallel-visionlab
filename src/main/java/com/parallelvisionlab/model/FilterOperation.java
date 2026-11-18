package com.parallelvisionlab.model;

public enum FilterOperation {
    GRAYSCALE("Grayscale", "grayscale"),
    GAUSSIAN("Gaussian Blur", "gaussian"),
    SOBEL("Sobel Edge Detection", "sobel"),
    SHARPEN("Sharpen Convolution", "sharpen");

    private final String label;
    private final String fileSuffix;
    FilterOperation(String label, String fileSuffix) { this.label = label; this.fileSuffix = fileSuffix; }
    public String fileSuffix() { return fileSuffix; }
    @Override public String toString() { return label; }
}
