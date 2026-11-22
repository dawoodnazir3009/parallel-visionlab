package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.*;
import com.parallelvisionlab.service.CudaProtocol;
import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;

/** Persistent binary-pipe CUDA worker with bounded requests and explicit ownership. */
public final class CudaProcessor implements ImageProcessor, AutoCloseable {
    private final Path executable;
    private final Duration timeout;
    private final Object requestLock = new Object();
    private final Object lifecycle = new Object();
    private final ExecutorService exchange = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon(true).name("visionlab-cuda-pipe").factory());
    private volatile Worker worker;
    private volatile boolean closed;
    private volatile CudaProtocol.Result lastResult;
    private Future<CudaProtocol.Result> inFlight;
    private final WorkerLauncher launcher;
    @FunctionalInterface interface WorkerLauncher { Process start() throws IOException; }

    public CudaProcessor(Path executable,boolean tiled,Duration timeout){
        this(executable,tiled,timeout,()->new ProcessBuilder(executable.toAbsolutePath().toString(),"--worker",tiled?"tiled":"basic").start());
    }
    CudaProcessor(Path executable,boolean tiled,Duration timeout,WorkerLauncher launcher){
        if(timeout.isNegative()||timeout.isZero())throw new IllegalArgumentException("Timeout must be positive");
        this.executable=executable.toAbsolutePath();this.timeout=timeout;
        this.launcher=launcher;
    }
    public static Path executable() {
        String configured=System.getProperty("visionlab.cuda.path",System.getenv("VISIONLAB_CUDA_PATH"));
        if(configured!=null&&!configured.isBlank())return Path.of(configured).toAbsolutePath();
        Path packaged=Path.of("visionlab_cuda.exe");
        return Files.isRegularFile(packaged)?packaged.toAbsolutePath():Path.of("native","cuda","build","Release","visionlab_cuda.exe").toAbsolutePath();
    }
    public static boolean available(){return Files.isRegularFile(executable());}
    public CudaProtocol.Result lastResult(){return lastResult;}
    public void clearLastResult(){lastResult=null;}
    long workerPid(){Worker current=worker;return current==null?-1:current.process.pid();}
    @Override public PixelImage grayscale(PixelImage source){return execute(source,0,null);}
    @Override public PixelImage convolve(PixelImage source,ConvolutionKernel kernel){return execute(source,1,java.util.Objects.requireNonNull(kernel));}
    @Override public PixelImage sobel(PixelImage source){return execute(source,2,null);}

    private PixelImage execute(PixelImage source,int operation,ConvolutionKernel kernel) {
        java.util.Objects.requireNonNull(source,"source");
        synchronized(requestLock){
            if(closed)throw new IllegalStateException("CUDA processor is closed");
            lastResult=null;
            if(Thread.currentThread().isInterrupted())throw new CancellationException("CUDA call interrupted");
            if(!Files.isRegularFile(executable))throw new IllegalStateException("CUDA executable missing. Run build-cuda.ps1 first.");
            Future<CudaProtocol.Result> pending;
            synchronized(lifecycle){
                if(closed)throw new IllegalStateException("CUDA processor is closed");
                pending=exchange.submit(()->exchangeRequest(source,operation,kernel));
                inFlight=pending;
            }
            try {
                var result=pending.get(timeout.toNanos(),TimeUnit.NANOSECONDS);
                synchronized(lifecycle){
                    if(closed)throw new CancellationException("CUDA processor closed during request");
                    lastResult=result;
                }
                return result.pixels();
            } catch(InterruptedException exception){
                pending.cancel(true);stopWorker();Thread.currentThread().interrupt();
                throw new CancellationException("CUDA call interrupted");
            } catch(TimeoutException exception){
                pending.cancel(true);stopWorker();
                throw new IllegalStateException("CUDA worker request timed out after "+timeout.toMillis()+" ms",exception);
            } catch(ExecutionException exception){
                stopWorker();
                throw new IllegalStateException("CUDA worker failed: "+detail(exception.getCause())
                        +". Rebuild the native executable with build-cuda.ps1 if its protocol is outdated.",exception.getCause());
            } finally {
                synchronized(lifecycle){inFlight=null;}
            }
        }
    }

    private CudaProtocol.Result exchangeRequest(PixelImage source,int operation,ConvolutionKernel kernel)throws IOException {
        Worker current=ensureWorker();
        try {
            boolean reuse=current.input.get()==source;
            CudaProtocol.writeRequest(current.output,source,operation,kernel,reuse);
            var result=CudaProtocol.readResponse(current.inputStream,source.width(),source.height());
            current.input=new WeakReference<>(source);
            return result;
        } catch(IOException exception){
            String details=current.errors();stopWorker();
            throw new IOException(detail(exception)+(details.isBlank()?"":"; "+details),exception);
        }
    }

    private Worker ensureWorker()throws IOException {
        Worker current;
        synchronized(lifecycle){
            if(closed)throw new IOException("CUDA processor is closed");
            current=worker;
            // Process.isAlive can briefly lag a ProcessHandle exit notification on Windows.
            if(current!=null&&current.process.toHandle().isAlive())return current;
            if(current!=null)current.stop();
            current=new Worker(launcher.start());
            worker=current;
        }
        try {
            if(current.inputStream.readInt()!=CudaProtocol.WORKER_READY)
                throw new IOException("CUDA worker handshake version mismatch");
            return current;
        } catch(IOException exception){
            String errors=current.errors();stopWorker();
            throw new IOException("CUDA worker could not start: "+detail(exception)+" "+errors,exception);
        }
    }

    private static String detail(Throwable error){
        return error.getMessage()==null?error.getClass().getSimpleName():error.getMessage();
    }

    private void stopWorker(){
        synchronized(lifecycle){
            Worker current=worker;worker=null;
            if(current!=null)current.stop();
        }
    }

    @Override public void close(){
        synchronized(lifecycle){closed=true;if(inFlight!=null)inFlight.cancel(true);}
        stopWorker();exchange.shutdownNow();lastResult=null;
    }

    private static final class Worker {
        final Process process;
        final DataInputStream inputStream;
        final DataOutputStream output;
        final ByteArrayOutputStream diagnostic=new ByteArrayOutputStream();
        final Thread diagnosticReader;
        WeakReference<PixelImage> input=new WeakReference<>(null);
        Worker(Process process){
            this.process=process;
            inputStream=new DataInputStream(new BufferedInputStream(process.getInputStream(),64*1024));
            output=new DataOutputStream(new BufferedOutputStream(process.getOutputStream(),64*1024));
            diagnosticReader=Thread.ofPlatform().daemon(true).name("visionlab-cuda-stderr").start(()->{
                try(var errors=process.getErrorStream()){
                    byte[] bytes=new byte[1024];int count;
                    while((count=errors.read(bytes))>=0){
                        synchronized(diagnostic){int remaining=4096-diagnostic.size();if(remaining>0)diagnostic.write(bytes,0,Math.min(remaining,count));}
                    }
                }catch(IOException ignored){ }
            });
        }
        String errors(){
            if(!process.toHandle().isAlive()){
                try{diagnosticReader.join(100);}catch(InterruptedException exception){Thread.currentThread().interrupt();}
            }
            synchronized(diagnostic){return diagnostic.toString(StandardCharsets.UTF_8).trim();}
        }
        void stop(){
            // Kill before closing pipes, so blocked reads/writes are unblocked.
            process.destroyForcibly();
            input.clear();
            Thread.ofPlatform().daemon(true).name("visionlab-cuda-cleanup").start(()->{
                boolean interrupted=false;
                try{process.waitFor();}catch(InterruptedException exception){interrupted=true;}
                try{inputStream.close();}catch(IOException ignored){ }
                try{output.close();}catch(IOException ignored){ }
                if(interrupted)Thread.currentThread().interrupt();
            });
        }
    }
}
