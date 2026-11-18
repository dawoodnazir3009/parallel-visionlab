package com.parallelvisionlab.benchmark;

import com.parallelvisionlab.model.*;
import java.time.Instant;
import java.util.List;

public record BenchmarkRun(String id, Instant timestamp, String source, String imageHash, int width, int height,
                           ProcessingRequest request, BenchmarkConfig config, List<Measurement> measurements,
                           String javaVersion, String operatingSystem) {
    public BenchmarkRun { measurements = List.copyOf(measurements); }
    public record Measurement(ExecutionBackend backend, int workers, double medianMs, double minimumMs,
                              double maximumMs, double speedup, double efficiency, double megapixelsPerSecond) { }
}
