package com.parallelvisionlab.benchmark;

import java.util.List;

/** Locale-independent CSV; every measured row carries its own configuration and provenance. */
public final class BenchmarkCsv {
    private BenchmarkCsv() { }
    public static String encode(List<BenchmarkRun> runs) {
        StringBuilder csv = new StringBuilder("run_id,timestamp,source,image_sha256,width,height,operation,radius,sigma,backend,workers,leaf_rows,warmups,iterations,median_ms,min_ms,max_ms,speedup,efficiency,megapixels_per_second,validation,timing_scope,java,os,data_type\r\n");
        for (var run : runs) for (var m : run.measurements()) {
            Object[] fields = {run.id(), run.timestamp(), run.source(), run.imageHash(), run.width(), run.height(),
                    run.request().operation(), run.request().gaussianRadius(), run.request().gaussianSigma(), m.backend(), m.workers(),
                    run.config().leafRows(), run.config().warmups(), run.config().iterations(), m.medianMs(), m.minimumMs(), m.maximumMs(),
                    m.speedup(), m.efficiency(), m.megapixelsPerSecond(), "exact-pass", "engine-wall-clock", run.javaVersion(),run.operatingSystem(),"measured"};
            for (int i=0; i<fields.length; i++) { if(i>0)csv.append(','); csv.append(quote(String.valueOf(fields[i]))); }
            csv.append("\r\n");
        }
        return csv.toString();
    }
    static String quote(String field) {
        // Avoid interpreting user-controlled filenames as spreadsheet formulas.
        if (!field.isEmpty() && "=+-@\t\r".indexOf(field.charAt(0)) >= 0) { field = "'" + field; }
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }
}
