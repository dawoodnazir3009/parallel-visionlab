package com.parallelvisionlab.service;

import com.parallelvisionlab.model.PixelImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImageValidationTest {
    @Test void measuresChannelErrorIncludingAlpha() {
        var left = new PixelImage(1,1,new int[]{0x80102030});
        var right = new PixelImage(1,1,new int[]{0x82102130});
        var result = ImageValidation.compare(left,right,0);
        assertFalse(result.passed());
        assertEquals(1,result.mismatchedPixels());
        assertEquals(2,result.maximumError());
        assertEquals(.75,result.meanError());
        assertTrue(ImageValidation.compare(left,right,2).passed());
    }
    @Test void rejectsWrongShapeEvenWithSamePixelCount() {
        var result = ImageValidation.compare(new PixelImage(1,2,new int[2]),new PixelImage(2,1,new int[2]),0);
        assertFalse(result.dimensionsMatch()); assertFalse(result.passed());
        assertTrue(Double.isNaN(result.meanError()));
    }
    @Test void validatesToleranceAndIdenticalImages() {
        var image = new PixelImage(1,1,new int[]{0x12345678});
        assertTrue(ImageValidation.compare(image,image,0).passed());
        assertThrows(IllegalArgumentException.class, () -> ImageValidation.compare(image,image,-1));
    }
}
