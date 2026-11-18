#requires -Version 5.1
[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$ProgressPreference='SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12
$toolsRoot=Join-Path $PSScriptRoot '.tools'
$cudaRoot=Join-Path $toolsRoot 'cuda'
New-Item -ItemType Directory -Force -Path $cudaRoot | Out-Null
# Official component ZIPs avoid a multi-gigabyte installer and do not modify drivers.
$manifest=Invoke-RestMethod 'https://developer.download.nvidia.com/compute/cuda/redist/redistrib_13.5.0.json'
foreach($component in @('cuda_nvcc','cuda_cudart','cuda_crt','libnvvm','cccl')) {
    $package=$manifest.$component.'windows-x86_64'
    if(-not $package){throw "Missing Windows compiler package: $component"}
    $archive=Join-Path $toolsRoot ($component+'.zip')
    if(-not(Test-Path -LiteralPath $archive) -or (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $package.sha256) {
        Write-Host "Downloading official CUDA component: $component"
        Invoke-WebRequest ('https://developer.download.nvidia.com/compute/cuda/redist/'+$package.relative_path) -UseBasicParsing -OutFile $archive
    }
    if((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $package.sha256){throw "CUDA SHA-256 mismatch: $component"}
    $unpack=Join-Path $toolsRoot ('unpack-'+$component)
    Expand-Archive -LiteralPath $archive -DestinationPath $unpack -Force
    $roots=@(Get-ChildItem -LiteralPath $unpack -Directory)
    if($roots.Count -ne 1){throw "Unexpected CUDA package layout: $component"}
    Copy-Item -Path (Join-Path $roots[0].FullName '*') -Destination $cudaRoot -Recurse -Force
}
& (Join-Path $cudaRoot 'bin\nvcc.exe') --version
if($LASTEXITCODE -ne 0){throw 'Local CUDA compiler verification failed'}
Write-Host "CUDA compiler/runtime files installed locally: $cudaRoot"
Write-Host 'The NVIDIA driver and Visual Studio C++ build tools are separate prerequisites.'
