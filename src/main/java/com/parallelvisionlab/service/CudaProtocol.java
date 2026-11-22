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
        



     }










}
