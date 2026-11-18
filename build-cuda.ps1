#requires -Version 5.1
[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$vswhere="${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
if(-not(Test-Path -LiteralPath $vswhere)){throw 'Install Visual Studio C++ build tools first.'}
$visualStudio=& $vswhere -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if(-not $visualStudio){throw 'No Visual Studio C++ toolchain found.'}
& (Join-Path $visualStudio 'Common7\Tools\Launch-VsDevShell.ps1') -Arch amd64 -HostArch amd64 -SkipAutomaticLocation
$cmake=Get-Command cmake -ErrorAction SilentlyContinue
$cmakePath=if($cmake){$cmake.Source}else{Join-Path $visualStudio 'Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe'}
$ninja=Join-Path $visualStudio 'Common7\IDE\CommonExtensions\Microsoft\CMake\Ninja'
$env:Path=$ninja+';'+$env:Path
$localCuda=Join-Path $PSScriptRoot '.tools\cuda'
$compiler=Join-Path $localCuda 'bin\nvcc.exe'
if(-not(Test-Path -LiteralPath $compiler)){
    $nvcc=Get-Command nvcc -ErrorAction SilentlyContinue
    if(-not $nvcc){throw 'Run install-cuda.ps1 or install NVIDIA CUDA Toolkit first.'}
    $compiler=$nvcc.Source
}
$source=Join-Path $PSScriptRoot 'native\cuda'
$build=Join-Path $source 'build\Release'
& $cmakePath -S $source -B $build -G Ninja '-DCMAKE_BUILD_TYPE=Release' "-DCMAKE_CUDA_COMPILER=$compiler"
if($LASTEXITCODE -ne 0){throw 'CUDA CMake configuration failed'}
& $cmakePath --build $build
if($LASTEXITCODE -ne 0){throw 'CUDA compilation failed'}
& (Join-Path $build 'visionlab_cuda.exe') --device
if($LASTEXITCODE -ne 0){throw 'CUDA device smoke check failed'}
