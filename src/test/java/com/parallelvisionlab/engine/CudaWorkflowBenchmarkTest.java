package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.*;
import com.parallelvisionlab.service.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in same-session comparison of the retained file CLI and persistent pipe worker. */
class CudaWorkflowBenchmarkTest {
    @Test @EnabledIfSystemProperty(named="visionlab.cudaWorkflowBenchmark",matches="true")
    void comparesValidatedLegacyAndPersistentRequests()throws Exception {
        int side=512;int[] pixels=new int[side*side];
        for(int y=0;y<side;y++)for(int x=0;x<side;x++)pixels[y*side+x]=0xff000000|((x%256)<<16)|((y%256)<<8)|((x+y)%256);
        var image=new PixelImage(side,side,pixels);var kernel=GaussianKernels.create(2,1.2);
        var expected=new SequentialProcessor().convolve(image,kernel);
        var folder=Files.createTempDirectory("visionlab-bridge-comparison-");
        StringBuilder csv=new StringBuilder("variant,transport,width,height,warmups,iterations,median_ms,input_cache\n");
        try {
            for(boolean tiled:new boolean[]{false,true}){
                double[] legacy=new double[5],pipe=new double[5];
                for(int index=-2;index<5;index++){
                    long start=System.nanoTime();
                    var input=folder.resolve("input.bin");var output=folder.resolve("output.bin");
                    CudaProtocol.write(input,image,1,kernel);
                    var process=new ProcessBuilder(CudaProcessor.executable().toString(),input.toString(),output.toString(),tiled?"tiled":"basic")
                            .redirectErrorStream(true).redirectOutput(folder.resolve("native.log").toFile()).start();
                    try{assertTrue(process.waitFor(60,TimeUnit.SECONDS));assertEquals(0,process.exitValue());}
                    finally{if(process.isAlive())process.destroyForcibly();}
                    var actual=CudaProtocol.read(output,side,side).pixels();
                    double elapsed=(System.nanoTime()-start)/1_000_000.0;
                    assertTrue(ImageValidation.compare(expected,actual,1).passed());
                    if(index>=0)legacy[index]=elapsed;
                }
                try(var worker=new CudaProcessor(CudaProcessor.executable(),tiled,Duration.ofSeconds(60))){
                    for(int index=-2;index<5;index++){
                        long start=System.nanoTime();var actual=worker.convolve(image,kernel);
                        double elapsed=(System.nanoTime()-start)/1_000_000.0;
                        assertTrue(ImageValidation.compare(expected,actual,1).passed());
                        if(index>=0)pipe[index]=elapsed;
                    }
                }
                Arrays.sort(legacy);Arrays.sort(pipe);String variant=tiled?"tiled":"basic";
                csv.append(String.format(Locale.ROOT,"%s,file-process,512,512,2,5,%.3f,none\n",variant,legacy[2]));
                csv.append(String.format(Locale.ROOT,"%s,persistent-pipe,512,512,2,5,%.3f,immutable-identity\n",variant,pipe[2]));
                System.out.printf(Locale.ROOT,"%s CUDA bridge: file %.3f ms -> persistent %.3f ms (%.2fx)\n",variant,legacy[2],pipe[2],legacy[2]/pipe[2]);
            }
            Files.writeString(Path.of("target","cuda-workflow-comparison.csv"),csv);
        } finally {
            for(String name:new String[]{"input.bin","output.bin","native.log"})Files.deleteIfExists(folder.resolve(name));
            Files.deleteIfExists(folder);
        }
    }
}
