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
    private final boolean includeCuda;
    public BenchmarkRunner(){this(false);}
    public BenchmarkRunner(boolean includeCuda){this.includeCuda=includeCuda;}
    public BenchmarkRun run(PixelImage source, String name, ProcessingRequest request,
                            BenchmarkConfig config, DoubleConsumer progress) {
        // Old archives may contain larger worker counts; new runs obey the cap
        // and record the actual effective configuration without rewriting history.
        config = new BenchmarkConfig(config.warmups(),config.iterations(),
                Math.min(CpuLimits.MAX_WORKERS,config.workers()),config.leafRows());
        List<double[]> times = new ArrayList<>();
        var backends = ProcessingEngines.availableBackends().stream().filter(backend->includeCuda||!backend.gpu()).toArray(ExecutionBackend[]::new);
        List<double[][]> eventTimes=new ArrayList<>();
        int completed = 0, total = backends.length * (config.warmups() + config.iterations());
        try (var engines = new ProcessingEngines()) {
            PixelImage expected = engines.select(ExecutionBackend.SEQUENTIAL, 1).process(source, request);
            for (var backend : backends) {
                int workers = backend == ExecutionBackend.SEQUENTIAL || backend.gpu() ? 1 : config.workers();
                var processor = engines.select(backend, workers, config.leafRows());
                double[] measured = new double[config.iterations()];
                double[][] events=new double[3][config.iterations()];
                for (int i = -config.warmups(); i < config.iterations(); i++) {
                    if (Thread.currentThread().isInterrupted()) { throw new CancellationException("Benchmark cancelled"); }
                    long start = System.nanoTime();
                    PixelImage actual = processor.process(source, request);
                    double elapsed = Math.max(1, System.nanoTime() - start) / 1_000_000.0;
                    try {
                        if (!ImageValidation.compare(expected, actual, backend.gpu()?1:0).passed()) {
                            throw new IllegalStateException("Output validation failed for " + backend);
                        }
                        if (i >= 0) {
                            measured[i] = elapsed;
                            if(processor instanceof com.parallelvisionlab.engine.CudaProcessor gpu){var timings=gpu.lastResult();events[0][i]=timings.kernelMs();events[1][i]=timings.uploadMs();events[2][i]=timings.downloadMs();}
                        }
                    } finally {
                        if(processor instanceof com.parallelvisionlab.engine.CudaProcessor gpu)gpu.clearLastResult();
                    }
                    progress.accept(++completed / (double) total);
                }
                Arrays.sort(measured);
                times.add(measured);
                for(double[] values:events)Arrays.sort(values);eventTimes.add(events);
            }
        }
        double baseline = median(times.getFirst());
        List<BenchmarkRun.Measurement> measurements = new ArrayList<>();
        for (int i = 0; i < backends.length; i++) {
            double[] samples = times.get(i);
            double median = median(samples);
            int workers = backends[i].gpu()?0:(backends[i] == ExecutionBackend.SEQUENTIAL ? 1 : config.workers());
            double speedup = baseline / median;
            measurements.add(new BenchmarkRun.Measurement(backends[i], workers, median, samples[0], samples[samples.length - 1],
                    speedup, workers==0?Double.NaN:speedup / workers, (double) source.width() * source.height() / (median * 1000.0),
                    median(eventTimes.get(i)[0]),median(eventTimes.get(i)[1]),median(eventTimes.get(i)[2]),backends[i].gpu()?1:0));
        }
        return new BenchmarkRun(UUID.randomUUID().toString(), Instant.now(), name, hash(source), source.width(), source.height(),
                request, config, measurements, System.getProperty("java.version"), System.getProperty("os.name") + " " + System.getProperty("os.arch"),
                "logical_processors="+Runtime.getRuntime().availableProcessors()+"; CPU="+System.getenv().getOrDefault("PROCESSOR_IDENTIFIER",System.getProperty("os.arch"))+
                        "; GPU="+(includeCuda?NvidiaTelemetry.sample().map(NvidiaTelemetry.Snapshot::name).orElse("unavailable"):"not included")
                        +(includeCuda?"; CUDA_transport=persistent-pipe-v2; GPU_input_cache=immutable-identity":""));
    }

    static double median(double[] sorted) {
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
    }
    private static String hash(PixelImage image) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            for (int y=0;y<image.height();y++)for(int x=0;x<image.width();x++) {
                if (buffer.remaining() < 4) { digest.update(buffer.array(), 0, buffer.position()); buffer.clear(); }
                buffer.putInt(image.argbAt(x,y));
            }
            digest.update(buffer.array(), 0, buffer.position());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
