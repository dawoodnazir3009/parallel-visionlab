package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.PixelImage;
import com.parallelvisionlab.model.ConvolutionKernel;

/** Implementations return a new image of the same dimensions without modifying input. */
public interface ImageProcessor {
    PixelImage grayscale(PixelImage source);
    PixelImage convolve(PixelImage source, ConvolutionKernel kernel);
    PixelImage sobel(PixelImage source);
}
