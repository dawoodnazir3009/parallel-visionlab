package com.parallelvisionlab.benchmark;

import com.parallelvisionlab.model.*;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkRunnerTest {
    @Test void capsNewRunsAndRecordsTheEffectiveWorkerCount() {
        var run=new BenchmarkRunner().run(new PixelImage(8,8,new int[64]),"cap-test",
                new ProcessingRequest(FilterOperation.GRAYSCALE,1,1),new BenchmarkConfig(0,1,16,1),p->{});
        assertEquals(8,run.config().workers());
        for(var measurement:run.measurements())
            assertEquals(measurement.backend()==ExecutionBackend.SEQUENTIAL?1:8,measurement.workers());
    }
    @Test void measuredRunHasComparableMetricsAndSourceMetadata() {
        var run = new BenchmarkRunner().run(new PixelImage(3,2,new int[6]), "fixture.png",
                new ProcessingRequest(FilterOperation.SOBEL,1,1), new BenchmarkConfig(1,3,2,1), value -> {});
        assertEquals(3,run.measurements().size());
        assertEquals(64,run.imageHash().length());
        assertEquals("fixture.png",run.source());
        assertEquals(1,run.measurements().getFirst().speedup());
        double baseline = run.measurements().getFirst().medianMs();
        for (var result : run.measurements()) {
            assertTrue(result.medianMs() > 0);
            assertTrue(result.minimumMs() <= result.medianMs() && result.medianMs() <= result.maximumMs());
            assertEquals(baseline / result.medianMs(),result.speedup(),1e-10);
            assertEquals(result.speedup() / result.workers(),result.efficiency(),1e-10);
            assertEquals(6 / (result.medianMs() * 1000),result.megapixelsPerSecond(),1e-10);
        }
    }
    @Test void medianAndConfigurationAreValidated() {
        assertEquals(2,BenchmarkRunner.median(new double[]{1,2,3}));
        assertEquals(2.5,BenchmarkRunner.median(new double[]{1,2,3,4}));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkConfig(0,0,1,1));
    }
    @Test void cancellationDoesNotReturnPartialResults() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> new BenchmarkRunner().run(new PixelImage(1,1,new int[]{0}),"test",
                    new ProcessingRequest(FilterOperation.GRAYSCALE,1,1),new BenchmarkConfig(0,1,1,1),value -> {}));
        } finally { Thread.interrupted(); }
    }
}
