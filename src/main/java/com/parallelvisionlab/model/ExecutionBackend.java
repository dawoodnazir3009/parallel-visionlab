package com.parallelvisionlab.model;

public enum ExecutionBackend {
    SEQUENTIAL("Sequential Java"), EXECUTOR("Java Multithreading");
    private final String label;
    ExecutionBackend(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
