package com.parallelvisionlab.engine;

import java.util.ArrayList;
import java.util.List;

/** Balanced non-overlapping half-open row ranges; never creates empty tasks. */
public final class RowPartitions {
    public record Range(int start, int end) { }
    private RowPartitions() { }
    public static List<Range> create(int height, int workers) {
        if (height < 1 || workers < 1 || workers > 256) {
            throw new IllegalArgumentException("Height must be positive and workers between 1 and 256.");
        }
        int count = Math.min(height, workers);
        List<Range> ranges = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ranges.add(new Range((int) ((long) i * height / count), (int) ((long) (i + 1) * height / count)));
        }
        return List.copyOf(ranges);
    }
}
