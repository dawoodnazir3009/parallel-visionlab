package com.parallelvisionlab.ui;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DemoBenchmarksTest {
    @Test void sampleCsvKeepsProvenanceOnEveryRow() {
        String[] lines = DemoBenchmarks.csv().split("\r\n");
        assertEquals(6, lines.length);
        assertEquals("backend,duration_ms,speedup,data_type", lines[0]);
        for (int i = 1; i < lines.length; i++) {
            assertEquals(4, lines[i].split(",").length);
            assertTrue(lines[i].endsWith(",sample"));
        }
        assertEquals("CUDA Tiled,18,6.67,sample", lines[5]);
    }

    @Test void csvRemainsValidUnderCommaDecimalLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertTrue(DemoBenchmarks.csv().contains("Multithreading,54,2.22,sample\r\n"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test void baselineAndFastestSampleHaveCorrectRelativeSpeedup() {
        assertEquals(1.0, DemoBenchmarks.SAMPLES.getFirst().speedup());
        assertEquals(120.0 / 18.0, DemoBenchmarks.SAMPLES.getLast().speedup(), 0.0001);
    }
}
