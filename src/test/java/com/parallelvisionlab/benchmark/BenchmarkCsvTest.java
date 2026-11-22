package com.parallelvisionlab.benchmark;

import com.parallelvisionlab.model.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkCsvTest {
    @Test void preservesHistoricalFileBridgeScope() {
        var measurement=new BenchmarkRun.Measurement(ExecutionBackend.CUDA_BASIC,0,10,9,11,1,Double.NaN,1,.5,.2,.3,1);
        var old=new BenchmarkRun("fixture",java.time.Instant.EPOCH,"image","hash",1,1,
                new ProcessingRequest(FilterOperation.GRAYSCALE,1,1),new BenchmarkConfig(1,2,2,1),
                List.of(measurement),"25","Windows","GPU=test");
        String csv=BenchmarkCsv.encode(List.of(old));
        assertTrue(csv.contains("process-bridge-wall-clock"));
        assertFalse(csv.contains("persistent-pipe-wall-clock"));
    }
    @Test void escapesNamesAndIdentifiesMeasuredResults() {
        var run = new BenchmarkRunner().run(new PixelImage(1,1,new int[]{0}), "a,\"b.png",
                new ProcessingRequest(FilterOperation.GRAYSCALE,1,1),new BenchmarkConfig(0,1,1,1),v -> {});
        String csv = BenchmarkCsv.encode(List.of(run));
        assertTrue(csv.contains("\"a,\"\"b.png\""));
        assertTrue(csv.contains("\"measured\""));
        assertEquals(4,csv.split("\r\n").length);
        assertEquals("\"'=SUM(A1)\"",BenchmarkCsv.quote("=SUM(A1)"));
    }
}
