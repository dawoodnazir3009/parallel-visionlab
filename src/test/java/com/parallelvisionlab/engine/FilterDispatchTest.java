package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FilterDispatchTest {
    @Test void dispatchesEveryOperationWithOriginalShapeAndAlpha() {
        var source = new PixelImage(1, 1, new int[]{0x804080c0});
        ImageProcessor engine = new SequentialProcessor();
        for (FilterOperation operation : FilterOperation.values()) {
            var result = engine.process(source, new ProcessingRequest(operation, 2, 1.2));
            assertEquals(1, result.width());
            assertEquals(1, result.height());
            assertEquals(0x80, result.argbAt(0, 0) >>> 24);
        }
        assertEquals(0x804080c0, source.argbAt(0, 0));
    }
    @Test void requestRejectsInvalidGaussianOptionsAndNullOperation() {
        assertThrows(NullPointerException.class, () -> new ProcessingRequest(null, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ProcessingRequest(FilterOperation.GAUSSIAN, 8, 1));
        assertThrows(IllegalArgumentException.class, () -> new ProcessingRequest(FilterOperation.GAUSSIAN, 1, Double.NaN));
    }
}
