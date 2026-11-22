package com.parallelvisionlab.benchmark;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkPreferencesTest {
    @TempDir Path directory;
    @Test void migratesSavedWorkerCountsWithoutDiscardingOtherPreferences() throws Exception {
        var store=new BenchmarkPreferences(directory.resolve("legacy.properties"));
        store.save(new BenchmarkConfig(2,7,16,48));
        assertEquals(new BenchmarkConfig(2,7,8,48),store.load(null));
    }
    @Test void roundTripAndInvalidValues()throws Exception {
        Path file=directory.resolve("preferences.properties");var store=new BenchmarkPreferences(file);
        var defaults=new BenchmarkConfig(3,10,2,32);
        assertEquals(defaults,store.load(defaults));store.save(defaults);assertEquals(defaults,store.load(null));
        Files.writeString(file,"warmups=-2");assertThrows(java.io.IOException.class,()->store.load(defaults));
    }
}
