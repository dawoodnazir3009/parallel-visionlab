package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.ConvolutionKernel;
import com.parallelvisionlab.model.PixelImage;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/** Reusable fixed pool. Tasks own disjoint output rows and share immutable input. */
public final class ExecutorProcessor implements ImageProcessor, AutoCloseable {
    private static final AtomicInteger POOL_IDS = new AtomicInteger();
    private final int workers;
    private final ExecutorService pool;
    private final Object lifecycle = new Object();
    private final Set<Future<?>> inFlight = new HashSet<>();
    private boolean closed;

    public ExecutorProcessor(int workers) {
        com.parallelvisionlab.service.CpuLimits.validateWorkers(workers);
        RowPartitions.create(1, workers);
        this.workers = workers;
        pool = Executors.newFixedThreadPool(workers, Thread.ofPlatform().daemon(true)
                .name("visionlab-cpu-" + POOL_IDS.incrementAndGet() + "-", 0).factory());
    }

    @Override public PixelImage grayscale(PixelImage source) {
        Objects.requireNonNull(source, "source");
        int[] output = new int[source.width() * source.height()];
        forRows(source.height(), (start, end) -> RowFilters.grayscale(source, output, start, end));
        return new PixelImage(source.width(), source.height(), output);
    }
    @Override public PixelImage convolve(PixelImage source, ConvolutionKernel kernel) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(kernel, "kernel");
        int[] output = new int[source.width() * source.height()];
        forRows(source.height(), (start, end) -> RowFilters.convolve(source, kernel, output, start, end));
        return new PixelImage(source.width(), source.height(), output);
    }
    @Override public PixelImage sobel(PixelImage source) {
        PixelImage gray = grayscale(source); // Completion barrier before gradient tasks read it.
        int[] output = new int[source.width() * source.height()];
        forRows(source.height(), (start, end) -> RowFilters.sobel(source, gray, output, start, end));
        return new PixelImage(source.width(), source.height(), output);
    }

    void forRows(int height, BiConsumer<Integer, Integer> action) {
        if (Thread.currentThread().isInterrupted()) { throw new CancellationException("Caller interrupted"); }
        List<Future<?>> tasks = new ArrayList<>();
        boolean completed = false;
        try {
            synchronized (lifecycle) {
                if (closed) { throw new IllegalStateException("Processor is closed"); }
                for (var range : RowPartitions.create(height, workers)) {
                    Future<?> task = pool.submit(() -> action.accept(range.start(), range.end()));
                    tasks.add(task);
                    inFlight.add(task);
                }
            }
            for (Future<?> task : tasks) { task.get(); }
            completed = true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Caller interrupted while waiting for workers");
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof RuntimeException runtime) { throw runtime; }
            if (exception.getCause() instanceof Error error) { throw error; }
            throw new IllegalStateException("Worker failed", exception.getCause());
        } finally {
            if (!completed) { tasks.forEach(task -> task.cancel(true)); }
            synchronized (lifecycle) { inFlight.removeAll(tasks); }
        }
    }

    @Override public void close() {
        synchronized (lifecycle) {
            closed = true;
            inFlight.forEach(task -> task.cancel(true));
            pool.shutdownNow();
        }
    }

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return pool.awaitTermination(timeout, unit);
    }
}
