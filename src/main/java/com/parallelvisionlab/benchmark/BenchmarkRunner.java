package com.parallelvisionlab.benchmark;

import com.parallelvisionlab.model.*;
import com.parallelvisionlab.service.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.DoubleConsumer;

/** Engine-only wall-clock measurements; decoding, validation and rendering are excluded. */
public final class BenchmarkRunner {
    public BenchmarkRun run(PixelImage source, String name, ProcessingRequest request,
                            BenchmarkConfig config, DoubleConsumer progress) {
        List<double[]> times = new ArrayList<>();
        var backends = ExecutionBackend.values();
        int completed = 0, total = backends.length * (config.warmups() + config.iterations());
        try (var engines = new ProcessingEngines()) {
            PixelImage expected = engines.select(ExecutionBackend.SEQUENTIAL, 1).process(source, request);
            for (var backend : backends) {
                int workers = backend == ExecutionBackend.SEQUENTIAL ? 1 : config.workers();
                var processor = engines.select(backend, workers, config.leafRows());
                double[] measured = new double[config.iterations()];
                for (int i = -config.warmups(); i < config.iterations(); i++) {
                    if (Thread.currentThread().isInterrupted()) { throw new CancellationException("Benchmark cancelled"); }
                    long start = System.nanoTime();
                    PixelImage actual = processor.process(source, request);
                    double elapsed = Math.max(1, System.nanoTime() - start) / 1_000_000.0;
                    if (!ImageValidation.compare(expected, actual, 0).passed()) {
                        throw new IllegalStateException("Output validation failed for " + backend);
                    }
                    if (i >= 0) { measured[i] = elapsed; }
                    progress.accept(++completed / (double) total);
                }
                Arrays.sort(measured);
                times.add(measured);
            }
        }
        double baseline = median(times.getFirst());
        List<BenchmarkRun.Measurement> measurements = new ArrayList<>();
        for (int i = 0; i < backends.length; i++) {
            double[] samples = times.get(i);
            double median = median(samples);
            int workers = backends[i] == ExecutionBackend.SEQUENTIAL ? 1 : config.workers();
            double speedup = baseline / median;
            measurements.add(new BenchmarkRun.Measurement(backends[i], workers, median, samples[0], samples[samples.length - 1],
                    speedup, speedup / workers, (double) source.width() * source.height() / (median * 1000.0)));
        }
        return new BenchmarkRun(UUID.randomUUID().toString(), Instant.now(), name, hash(source), source.width(), source.height(),
                request, config, measurements, System.getProperty("java.version"), System.getProperty("os.name") + " " + System.getProperty("os.arch"));
    }

    static double median(double[] sorted) {
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
    }
    private static String hash(PixelImage image) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            for (int pixel : image.copyPixels()) {
                if (buffer.remaining() < 4) { digest.update(buffer.array(), 0, buffer.position()); buffer.clear(); }
                buffer.putInt(pixel);
            }
            digest.update(buffer.array(), 0, buffer.position());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
