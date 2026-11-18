package com.parallelvisionlab;

import com.parallelvisionlab.engine.*;
import com.parallelvisionlab.model.*;
import com.parallelvisionlab.service.*;
import java.nio.file.*;
import java.time.Duration;

/** Plain launcher also supports a no-display packaged-runtime verification mode. */
public final class VisionLabLauncher {
    private VisionLabLauncher() { }
    public static void main(String[] args)throws Exception {
        if(args.length==1&&args[0].equals("--self-test")){selfTest();return;}
        javafx.application.Application.launch(VisionLabApplication.class,args);
    }
    private static void selfTest()throws Exception {
        int[] pixels=new int[19*23];
        java.util.Random random=new java.util.Random(19);for(int i=0;i<pixels.length;i++)pixels[i]=random.nextInt();
        var source=new PixelImage(19,23,pixels);var reference=new SequentialProcessor();
        try(var engines=new ProcessingEngines()){
            for(var backend:ExecutionBackend.values())for(var operation:FilterOperation.values()){
                var request=new ProcessingRequest(operation,2,1.2);
                if(!ImageValidation.compare(reference.process(source,request),engines.select(backend,2,4).process(source,request),0).passed())throw new IllegalStateException("CPU self-test failed: "+backend+" "+operation);
            }
        }
        if(CudaProcessor.available()){
            for(boolean tiled:new boolean[]{false,true})for(var operation:FilterOperation.values()){
                var request=new ProcessingRequest(operation,2,1.2);
                var gpu=new CudaProcessor(CudaProcessor.executable(),tiled,Duration.ofSeconds(60));
                if(!ImageValidation.compare(reference.process(source,request),gpu.process(source,request),1).passed())throw new IllegalStateException("GPU self-test failed: "+operation);
            }
        }
        Path file=Files.createTempFile("visionlab-selftest-",".png");
        try{new PngExporter().save(source,file);if(javax.imageio.ImageIO.read(file.toFile()).getWidth()!=19)throw new IllegalStateException("PNG self-test failed");}
        finally{Files.deleteIfExists(file);}
        System.out.println("Parallel VisionLab runtime self-test PASS · 3 CPU engines · 4 filters · PNG export · GPU "+(CudaProcessor.available()?"PASS":"not installed"));
    }
}
