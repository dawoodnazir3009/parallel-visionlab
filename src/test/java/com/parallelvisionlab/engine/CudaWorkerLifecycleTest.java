package com.parallelvisionlab.engine;

import com.parallelvisionlab.model.PixelImage;
import com.parallelvisionlab.service.CudaProtocol;
import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Protocol/process failure tests run without an NVIDIA GPU or native toolchain. */
class CudaWorkerLifecycleTest {
    private static final Path JAVA=Path.of(System.getProperty("java.home"),"bin",
            System.getProperty("os.name").startsWith("Windows")?"java.exe":"java");
    private static final PixelImage IMAGE=new PixelImage(1,1,new int[]{0xff123456});

    private static Process launch(String mode)throws IOException {
        return new ProcessBuilder(JAVA.toString(),"-cp",
                System.getProperty("surefire.test.class.path",System.getProperty("java.class.path")),
                FakeWorker.class.getName(),mode).start();
    }

    @Test void restartsAfterMalformedHandshakeAndRejectsCallsAfterClose()throws Exception {
        AtomicInteger launches=new AtomicInteger();
        try(var engine=new CudaProcessor(JAVA,false,Duration.ofSeconds(5),
                ()->launch(launches.getAndIncrement()==0?"bad-handshake":"echo"))){
            assertThrows(IllegalStateException.class,()->engine.grayscale(IMAGE));
            assertEquals(IMAGE.argbAt(0,0),engine.grayscale(IMAGE).argbAt(0,0));
            long pid=engine.workerPid();
            assertEquals(IMAGE.argbAt(0,0),engine.grayscale(IMAGE).argbAt(0,0));
            assertEquals(pid,engine.workerPid());assertEquals(2,launches.get());
            engine.close();
            assertThrows(IllegalStateException.class,()->engine.grayscale(IMAGE));
        }
    }

    @Test void timesOutBlockedHandshakeAndResponseAndKillsWorker()throws Exception {
        for(String mode:new String[]{"stall-handshake","stall-response"}){
            AtomicReference<Process> process=new AtomicReference<>();
            try(var engine=new CudaProcessor(JAVA,false,Duration.ofSeconds(2),()->{
                var child=launch(mode);process.set(child);return child;
            })){
                var failure=assertThrows(IllegalStateException.class,()->engine.grayscale(IMAGE));
                assertTrue(failure.getMessage().contains("timed out"));
                assertTrue(process.get().waitFor(5,TimeUnit.SECONDS));
                assertFalse(process.get().isAlive());
            }
        }
    }

    @Test void interruptionUnblocksPipeExchangeAndTerminatesWorker()throws Exception {
        AtomicReference<Process> process=new AtomicReference<>();CountDownLatch started=new CountDownLatch(1);
        AtomicInteger launches=new AtomicInteger();
        try(var engine=new CudaProcessor(JAVA,false,Duration.ofSeconds(30),()->{
            var child=launch(launches.getAndIncrement()==0?"stall-response":"echo");process.set(child);started.countDown();return child;
        });var caller=Executors.newSingleThreadExecutor()){
            Future<?> request=caller.submit(()->engine.grayscale(IMAGE));
            assertTrue(started.await(5,TimeUnit.SECONDS));request.cancel(true);
            assertTrue(process.get().waitFor(5,TimeUnit.SECONDS));
            assertFalse(process.get().isAlive());
            assertEquals(IMAGE.argbAt(0,0),engine.grayscale(IMAGE).argbAt(0,0));
        }
    }

    @Test void closingDuringExchangeCancelsPendingRequest()throws Exception {
        AtomicReference<Process> process=new AtomicReference<>();CountDownLatch started=new CountDownLatch(1);
        try(var engine=new CudaProcessor(JAVA,false,Duration.ofSeconds(30),()->{
            var child=launch("stall-response");process.set(child);started.countDown();return child;
        });var caller=Executors.newSingleThreadExecutor()){
            Future<?> request=caller.submit(()->engine.grayscale(IMAGE));
            assertTrue(started.await(5,TimeUnit.SECONDS));engine.close();
            assertThrows(ExecutionException.class,()->request.get(5,TimeUnit.SECONDS));
            assertTrue(process.get().waitFor(5,TimeUnit.SECONDS));
            assertFalse(process.get().isAlive());
        }
    }

    public static class FakeWorker {
        public static void main(String[] args)throws Exception {
            var out=new DataOutputStream(System.out);String mode=args[0];
            if(mode.equals("stall-handshake")){Thread.sleep(20000);return;}
            out.writeInt(mode.equals("bad-handshake")?0:CudaProtocol.WORKER_READY);out.flush();
            if(mode.equals("bad-handshake"))return;
            if(mode.equals("stall-response")){Thread.sleep(20000);return;}
            var in=new DataInputStream(System.in);int[] pixels=null;
            while(true){
                try{if(in.readInt()!=CudaProtocol.WORKER_REQUEST)return;}catch(EOFException end){return;}
                int width=in.readInt(),height=in.readInt();in.readInt();int size=in.readInt(),reuse=in.readInt();
                for(int i=0;i<size*size;i++)in.readDouble();
                if(reuse==0){pixels=new int[width*height];for(int i=0;i<pixels.length;i++)pixels[i]=in.readInt();}
                out.writeInt(CudaProtocol.WORKER_RESPONSE);out.writeInt(width);out.writeInt(height);
                out.writeDouble(0);out.writeDouble(0);out.writeDouble(0);
                for(int pixel:pixels)out.writeInt(pixel);out.flush();
            }
        }
    }
}
