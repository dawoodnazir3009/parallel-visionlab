package com.parallelvisionlab.model;

import java.util.Objects;

/** Immutable options snapshot so UI edits cannot change a running operation. */
public record ProcessingRequest(FilterOperation operation, int gaussianRadius, double gaussianSigma) {
    public ProcessingRequest {
        Objects.requireNonNull(operation, "operation");
        if (operation == FilterOperation.GAUSSIAN && (gaussianRadius < 1 || gaussianRadius > 7
                || !Double.isFinite(gaussianSigma) || gaussianSigma < 0.1 || gaussianSigma > 10)) {
            throw new IllegalArgumentException("Gaussian radius must be 1-7 and sigma 0.1-10.");
        }
    }
}
