package com.parallelvisionlab.benchmark;

public record BenchmarkConfig(int warmups, int iterations, int workers, int leafRows) {
    public BenchmarkConfig {
        if (warmups < 0 || warmups > 20 || iterations < 1 || iterations > 100 || workers < 1 || workers > 256
                || leafRows < 1 || leafRows > 2048) { throw new IllegalArgumentException("Invalid benchmark configuration"); }
    }
}
