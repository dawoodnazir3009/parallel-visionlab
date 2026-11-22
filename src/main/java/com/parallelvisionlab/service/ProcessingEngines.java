package com.parallelvisionlab.service;

import com.parallelvisionlab.engine.*;
import com.parallelvisionlab.model.ExecutionBackend;
import java.util.Objects;

/** Owns reusable CPU pools and CUDA workers; rejects use after close. */
public final class ProcessingEngines implements AutoCloseable {
    private final ImageProcessor sequential = new SequentialProcessor();
    private ExecutorProcessor executor;
    private ForkJoinProcessor forkJoin;
    private final java.util.EnumMap<ExecutionBackend,CudaProcessor> cuda = new java.util.EnumMap<>(ExecutionBackend.class);
    private int forkWorkers, forkThreshold;
    private int currentWorkers;
    private boolean closed;

    public static int automaticWorkers() { return CpuLimits.automaticWorkers(); }
    public static java.util.List<ExecutionBackend> availableBackends(){return java.util.Arrays.stream(ExecutionBackend.values()).filter(backend->!backend.gpu()||CudaProcessor.available()).toList();}

    public synchronized ImageProcessor select(ExecutionBackend backend, int workers) {
        return select(backend, workers, 32);
    }

    public synchronized ImageProcessor select(ExecutionBackend backend, int workers, int threshold) {
        Objects.requireNonNull(backend, "backend");
        if (closed) { throw new IllegalStateException("Processing engines are closed"); }
        CpuLimits.validateWorkers(workers);
        if(backend.gpu())return cuda.computeIfAbsent(backend,key->new CudaProcessor(CudaProcessor.executable(),key==ExecutionBackend.CUDA_TILED,java.time.Duration.ofSeconds(60)));
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

    public synchronized void releaseCudaResources() {
        cuda.values().forEach(CudaProcessor::close);cuda.clear();
    }
    public synchronized void clearCudaResults() { cuda.values().forEach(CudaProcessor::clearLastResult); }

    @Override public synchronized void close() {
        closed = true;
        if (executor != null) { executor.close(); }
        if (forkJoin != null) { forkJoin.close(); }
        releaseCudaResources();
    }
}
