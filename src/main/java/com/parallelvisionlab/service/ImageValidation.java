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


    public static Result compare(PixelImage expected, PixelImage actual, int tolerance) 
    {
        if (tolerance < 0 || tolerance > 255) 
        { 
            throw new IllegalArgumentException("Tolerance must be 0-255"); 
        }

        if (expected.width() != actual.width() || expected.height() != actual.height()) 
        {
            return new Result(false, -1, -1, Double.NaN);
        }
