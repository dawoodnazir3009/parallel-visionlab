package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.*;
import java.util.Random;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ForkJoinProcessorTest {
    @Test void exactAgreementAcrossThresholdBoundariesWorkersAndFilters() throws Exception {
        Random random = new Random(123);
        var reference = new SequentialProcessor();
        for (int threshold : new int[]{1, 4, 32}) for (int workers : new int[]{1, 2, 8}) {
            var parallel = new ForkJoinProcessor(workers, threshold);
            try (parallel) {
                for (int height : new int[]{1, 3, 4, 5, 33}) {
                    var source = new PixelImage(7, height, random.ints(7 * height).toArray());
                    for (var operation : FilterOperation.values()) {
                        var request = new ProcessingRequest(operation, 2, 1.2);
                        assertArrayEquals(reference.process(source, request).copyPixels(), parallel.process(source, request).copyPixels());
                    }
                }
            }
            assertTrue(parallel.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
    @Test void validatesConfigurationAndClosedState() {
        assertThrows(IllegalArgumentException.class, () -> new ForkJoinProcessor(0, 32));
        assertThrows(IllegalArgumentException.class, () -> new ForkJoinProcessor(2, 0));
        var engine = new ForkJoinProcessor(2, 1);
        engine.close(); engine.close();
        assertThrows(IllegalStateException.class, () -> engine.grayscale(new PixelImage(1,1,new int[]{0})));
    }
    @Test void preservesInterruptedCaller() {
        try (var engine = new ForkJoinProcessor(2, 1)) {
            Thread.currentThread().interrupt();
            try {
                assertThrows(CancellationException.class, () -> engine.grayscale(new PixelImage(1,1,new int[]{0})));
                assertTrue(Thread.currentThread().isInterrupted());
            } finally { Thread.interrupted(); }
        }
    }
}
