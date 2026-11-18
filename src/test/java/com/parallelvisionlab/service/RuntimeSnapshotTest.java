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
    }
}
