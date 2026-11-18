package com.parallelvisionlab.service;

import com.parallelvisionlab.model.*;
import java.io.*;
import java.nio.file.*;

/** Version 1 big-endian file protocol for the standalone native executable. */
public final class CudaProtocol {
    public record Result(PixelImage pixels,double kernelMs,double uploadMs,double downloadMs) { }
    private CudaProtocol() { }
    public static void write(Path file,PixelImage image,int operation,ConvolutionKernel kernel)throws IOException {
        if(operation<0||operation>2)throw new IllegalArgumentException("Unknown native operation");
        try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeInt(0x50564c31);out.writeInt(image.width());out.writeInt(image.height());out.writeInt(operation);
            int size=kernel==null?1:kernel.size();out.writeInt(size);
            for(int y=0;y<size;y++)for(int x=0;x<size;x++)out.writeDouble(kernel==null?1:kernel.weightAt(x,y));
            for(int pixel:image.copyPixels())out.writeInt(pixel);
        }
    }
    public static Result read(Path file,int width,int height)throws IOException {
        if(Files.size(file)!=36L+4L*width*height)throw new IOException("Malformed CUDA output length");
        try(var input=new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if(input.readInt()!=0x50564f31||input.readInt()!=width||input.readInt()!=height)throw new IOException("CUDA output version/dimensions mismatch");
            double kernel=input.readDouble(),upload=input.readDouble(),download=input.readDouble();
            for(double value:new double[]{kernel,upload,download})if(!Double.isFinite(value)||value<0)throw new IOException("Invalid CUDA timing");
            int[] pixels=new int[Math.multiplyExact(width,height)];for(int i=0;i<pixels.length;i++)pixels[i]=input.readInt();
            return new Result(new PixelImage(width,height,pixels),kernel,upload,download);
        }
    }
}
