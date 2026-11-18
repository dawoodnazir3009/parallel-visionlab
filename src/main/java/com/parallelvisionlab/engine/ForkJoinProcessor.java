package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/** Recursive disjoint-row tasks with cooperative cancellation between leaf rows. */
public final class ForkJoinProcessor implements ImageProcessor, AutoCloseable {
    private final ForkJoinPool pool;
    private final int threshold;
    private final Map<ForkJoinTask<?>, AtomicBoolean> active = new HashMap<>();
    private boolean closed;

    public ForkJoinProcessor(int workers, int threshold) {
        RowPartitions.create(1, workers);
        if (threshold < 1 || threshold > 2048) { throw new IllegalArgumentException("Row threshold must be 1-2048"); }
        this.threshold = threshold;
        pool = new ForkJoinPool(workers);
    }

    @Override public PixelImage grayscale(PixelImage source) {
        Objects.requireNonNull(source);
        int[] output = new int[source.width() * source.height()];
        rows(source.height(), (start, end) -> RowFilters.grayscale(source, output, start, end));
        return new PixelImage(source.width(), source.height(), output);
    }
    @Override public PixelImage convolve(PixelImage source, ConvolutionKernel kernel) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(kernel);
        int[] output = new int[source.width() * source.height()];
        rows(source.height(), (start, end) -> RowFilters.convolve(source, kernel, output, start, end));
        return new PixelImage(source.width(), source.height(), output);
    }
    @Override public PixelImage sobel(PixelImage source) {
        PixelImage gray = grayscale(source);
        int[] output = new int[source.width() * source.height()];
        rows(source.height(), (start, end) -> RowFilters.sobel(source, gray, output, start, end));
        return new PixelImage(source.width(), source.height(), output);
    }

    private void rows(int height, BiConsumer<Integer, Integer> action) {
        if (Thread.currentThread().isInterrupted()) { throw new CancellationException("Caller interrupted"); }
        AtomicBoolean abort = new AtomicBoolean();
        ForkJoinTask<?> root;
        synchronized (this) {
            if (closed) { throw new IllegalStateException("Processor is closed"); }
            root = pool.submit(new Rows(0, height, action, abort));
            active.put(root, abort);
        }
        try { root.get(); }
        catch (InterruptedException exception) {
            abort.set(true);
            Thread.currentThread().interrupt();
            throw new CancellationException("Interrupted while waiting for Fork/Join");
        } catch (ExecutionException exception) {
            abort.set(true);
            if (exception.getCause() instanceof RuntimeException runtime) { throw runtime; }
            if (exception.getCause() instanceof Error error) { throw error; }
            throw new IllegalStateException(exception.getCause());
        } finally {
            abort.set(true);
            root.cancel(true);
            synchronized (this) { active.remove(root); }
        }
    }

    private final class Rows extends RecursiveAction {
        private final int start, end;
        private final BiConsumer<Integer, Integer> action;
        private final AtomicBoolean abort;
        Rows(int start, int end, BiConsumer<Integer, Integer> action, AtomicBoolean abort) {
            this.start = start; this.end = end; this.action = action; this.abort = abort;
        }
        @Override protected void compute() {
            if (abort.get()) { throw new CancellationException("Fork/Join cancelled"); }
            if (end - start <= threshold) {
                for (int row = start; row < end; row++) {
                    if (abort.get() || Thread.currentThread().isInterrupted()) { throw new CancellationException(); }
                    action.accept(row, row + 1);
                }
            } else {
                int middle = start + (end - start) / 2;
                invokeAll(new Rows(start, middle, action, abort), new Rows(middle, end, action, abort));
            }
        }
    }

    @Override public synchronized void close() {
        closed = true;
        active.forEach((task, abort) -> { abort.set(true); task.cancel(true); });
        pool.shutdownNow();
    }
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException { return pool.awaitTermination(timeout, unit); }
}
