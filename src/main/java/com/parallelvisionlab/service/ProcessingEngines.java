package com.parallelvisionlab.service;

import com.parallelvisionlab.engine.*;
import com.parallelvisionlab.model.ExecutionBackend;
import java.util.Objects;

/** Owns a reusable pool, replaces it on worker-count changes, rejects use after close. */
public final class ProcessingEngines implements AutoCloseable {
    private final ImageProcessor sequential = new SequentialProcessor();
    private ExecutorProcessor executor;
    private ForkJoinProcessor forkJoin;
    private int forkWorkers, forkThreshold;
    private int currentWorkers;
    private boolean closed;

    public static int automaticWorkers() { return Math.max(1, Math.min(256, Runtime.getRuntime().availableProcessors())); }
    public static java.util.List<ExecutionBackend> availableBackends(){return java.util.Arrays.stream(ExecutionBackend.values()).filter(backend->!backend.gpu()||CudaProcessor.available()).toList();}

    public synchronized ImageProcessor select(ExecutionBackend backend, int workers) {
        return select(backend, workers, 32);
    }

    public synchronized ImageProcessor select(ExecutionBackend backend, int workers, int threshold) {
        Objects.requireNonNull(backend, "backend");
        if (closed) { throw new IllegalStateException("Processing engines are closed"); }
        if (workers < 1 || workers > 256) { throw new IllegalArgumentException("Workers must be between 1 and 256"); }
        if(backend.gpu())return new CudaProcessor(CudaProcessor.executable(),backend==ExecutionBackend.CUDA_TILED,java.time.Duration.ofSeconds(60));
        if (backend == ExecutionBackend.SEQUENTIAL) { return sequential; }
        if (backend == ExecutionBackend.FORK_JOIN) {
            if (threshold < 1 || threshold > 2048) { throw new IllegalArgumentException("Row threshold must be 1-2048"); }
            if (forkJoin == null || forkWorkers != workers || forkThreshold != threshold) {
                if (forkJoin != null) { forkJoin.close(); }
                forkJoin = new ForkJoinProcessor(workers, threshold);
                forkWorkers = workers; forkThreshold = threshold;
            }
            return forkJoin;
        }
        if (executor == null || currentWorkers != workers) {
            if (executor != null) { executor.close(); }
            executor = new ExecutorProcessor(workers);
            currentWorkers = workers;
        }
        return executor;
    }

    @Override public synchronized void close() {
        closed = true;
        if (executor != null) { executor.close(); }
        if (forkJoin != null) { forkJoin.close(); }
    }
}
