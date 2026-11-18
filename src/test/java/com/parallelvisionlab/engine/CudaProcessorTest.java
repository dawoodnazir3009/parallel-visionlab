package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.*;
import com.parallelvisionlab.service.ImageValidation;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

class CudaProcessorTest {
    @Test @EnabledIfSystemProperty(named="visionlab.cudaTests",matches="true")
    void nativeProtocolSupportsSpacesAndUnicodeWindowsPaths()throws Exception {
        var directory=java.nio.file.Files.createTempDirectory("visionlab unicode-图像-");
        try{
            var input=directory.resolve("输入 image.bin");var output=directory.resolve("输出 image.bin");
            com.parallelvisionlab.service.CudaProtocol.write(input,new PixelImage(1,1,new int[]{0xffff0000}),0,null);
            Process process=new ProcessBuilder(CudaProcessor.executable().toString(),input.toString(),output.toString(),"basic").start();
            try{assertTrue(process.waitFor(10,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());}
            finally{if(process.isAlive())process.destroyForcibly();}
            assertEquals(0xff4c4c4c,com.parallelvisionlab.service.CudaProtocol.read(output,1,1).pixels().argbAt(0,0));
        }finally{try(var files=java.nio.file.Files.list(directory)){for(var file:files.toList())java.nio.file.Files.delete(file);}java.nio.file.Files.delete(directory);}
    }
    @Test @EnabledIfSystemProperty(named="visionlab.cudaTests",matches="true")
    void fiveBackendRunRecordsGpuEventsAndNonCpuEfficiency()throws Exception {
        var run=new com.parallelvisionlab.benchmark.BenchmarkRunner(true).run(new PixelImage(19,23,new int[19*23]),"gpu-fixture",
                new ProcessingRequest(FilterOperation.GAUSSIAN,2,1.2),new com.parallelvisionlab.benchmark.BenchmarkConfig(1,2,2,4),p->{});
        assertEquals(5,run.measurements().size());
        for(var result:run.measurements())if(result.backend().gpu()){
            assertEquals(0,result.workers());assertTrue(Double.isNaN(result.efficiency()));assertTrue(result.kernelMs()>=0);
            assertEquals(1,result.rgbTolerance());
        }
        var directory=java.nio.file.Files.createTempDirectory("visionlab-gpu-archive-test-");
        try{
            var archive=new com.parallelvisionlab.benchmark.BenchmarkArchive(directory);archive.save(run);
            assertEquals(run,archive.load().runs().getFirst());
        }finally{try(var files=java.nio.file.Files.list(directory)){for(var file:files.toList())java.nio.file.Files.delete(file);}java.nio.file.Files.delete(directory);}
    }
    @Test void missingExecutableFailsWithoutSilentCpuFallback() {
        var engine=new CudaProcessor(Path.of("definitely-missing-cuda.exe"),false,Duration.ofSeconds(1));
        assertThrows(IllegalStateException.class,()->engine.grayscale(new PixelImage(1,1,new int[]{0})));
        assertThrows(IllegalArgumentException.class,()->new CudaProcessor(Path.of("x"),false,Duration.ZERO));
    }
    @Test @EnabledIfSystemProperty(named="visionlab.cudaTests",matches="true")
    void nativeKernelsMatchCpuForNarrowAndPartialTiles() {
        var reference=new SequentialProcessor();Random random=new Random(73);
        for(boolean tiled:new boolean[]{false,true}) {
            var engine=new CudaProcessor(CudaProcessor.executable(),tiled,Duration.ofSeconds(60));
            for(int[] shape:new int[][]{{1,1},{1,17},{19,1},{19,23}}) {
                var input=new PixelImage(shape[0],shape[1],random.ints(shape[0]*shape[1]).toArray());
                for(var operation:FilterOperation.values()) {
                    var request=new ProcessingRequest(operation,2,1.2);
                    var expected=reference.process(input,request);var actual=engine.process(input,request);
                    assertTrue(ImageValidation.compare(expected,actual,operation==FilterOperation.SOBEL?1:0).passed(),operation+" tiled="+tiled);
                    assertTrue(engine.lastResult().kernelMs()>=0);
                }
            }
        }
    }
}
