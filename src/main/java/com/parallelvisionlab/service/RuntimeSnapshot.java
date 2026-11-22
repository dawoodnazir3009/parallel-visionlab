package com.parallelvisionlab.service;

import java.lang.management.ManagementFactory;

/** Real JVM and OS measurements; negative CPU load means the provider has no sample. */
public record RuntimeSnapshot(int processors, long usedHeapBytes, long maxHeapBytes, double cpuLoad,
                              long usedMemoryBytes, long totalMemoryBytes) {
    public boolean memoryAvailable() { return totalMemoryBytes > 0 && usedMemoryBytes >= 0; }
    public double memoryFraction() { return memoryAvailable() ? (double) usedMemoryBytes / totalMemoryBytes : Double.NaN; }
    public String memorySummary() {
        return memoryAvailable() ? String.format(java.util.Locale.ROOT, "%.1f / %.1f GB · %.0f%%",
                usedMemoryBytes / 1073741824.0, totalMemoryBytes / 1073741824.0, memoryFraction() * 100) : "Unavailable";
    }
    public static RuntimeSnapshot capture() {
        Runtime runtime = Runtime.getRuntime();
        double load = -1;
        long totalMemory = -1, usedMemory = -1;
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
            load = os.getCpuLoad();
            long total = os.getTotalMemorySize(), free = os.getFreeMemorySize();
            if (total > 0 && free >= 0 && free <= total) {
                totalMemory = total; usedMemory = total - free;
            }
        }
        return new RuntimeSnapshot(runtime.availableProcessors(), runtime.totalMemory() - runtime.freeMemory(),
                runtime.maxMemory(), load, usedMemory, totalMemory);
    }
}
