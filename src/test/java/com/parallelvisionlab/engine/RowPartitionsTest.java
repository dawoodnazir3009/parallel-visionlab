package com.parallelvisionlab.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RowPartitionsTest {
    @Test void coversEveryRowOnceForOddSizesAndExcessWorkers() {
        for (int height : new int[]{1, 2, 7, 17, Integer.MAX_VALUE}) {
            for (int workers : new int[]{1, 2, 8, 256}) {
                int next = 0, shortest = Integer.MAX_VALUE, longest = 0;
                for (var range : RowPartitions.create(height, workers)) {
                    assertEquals(next, range.start());
                    assertTrue(range.end() > range.start());
                    int length = range.end() - range.start();
                    shortest = Math.min(shortest, length);
                    longest = Math.max(longest, length);
                    next = range.end();
                }
                assertEquals(height, next);
                assertTrue(longest - shortest <= 1);
            }
        }
    }
    @Test void rejectsInvalidSettings() {
        assertThrows(IllegalArgumentException.class, () -> RowPartitions.create(0, 1));
        assertThrows(IllegalArgumentException.class, () -> RowPartitions.create(1, 0));
        assertThrows(IllegalArgumentException.class, () -> RowPartitions.create(1, 257));
    }
}
