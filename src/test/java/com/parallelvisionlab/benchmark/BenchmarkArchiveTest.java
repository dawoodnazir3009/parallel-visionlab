package com.parallelvisionlab.benchmark;

import com.parallelvisionlab.model.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkArchiveTest {
    @TempDir Path directory;
    @Test void persistsAndRestoresAllExperimentFieldsAndSkipsCorruptFiles()throws Exception {
        var run=new BenchmarkRunner().run(new PixelImage(1,1,new int[]{0}),"image,one.png",
                new ProcessingRequest(FilterOperation.GRAYSCALE,1,1),new BenchmarkConfig(0,1,1,1),p->{});
        var archive=new BenchmarkArchive(directory);archive.save(run);
        assertEquals(run,archive.load().runs().getFirst());
        Files.writeString(directory.resolve("broken.properties"),"version=bad");
        assertEquals(1,archive.load().skipped());
        assertEquals(1,archive.load().runs().size());
    }
}
