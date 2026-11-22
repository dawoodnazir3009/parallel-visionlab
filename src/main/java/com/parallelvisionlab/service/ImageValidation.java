package com.parallelvisionlab.service;

import com.parallelvisionlab.model.PixelImage;
import java.util.concurrent.CancellationException;

/** Compare all ARGB channels; dimensions must agree before pixel errors are meaningful. */
public final class ImageValidation {
    public record Result(boolean dimensionsMatch, long mismatchedPixels, int maximumError, double meanError) {
        public boolean passed() { return dimensionsMatch && mismatchedPixels == 0; }
    }

    private ImageValidation() 
    { 
        
    }
