package com.parallelvisionlab.model;

public enum ExecutionBackend {
    SEQUENTIAL("Sequential Java"), EXECUTOR("Java Multithreading"), FORK_JOIN("Java Fork/Join");
    private final String label;
    ExecutionBackend(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
