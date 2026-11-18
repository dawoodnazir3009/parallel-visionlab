package com.parallelvisionlab.ui;

import java.util.List;
import java.util.Locale;

/** Explicitly illustrative reference data from the supplied HTML, never measured results. */
final class DemoBenchmarks {
    record Sample(String backend, double milliseconds) {
        double speedup() { return 120.0 / milliseconds; }
        String timeLabel() { return String.format(Locale.ROOT, "%.0f ms", milliseconds); }
        String speedupLabel() { return String.format(Locale.ROOT, "%.2fx", speedup()); }
    }

    static final List<Sample> SAMPLES = List.of(
            new Sample("Sequential Java", 120), new Sample("Multithreading", 54),
            new Sample("Fork/Join", 48), new Sample("CUDA Basic", 27), new Sample("CUDA Tiled", 18));

    private DemoBenchmarks() { }

    static String csv() {
        StringBuilder csv = new StringBuilder("backend,duration_ms,speedup,data_type\r\n");
        for (Sample sample : SAMPLES) {
            csv.append(sample.backend()).append(',')
                    .append(String.format(Locale.ROOT, "%.0f,%.2f,sample\r\n", sample.milliseconds(), sample.speedup()));
        }
        return csv.toString();
    }
}
