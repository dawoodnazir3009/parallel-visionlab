package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.PixelImage;
import com.parallelvisionlab.model.ConvolutionKernel;
import com.parallelvisionlab.model.ProcessingRequest;
import com.parallelvisionlab.model.GaussianKernels;

/** Implementations return a new image of the same dimensions without modifying input. */
public interface ImageProcessor {
    PixelImage grayscale(PixelImage source);
    PixelImage convolve(PixelImage source, ConvolutionKernel kernel);
    PixelImage sobel(PixelImage source);
    default PixelImage process(PixelImage source, ProcessingRequest request) {
        return switch (request.operation()) {
            case GRAYSCALE -> grayscale(source);
            case GAUSSIAN -> convolve(source, GaussianKernels.create(request.gaussianRadius(), request.gaussianSigma()));
            case SOBEL -> sobel(source);
            case SHARPEN -> convolve(source, ConvolutionKernel.sharpen());
        };
    }
}
