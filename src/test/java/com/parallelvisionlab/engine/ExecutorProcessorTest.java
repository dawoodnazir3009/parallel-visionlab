package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.*;
import java.util.Random;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExecutorProcessorTest {
    @Test void everyFilterMatchesSequentialAcrossShapesAndWorkerCounts() throws Exception {
        Random random = new Random(42);
        var sequential = new SequentialProcessor();
        for (int workers : new int[]{1, 2, 4, 16}) {
            var parallel = new ExecutorProcessor(workers);
            try (parallel) {
                for (int[] shape : new int[][]{{1,1}, {1,9}, {11,1}, {13,7}, {8,12}}) {
                    int[] pixels = random.ints(shape[0] * shape[1]).toArray();
                    var source = new PixelImage(shape[0], shape[1], pixels);
                    for (var operation : FilterOperation.values()) {
                        var request = new ProcessingRequest(operation, 2, 1.2);
                        var expected = sequential.process(source, request);
                        var actual = parallel.process(source, request);
                        assertEquals(expected.width(), actual.width());
                        assertEquals(expected.height(), actual.height());
                        assertArrayEquals(expected.copyPixels(), actual.copyPixels(), operation + " workers=" + workers);
                        assertArrayEquals(pixels, source.copyPixels());
                    }
                    var asymmetric = new ConvolutionKernel(3, new double[]{0,0,0,-1,1,1,0,0,0});
                    assertArrayEquals(sequential.convolve(source, asymmetric).copyPixels(), parallel.convolve(source, asymmetric).copyPixels());
                }
            }
            assertTrue(parallel.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
    @Test void rejectsInvalidWorkersAndClosedProcessor() {
        assertThrows(IllegalArgumentException.class, () -> new ExecutorProcessor(0));
        assertThrows(IllegalArgumentException.class, () -> new ExecutorProcessor(257));
        var processor = new ExecutorProcessor(2);
        processor.close();
        processor.close();
        assertThrows(IllegalStateException.class, () -> processor.grayscale(new PixelImage(1, 1, new int[]{0})));
    }
    @Test void propagatesWorkerFailureAndCanBeReusedAfterward() {
        try (var processor = new ExecutorProcessor(2)) {
            assertThrows(IllegalArgumentException.class, () -> processor.forRows(3, (start, end) -> { throw new IllegalArgumentException("fixture"); }));
            assertEquals(0xffffffff, processor.grayscale(new PixelImage(1,1,new int[]{0xffffffff})).argbAt(0,0));
        }
    }
    @Test void preservesCallerInterruptFlag() {
        try (var processor = new ExecutorProcessor(2)) {
            Thread.currentThread().interrupt();
            try {
                assertThrows(CancellationException.class, () -> processor.grayscale(new PixelImage(1,1,new int[]{0})));
                assertTrue(Thread.currentThread().isInterrupted());
            } finally { Thread.interrupted(); }
        }
    }
    @Test void closingAnActivePoolReleasesWorkersAndWaitingCaller() throws Exception {
        var processor = new ExecutorProcessor(2);
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (ExecutorService caller = Executors.newSingleThreadExecutor()) {
            Future<?> result = caller.submit(() -> processor.forRows(8, (start, end) -> {
                running.countDown();
                try { release.await(); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            }));
            try {
                assertTrue(running.await(5, TimeUnit.SECONDS));
                processor.close();
                assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
                assertTrue(processor.awaitTermination(5, TimeUnit.SECONDS));
            } finally { release.countDown(); processor.close(); }
        }
    }
}
