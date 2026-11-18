package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.*;
import com.parallelvisionlab.service.CudaProtocol;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;

/** Standalone executable bridge, without JNI; every invocation has isolated files and a timeout. */
public final class CudaProcessor implements ImageProcessor {
    private final Path executable;
    private final boolean tiled;
    private final Duration timeout;
    private volatile CudaProtocol.Result lastResult;
    public CudaProcessor(Path executable,boolean tiled,Duration timeout){
        if(timeout.isNegative()||timeout.isZero())throw new IllegalArgumentException("Timeout must be positive");
        this.executable=executable.toAbsolutePath();this.tiled=tiled;this.timeout=timeout;
    }
    public static Path executable() {
        String configured=System.getProperty("visionlab.cuda.path",System.getenv("VISIONLAB_CUDA_PATH"));
        if(configured!=null&&!configured.isBlank())return Path.of(configured).toAbsolutePath();
        Path packaged=Path.of("visionlab_cuda.exe");
        return Files.isRegularFile(packaged)?packaged.toAbsolutePath():Path.of("native","cuda","build","Release","visionlab_cuda.exe").toAbsolutePath();
    }
    public static boolean available(){return Files.isRegularFile(executable());}
    public CudaProtocol.Result lastResult(){return lastResult;}
    @Override public PixelImage grayscale(PixelImage source){return execute(source,0,null);}
    @Override public PixelImage convolve(PixelImage source,ConvolutionKernel kernel){return execute(source,1,java.util.Objects.requireNonNull(kernel));}
    @Override public PixelImage sobel(PixelImage source){return execute(source,2,null);}

    private PixelImage execute(PixelImage source,int operation,ConvolutionKernel kernel) {
        if(Thread.currentThread().isInterrupted())throw new CancellationException();
        if(!Files.isRegularFile(executable))throw new IllegalStateException("CUDA executable missing. Run build-cuda.ps1 first.");
        Path directory=null;Process process=null;
        try {
            directory=Files.createTempDirectory("visionlab-cuda-");
            Path input=directory.resolve("input.bin"),output=directory.resolve("output.bin"),log=directory.resolve("native.log");
            CudaProtocol.write(input,source,operation,kernel);
            process=new ProcessBuilder(executable.toString(),input.toString(),output.toString(),tiled?"tiled":"basic")
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if(!process.waitFor(timeout.toMillis(),TimeUnit.MILLISECONDS))throw new IllegalStateException("CUDA process timed out after "+timeout.toSeconds()+" seconds");
            if(process.exitValue()!=0){
                String detail=Files.readString(log);if(detail.length()>2000)detail=detail.substring(0,2000);
                throw new IllegalStateException("CUDA failed (exit "+process.exitValue()+"): "+detail);
            }
            lastResult=CudaProtocol.read(output,source.width(),source.height());
            return lastResult.pixels();
        } catch(InterruptedException exception){Thread.currentThread().interrupt();throw new CancellationException("CUDA call interrupted");}
        catch(IOException exception){throw new IllegalStateException("CUDA bridge I/O failed: "+exception.getMessage(),exception);}
        finally {
            if(process!=null&&process.isAlive()){
                process.destroyForcibly();
                try{process.waitFor(5,TimeUnit.SECONDS);}catch(InterruptedException exception){Thread.currentThread().interrupt();}
            }
            if(directory!=null){
                for(String name:new String[]{"input.bin","output.bin","native.log"})try{Files.deleteIfExists(directory.resolve(name));}catch(IOException ignored){ }
                try{Files.deleteIfExists(directory);}catch(IOException ignored){ }
            }
        }
    }
}
