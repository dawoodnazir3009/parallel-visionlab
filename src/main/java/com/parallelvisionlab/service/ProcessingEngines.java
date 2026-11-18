package com.parallelvisionlab.service;

import com.parallelvisionlab.engine.*;
import com.parallelvisionlab.model.ExecutionBackend;
import java.util.Objects;

/** Owns a reusable pool, replaces it on worker-count changes, rejects use after close. */
public final class ProcessingEngines implements AutoCloseable {
    private final ImageProcessor sequential = new SequentialProcessor();
    private ExecutorProcessor executor;
    private int currentWorkers;
    private boolean closed;

    public static int automaticWorkers() { return Math.max(1, Math.min(256, Runtime.getRuntime().availableProcessors())); }

    public synchronized ImageProcessor select(ExecutionBackend backend, int workers) {
        Objects.requireNonNull(backend, "backend");
        if (closed) { throw new IllegalStateException("Processing engines are closed"); }
        if (workers < 1 || workers > 256) { throw new IllegalArgumentException("Workers must be between 1 and 256"); }
        if (backend == ExecutionBackend.SEQUENTIAL) { return sequential; }
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
    }
}
