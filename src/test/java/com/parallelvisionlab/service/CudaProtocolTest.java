package com.parallelvisionlab.service;

import com.parallelvisionlab.model.PixelImage;
import java.io.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CudaProtocolTest {
    @Test void framesMultipleRequestsAndOmitsCachedInputPayload()throws Exception {
        var bytes=new ByteArrayOutputStream();var output=new DataOutputStream(bytes);
        var image=new PixelImage(1,1,new int[]{0x12345678});
        CudaProtocol.writeRequest(output,image,0,null,false);
        CudaProtocol.writeRequest(output,image,0,null,true);
        var input=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        for(int reuse=0;reuse<=1;reuse++){
            assertEquals(CudaProtocol.WORKER_REQUEST,input.readInt());assertEquals(1,input.readInt());assertEquals(1,input.readInt());
            assertEquals(0,input.readInt());assertEquals(1,input.readInt());assertEquals(reuse,input.readInt());assertEquals(1,input.readDouble());
            if(reuse==0)assertEquals(0x12345678,input.readInt());
        }
        assertEquals(-1,input.read());
    }

    @Test void decodesConsecutiveStreamFramesAndRejectsTruncatedOrInvalidTimings()throws Exception {
        var bytes=new ByteArrayOutputStream();var output=new DataOutputStream(bytes);
        for(int i=0;i<2;i++){
            output.writeInt(CudaProtocol.WORKER_RESPONSE);output.writeInt(1);output.writeInt(1);
            output.writeDouble(i==0?.5:Double.NaN);output.writeDouble(.2);output.writeDouble(.3);output.writeInt(0x804c4c4c);
        }
        var input=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(0x804c4c4c,CudaProtocol.readResponse(input,1,1).pixels().argbAt(0,0));
        assertThrows(IOException.class,()->CudaProtocol.readResponse(input,1,1));
        assertThrows(IOException.class,()->CudaProtocol.readResponse(new DataInputStream(new ByteArrayInputStream(new byte[2])),1,1));
    }
    @TempDir Path directory;
    @Test void writesPortableRequestAndRejectsMalformedOutputs()throws Exception {
        Path request=directory.resolve("in.bin");CudaProtocol.write(request,new PixelImage(1,1,new int[]{0x12345678}),0,null);
        try(var in=new DataInputStream(Files.newInputStream(request))) {
            assertEquals(0x50564c31,in.readInt());assertEquals(1,in.readInt());assertEquals(1,in.readInt());assertEquals(0,in.readInt());
            assertEquals(1,in.readInt());assertEquals(1,in.readDouble());assertEquals(0x12345678,in.readInt());
        }
        assertThrows(IOException.class,()->CudaProtocol.read(request,1,1));
    }
    @Test void readsPixelsAndTimingsAndChecksDimensions()throws Exception {
        Path file=directory.resolve("out.bin");try(var out=new DataOutputStream(Files.newOutputStream(file))) {
            out.writeInt(0x50564f31);out.writeInt(1);out.writeInt(1);out.writeDouble(.5);out.writeDouble(.2);out.writeDouble(.3);out.writeInt(0x804c4c4c);
        }
        var result=CudaProtocol.read(file,1,1);assertEquals(.5,result.kernelMs());assertEquals(0x804c4c4c,result.pixels().argbAt(0,0));
        assertThrows(IOException.class,()->CudaProtocol.read(file,2,1));
    }
}
