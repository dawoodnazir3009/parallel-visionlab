package com.parallelvisionlab.service;

import com.parallelvisionlab.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcessingEnginesTest {
    @Test void reusesPoolAndClosesReplacedAndFinalPools() {
        var engines = new ProcessingEngines();
        var source = new PixelImage(1, 1, new int[]{0xffffffff});
        var first = engines.select(ExecutionBackend.EXECUTOR, 2);
        assertSame(first, engines.select(ExecutionBackend.EXECUTOR, 2));
        var second = engines.select(ExecutionBackend.EXECUTOR, 4);
        assertNotSame(first, second);
        assertThrows(IllegalStateException.class, () -> first.grayscale(source));
        assertEquals(0xffffffff, second.grayscale(source).argbAt(0, 0));
        engines.close();
        engines.close();
        assertThrows(IllegalStateException.class, () -> second.grayscale(source));
        assertThrows(IllegalStateException.class, () -> engines.select(ExecutionBackend.SEQUENTIAL, 1));
    }
    @Test void automaticWorkersAreBoundedAndBadCountsRejected() {
        assertTrue(ProcessingEngines.automaticWorkers() >= 1 && ProcessingEngines.automaticWorkers() <= 8);
        try (var engines = new ProcessingEngines()) {
            assertThrows(IllegalArgumentException.class, () -> engines.select(ExecutionBackend.EXECUTOR, 0));
            assertThrows(IllegalArgumentException.class, () -> engines.select(ExecutionBackend.EXECUTOR, 9));
            assertThrows(IllegalArgumentException.class, () -> engines.select(ExecutionBackend.FORK_JOIN, 9));
            assertThrows(IllegalArgumentException.class, () -> new com.parallelvisionlab.engine.ExecutorProcessor(9));
            assertThrows(IllegalArgumentException.class, () -> new com.parallelvisionlab.engine.ForkJoinProcessor(9,32));
        }
    }
}
