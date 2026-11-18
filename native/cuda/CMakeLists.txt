cmake_minimum_required(VERSION 3.24)
project(visionlab_cuda LANGUAGES CXX CUDA)
add_executable(visionlab_cuda main.cu)
set_target_properties(visionlab_cuda PROPERTIES CXX_STANDARD 17 CUDA_STANDARD 17
    CUDA_ARCHITECTURES "75;86;89" CUDA_RUNTIME_LIBRARY Static
    MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
target_compile_options(visionlab_cuda PRIVATE $<$<COMPILE_LANGUAGE:CUDA>:--fmad=false>)
install(TARGETS visionlab_cuda RUNTIME DESTINATION .)
