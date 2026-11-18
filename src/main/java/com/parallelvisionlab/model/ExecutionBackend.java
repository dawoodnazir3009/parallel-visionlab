package com.parallelvisionlab.model;

public enum ExecutionBackend {
    SEQUENTIAL("Sequential Java"), EXECUTOR("Java Multithreading"), FORK_JOIN("Java Fork/Join"),
    CUDA_BASIC("CUDA GPU — Basic Kernel"), CUDA_TILED("CUDA GPU — Tiled Kernel");
    private final String label;
    ExecutionBackend(String label) { this.label = label; }
    @Override public String toString() { return label; }
    public boolean gpu(){return this==CUDA_BASIC||this==CUDA_TILED;}
}
