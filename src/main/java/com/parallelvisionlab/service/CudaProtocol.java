package com.parallelvisionlab.service;
import com.parallelvisionlab.model.*;
import java.io.*;
import java.nio.file.*;
import java.nio.ByteBuffer;



public class CudaProtocol 
{
    public static final int WORKER_READY = 0x50565732;
    public static final int WORKER_REQUEST = 0x50564c32;
    public static final int WORKER_RESPONSE = 0x50564f32;

    public record Result(PixelImage pixels,double kernelMs,double uploadMs,double downloadMs) 
    {

     }
    private CudaProtocol() 
    {

     }
    
     public static void writeRequest(DataOutputStream out, PixelImage image, int operation, ConvolutionKernel kernel, boolean reuseInput) throws IOException 
     {
        if(operation<0||operation>2)throw new IllegalArgumentException("Unknown native operation");
        out.writeInt(WORKER_REQUEST);out.writeInt(image.width());out.writeInt(image.height());out.writeInt(operation);
        int size=kernel==null?1:kernel.size();
        out.writeInt(size);
        out.writeInt(reuseInput?1:0);
        for(int y=0;y<size;y++)
            for(int x=0;x<size;x++)
                out.writeDouble(kernel==null?1:kernel.weightAt(x,y));

        if(!reuseInput)
        {
            ByteBuffer bytes=ByteBuffer.allocate(64*1024);
            for(int y=0;y<image.height();y++)
                for(int x=0;x<image.width();x++)
                {
                if(!bytes.hasRemaining())
                    {
                        out.write(bytes.array());bytes.clear();
                    }
                bytes.putInt(image.argbAt(x,y));
                if(bytes.position()>0)
                    out.write(bytes.array(),0,bytes.position());
        }
        out.flush();
    }


     public static Result readResponse(DataInputStream input,int width,int height)throws IOException 
     {
        if(input.readInt()!=WORKER_RESPONSE||input.readInt()!=width||input.readInt()!=height)

            throw new IOException("CUDA worker response version/dimensions mismatch");

        double kernel=input.readDouble(),upload=input.readDouble(),download=input.readDouble();
        for(double value:new double[]{kernel,upload,download})
            if(!Double.isFinite(value)||value<0)
            throw new IOException("Invalid CUDA worker timing");
        
        int[] pixels=new int[Math.multiplyExact(width,height)];
        
        byte[] chunk=new byte[64*1024];
        
        for(int offset=0;offset<pixels.length;)
        {
            int count=Math.min(chunk.length/4,pixels.length-offset);
            input.readFully(chunk,0,count*4);
            ByteBuffer bytes=ByteBuffer.wrap(chunk,0,count*4);
            for(int i=0;i<count;i++)
                pixels[offset++]=bytes.getInt();
        }
        return new Result(new PixelImage(width,height,pixels),kernel,upload,download);
    }

    public static void write(Path file,PixelImage image,int operation,ConvolutionKernel kernel)throws IOException 
    {
        if(operation<0||operation>2)
            throw new IllegalArgumentException("Unknown native operation");

        try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) 
        {
            out.writeInt(0x50564c31);
            out.writeInt(image.width());
            out.writeInt(image.height());
            out.writeInt(operation);






