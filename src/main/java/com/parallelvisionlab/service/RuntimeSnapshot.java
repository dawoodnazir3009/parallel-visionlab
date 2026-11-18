package com.parallelvisionlab.service;

import java.lang.management.ManagementFactory;

/** Real JVM and OS measurements; negative CPU load means the provider has no sample. */
public record RuntimeSnapshot(int processors, long usedHeapBytes, long maxHeapBytes, double cpuLoad) {
    public static RuntimeSnapshot capture() {
        Runtime runtime = Runtime.getRuntime();
        double load = -1;
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
            load = os.getCpuLoad();
        }
        return new RuntimeSnapshot(runtime.availableProcessors(), runtime.totalMemory() - runtime.freeMemory(),
                runtime.maxMemory(), load);
    }
}
