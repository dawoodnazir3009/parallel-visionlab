#include <cuda_runtime.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>

using Pixel=uint32_t;
void check(cudaError_t error){if(error!=cudaSuccess)throw std::runtime_error(cudaGetErrorString(error));}
uint32_t read32(std::istream& in){unsigned char b[4];if(!in.read(reinterpret_cast<char*>(b),4))throw std::runtime_error("Truncated input");return(uint32_t(b[0])<<24)|(uint32_t(b[1])<<16)|(uint32_t(b[2])<<8)|b[3];}
void write32(std::ostream& out,uint32_t value){char b[4]={char(value>>24),char(value>>16),char(value>>8),char(value)};out.write(b,4);}
double readDouble(std::istream& in){uint64_t bits=uint64_t(read32(in))<<32;bits|=read32(in);double value;std::memcpy(&value,&bits,8);return value;}
void writeDouble(std::ostream& out,double value){uint64_t bits;std::memcpy(&bits,&value,8);write32(out,uint32_t(bits>>32));write32(out,uint32_t(bits));}
template<class T>struct Device {
    T* data=nullptr;
    explicit Device(size_t count){check(cudaMalloc(reinterpret_cast<void**>(&data),count*sizeof(T)));}
    ~Device(){if(data)cudaFree(data);}
    Device(const Device&)=delete;Device& operator=(const Device&)=delete;
};
struct Event {
    cudaEvent_t value;
    Event(){check(cudaEventCreate(&value));}~Event(){cudaEventDestroy(value);}
};
template<class F>double timed(F action){Event start,stop;check(cudaEventRecord(start.value));action();check(cudaEventRecord(stop.value));check(cudaEventSynchronize(stop.value));float value;check(cudaEventElapsedTime(&value,start.value,stop.value));return value;}
__device__ int clampCoordinate(int value,int extent){return max(0,min(extent-1,value));}
__device__ int channel(double value){return int(fmax(0.0,fmin(255.0,floor(value+0.5))));}
__global__ void grayscale(const Pixel* source,Pixel* output,int count){
    int i=blockIdx.x*blockDim.x+threadIdx.x;if(i>=count)return;
    Pixel pixel=source[i];int gray=(299*((pixel>>16)&255)+587*((pixel>>8)&255)+114*(pixel&255)+500)/1000;
    output[i]=(pixel&0xff000000u)|(gray<<16)|(gray<<8)|gray;
}
__global__ void convolutionBasic(const Pixel* source,Pixel* output,int width,int height,const double* kernel,int size){
    int i=blockIdx.x*blockDim.x+threadIdx.x;if(i>=width*height)return;
    int x=i%width,y=i/width,radius=size/2;double red=0,green=0,blue=0;
    for(int ky=0;ky<size;ky++)for(int kx=0;kx<size;kx++){
        Pixel pixel=source[clampCoordinate(y+ky-radius,height)*width+clampCoordinate(x+kx-radius,width)];
        double weight=kernel[ky*size+kx];red+=((pixel>>16)&255)*weight;green+=((pixel>>8)&255)*weight;blue+=(pixel&255)*weight;
    }
    output[i]=(source[i]&0xff000000u)|(channel(red)<<16)|(channel(green)<<8)|channel(blue);
}
__global__ void convolutionTiled(const Pixel* source,Pixel* output,int width,int height,const double* kernel,int size,int tilesX){
    extern __shared__ Pixel tile[];
    int radius=size/2,side=16+2*radius;
    int tileX=blockIdx.x%tilesX,tileY=blockIdx.x/tilesX;
    int tid=threadIdx.y*16+threadIdx.x;
    for(int index=tid;index<side*side;index+=256){
        int sx=clampCoordinate(tileX*16+index%side-radius,width);
        int sy=clampCoordinate(tileY*16+index/side-radius,height);
        tile[index]=source[sy*width+sx];
    }
    __syncthreads(); // All threads, including those beyond the output edge, load halo pixels.
    int x=tileX*16+threadIdx.x,y=tileY*16+threadIdx.y;if(x>=width||y>=height)return;
    double red=0,green=0,blue=0;
    for(int ky=0;ky<size;ky++)for(int kx=0;kx<size;kx++){
        Pixel pixel=tile[(threadIdx.y+ky)*side+threadIdx.x+kx];double weight=kernel[ky*size+kx];
        red+=((pixel>>16)&255)*weight;green+=((pixel>>8)&255)*weight;blue+=(pixel&255)*weight;
    }
    output[y*width+x]=(source[y*width+x]&0xff000000u)|(channel(red)<<16)|(channel(green)<<8)|channel(blue);
}
__global__ void sobel(const Pixel* gray,const Pixel* source,Pixel* output,int width,int height){
    int i=blockIdx.x*blockDim.x+threadIdx.x;if(i>=width*height)return;
    int x=i%width,y=i/width,gx=0,gy=0;
    const int kx[9]={-1,0,1,-2,0,2,-1,0,1},ky[9]={-1,-2,-1,0,0,0,1,2,1};
    for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
        int value=gray[clampCoordinate(y+dy,height)*width+clampCoordinate(x+dx,width)]&255;
        int index=(dy+1)*3+dx+1;gx+=value*kx[index];gy+=value*ky[index];
    }
    int edge=channel(hypot(double(gx),double(gy)));output[i]=(source[i]&0xff000000u)|(edge<<16)|(edge<<8)|edge;
}
int main(int argc,char** argv){
    try{
        if(argc==2&&std::string(argv[1])=="--device"){
            cudaDeviceProp device;check(cudaGetDeviceProperties(&device,0));std::cout<<device.name<<"\n";return 0;
        }
        if(argc!=4)throw std::runtime_error("Usage: visionlab_cuda input.bin output.bin basic|tiled");
        std::string variant=argv[3];if(variant!="basic"&&variant!="tiled")throw std::runtime_error("Unknown kernel variant");
        std::ifstream input(argv[1],std::ios::binary);if(!input)throw std::runtime_error("Input cannot be opened");
        if(read32(input)!=0x50564c31)throw std::runtime_error("Unsupported protocol");
        uint32_t width=read32(input),height=read32(input),operation=read32(input),size=read32(input);
        uint64_t count64=uint64_t(width)*height;
        if(!width||!height||count64>24000000||operation>2||size<1||size>31||size%2==0)throw std::runtime_error("Invalid dimensions/filter");
        int count=int(count64);std::vector<double> weights(size*size);double magnitude=0;
        for(double& weight:weights){weight=readDouble(input);if(!std::isfinite(weight))throw std::runtime_error("Nonfinite weight");magnitude+=std::abs(weight);}
        if(magnitude>1000000)throw std::runtime_error("Excessive kernel magnitude");
        std::vector<Pixel> pixels(count),result(count);for(Pixel& pixel:pixels)pixel=read32(input);
        if(input.peek()!=std::char_traits<char>::eof())throw std::runtime_error("Unexpected input bytes");
        cudaDeviceProp device;check(cudaGetDeviceProperties(&device,0));check(cudaSetDevice(0));
        Device<Pixel> source(count),output(count),gray(operation==2?count:1);Device<double> kernel(weights.size());
        double upload=timed([&]{check(cudaMemcpy(source.data,pixels.data(),count*sizeof(Pixel),cudaMemcpyHostToDevice));check(cudaMemcpy(kernel.data,weights.data(),weights.size()*sizeof(double),cudaMemcpyHostToDevice));});
        int blocks=(count+255)/256;
        double kernelTime=timed([&]{
            if(operation==0)grayscale<<<blocks,256>>>(source.data,output.data,count);
            else if(operation==2){grayscale<<<blocks,256>>>(source.data,gray.data,count);check(cudaGetLastError());sobel<<<blocks,256>>>(gray.data,source.data,output.data,width,height);}
            else if(variant=="tiled"){
                size_t shared=(16+size-1)*(16+size-1)*sizeof(Pixel);
                if(shared>device.sharedMemPerBlock)throw std::runtime_error("Kernel exceeds shared-memory limit");
                int tilesX=(width+15)/16,tilesY=(height+15)/16;
                convolutionTiled<<<tilesX*tilesY,dim3(16,16),shared>>>(source.data,output.data,width,height,kernel.data,size,tilesX);
            }else convolutionBasic<<<blocks,256>>>(source.data,output.data,width,height,kernel.data,size);
            check(cudaGetLastError());
        });
        double download=timed([&]{check(cudaMemcpy(result.data(),output.data,count*sizeof(Pixel),cudaMemcpyDeviceToHost));});
        std::ofstream out(argv[2],std::ios::binary);if(!out)throw std::runtime_error("Output cannot be opened");
        write32(out,0x50564f31);write32(out,width);write32(out,height);writeDouble(out,kernelTime);writeDouble(out,upload);writeDouble(out,download);
        for(Pixel pixel:result)write32(out,pixel);out.flush();if(!out)throw std::runtime_error("Output write failed");
        return 0;
    }catch(const std::exception& exception){std::cerr<<exception.what()<<"\n";return 2;}
}
