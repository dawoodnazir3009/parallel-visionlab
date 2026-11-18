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
