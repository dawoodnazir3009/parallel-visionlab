package com.parallelvisionlab.service;

import com.parallelvisionlab.model.PixelImage;
import java.io.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CudaProtocolTest {
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
