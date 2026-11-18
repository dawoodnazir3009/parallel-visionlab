package com.parallelvisionlab.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NvidiaTelemetryTest {
    @Test void parsesDetectedValuesAndPreservesUnavailablePower() {
        var value=NvidiaTelemetry.parse("NVIDIA RTX 4050, 42, 1024, 6144, N/A, 58");
        assertEquals(42,value.utilization());assertEquals(6144,value.totalMiB());assertTrue(Double.isNaN(value.watts()));assertEquals(58,value.temperature());
        assertTrue(Double.isNaN(NvidiaTelemetry.parse("GPU, 42, 1, 6, [N/A], 58").watts()));
    }
    @Test void rejectsMalformedOrImpossibleLoad() {
        assertThrows(IllegalArgumentException.class,()->NvidiaTelemetry.parse("not telemetry"));
        assertThrows(IllegalArgumentException.class,()->NvidiaTelemetry.parse("GPU, 200, 1, 6, 10, 50"));
    }
}
