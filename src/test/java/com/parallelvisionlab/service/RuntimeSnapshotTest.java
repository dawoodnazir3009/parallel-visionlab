package com.parallelvisionlab.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeSnapshotTest {
    @Test void reportsBoundedRealRuntimeResources() {
        RuntimeSnapshot snapshot = RuntimeSnapshot.capture();
        assertTrue(snapshot.processors() > 0);
        assertTrue(snapshot.maxHeapBytes() > 0);
        assertTrue(snapshot.usedHeapBytes() >= 0);
        assertTrue(snapshot.usedHeapBytes() <= snapshot.maxHeapBytes());
        assertTrue(snapshot.cpuLoad() == -1 || (snapshot.cpuLoad() >= 0 && snapshot.cpuLoad() <= 1));
        if (snapshot.memoryAvailable()) {
            assertTrue(snapshot.usedMemoryBytes() <= snapshot.totalMemoryBytes());
            assertTrue(snapshot.memoryFraction() >= 0 && snapshot.memoryFraction() <= 1);
            assertTrue(snapshot.memorySummary().contains("GB"));
        }
    }

    @Test void formatsPhysicalMemorySeparatelyFromJvmHeap() {
        long gib = 1024L * 1024 * 1024;
        var snapshot = new RuntimeSnapshot(16, 32*1024*1024, gib, 0.5, 8*gib, 16*gib);
        assertEquals("8.0 / 16.0 GB · 50%", snapshot.memorySummary());
        assertEquals(0.5, snapshot.memoryFraction());
        var unavailable = new RuntimeSnapshot(16, 0, gib, -1, -1, -1);
        assertFalse(unavailable.memoryAvailable());
        assertTrue(Double.isNaN(unavailable.memoryFraction()));
        assertEquals("Unavailable", unavailable.memorySummary());
    }
}
