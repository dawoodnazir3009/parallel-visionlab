package com.parallelvisionlab.benchmark;

import com.parallelvisionlab.model.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkCsvTest {
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
