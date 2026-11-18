package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.PixelImage;
import com.parallelvisionlab.model.ConvolutionKernel;
import java.util.Objects;

/** Single-thread reference using the same deterministic pixel operations as parallel engines. */
public final class SequentialProcessor implements ImageProcessor {
    @Override public PixelImage grayscale(PixelImage source) {
        Objects.requireNonNull(source, "source");
        int[] output = new int[source.width() * source.height()];
        RowFilters.grayscale(source, output, 0, source.height());
        return new PixelImage(source.width(), source.height(), output);
    }
    @Override public PixelImage convolve(PixelImage source, ConvolutionKernel kernel) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(kernel, "kernel");
        int[] output = new int[source.width() * source.height()];
        RowFilters.convolve(source, kernel, output, 0, source.height());
        return new PixelImage(source.width(), source.height(), output);
    }
    @Override public PixelImage sobel(PixelImage source) {
        PixelImage gray = grayscale(source);
        int[] output = new int[source.width() * source.height()];
        RowFilters.sobel(source, gray, output, 0, source.height());
        return new PixelImage(source.width(), source.height(), output);
    }
}
