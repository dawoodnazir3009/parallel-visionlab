package com.parallelvisionlab.service;

import com.parallelvisionlab.engine.SequentialProcessor;
import com.parallelvisionlab.model.PixelImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PixelImagesTest {
    @Test void opaquePreviewConversionPreservesPixelOrderAndResolution() {
        PixelImage source = new PixelImage(3, 1, new int[]{0xffff0000, 0xff00ff00, 0xff0000ff});
        var preview = PixelImages.toFx(source);
        assertEquals(3, preview.getWidth());
        assertArrayEquals(source.copyPixels(), PixelImages.fromFx(preview).copyPixels());
    }
    @Test void loadedPreviewToEngineToResultPreviewUsesSameGrayscaleContract() {
        var input = PixelImages.toFx(new PixelImage(1, 2, new int[]{0xffff0000, 0xff0000ff}));
        PixelImage result = new SequentialProcessor().grayscale(PixelImages.fromFx(input));
        var output = PixelImages.toFx(result);
        assertEquals(0xff4c4c4c, output.getPixelReader().getArgb(0, 0));
        assertEquals(0xff1d1d1d, output.getPixelReader().getArgb(0, 1));
        assertEquals(0xffff0000, input.getPixelReader().getArgb(0, 0));
    }
}
