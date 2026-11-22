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
    @Test @EnabledIfSystemProperty(named="visionlab.largeCudaTests",matches="true")
    void nativeKernelsProcessFullResolution48MegapixelPhoto() throws Exception {
        var image=new com.parallelvisionlab.service.ImageLoader().load(Path.of(System.getProperty("visionlab.testImage")));
        var input=com.parallelvisionlab.service.PixelImages.fromFx(image);
        assertEquals(8064L*6048,(long)input.width()*input.height());
        var reference=new SequentialProcessor();
        try(var basic=new CudaProcessor(CudaProcessor.executable(),false,Duration.ofSeconds(60));
            var tiled=new CudaProcessor(CudaProcessor.executable(),true,Duration.ofSeconds(60))){
            for(var operation:FilterOperation.values()) {
                var request=new ProcessingRequest(operation,2,1.2);
                var expected=reference.process(input,request);
                for(var engine:java.util.List.of(basic,tiled)) {
                    var actual=engine.process(input,request);
                    assertTrue(ImageValidation.compare(expected,actual,1).passed(),operation+" tiled="+(engine==tiled));
                    engine.clearLastResult();
                    assertNull(engine.lastResult());
                    assertEquals(expected.argbAt(0,0)>>>24,actual.argbAt(0,0)>>>24);
                    System.out.println("48MP CUDA validated: "+operation+" tiled="+(engine==tiled));
                }
            }
        }
    }
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
        assertTrue(com.parallelvisionlab.benchmark.BenchmarkCsv.encode(java.util.List.of(run)).contains("persistent-pipe-wall-clock"));
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
        try(var engine=new CudaProcessor(Path.of("definitely-missing-cuda.exe"),false,Duration.ofSeconds(1))){
            assertThrows(IllegalStateException.class,()->engine.grayscale(new PixelImage(1,1,new int[]{0})));
        }
        assertThrows(IllegalArgumentException.class,()->new CudaProcessor(Path.of("x"),false,Duration.ZERO));
    }
    @Test @EnabledIfSystemProperty(named="visionlab.cudaTests",matches="true")
    void nativeKernelsMatchCpuForNarrowAndPartialTiles() {
        var reference=new SequentialProcessor();Random random=new Random(73);
        for(boolean tiled:new boolean[]{false,true}) {
            try(var engine=new CudaProcessor(CudaProcessor.executable(),tiled,Duration.ofSeconds(60))){
                for(int[] shape:new int[][]{{1,1},{1,17},{19,1},{19,23},{2,2},{31,17}}) {
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

    @Test @EnabledIfSystemProperty(named="visionlab.cudaTests",matches="true")
    void reusesWorkerAndReplacesCachedPixelsAndClosesOwnedProcesses()throws Exception {
        var engines=new com.parallelvisionlab.service.ProcessingEngines();
        var basic=(CudaProcessor)engines.select(ExecutionBackend.CUDA_BASIC,1);
        try {
            var red=new PixelImage(2,1,new int[]{0xffff0000,0xff0000ff});
            var green=new PixelImage(2,1,new int[]{0xff00ff00,0xff00ff00});
            assertEquals(0xff4c4c4c,basic.grayscale(red).argbAt(0,0));
            long pid=basic.workerPid();
            assertTrue(pid>0);
            assertEquals(0xff4c4c4c,basic.grayscale(red).argbAt(0,0));
            assertEquals(pid,basic.workerPid());
            assertEquals(0xff969696,basic.grayscale(green).argbAt(0,0));
            assertEquals(pid,basic.workerPid());
            assertSame(basic,engines.select(ExecutionBackend.CUDA_BASIC,1));
            ProcessHandle.of(pid).orElseThrow().destroyForcibly();
            ProcessHandle.of(pid).ifPresent(handle->{
                try{handle.onExit().get(5,java.util.concurrent.TimeUnit.SECONDS);}catch(Exception error){throw new RuntimeException(error);}
            });
            assertEquals(0xff969696,basic.grayscale(green).argbAt(0,0));
            assertNotEquals(pid,basic.workerPid());
            pid=basic.workerPid();
            engines.releaseCudaResources();
            var handle=ProcessHandle.of(pid);
            if(handle.isPresent())handle.get().onExit().get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
            assertThrows(IllegalStateException.class,()->basic.grayscale(red));
            assertNotSame(basic,engines.select(ExecutionBackend.CUDA_BASIC,1));
        } finally { engines.close(); }
    }
}
