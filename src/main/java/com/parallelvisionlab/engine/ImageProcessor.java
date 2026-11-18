package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.PixelImage;

/** Implementations return a new image of the same dimensions without modifying input. */
public interface ImageProcessor {
    PixelImage grayscale(PixelImage source);
}
