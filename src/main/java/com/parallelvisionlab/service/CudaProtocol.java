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


     







}
