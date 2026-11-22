#requires -Version 5.1
[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
Push-Location $PSScriptRoot
try {
    $maven=Join-Path $PSScriptRoot '.tools\apache-maven-3.9.11\bin\mvn.cmd'
    if(-not(Test-Path -LiteralPath $maven)){throw 'Run setup.ps1 -NoLaunch first.'}
    $jdkHome=$env:JAVA_HOME
    $savedJava=Join-Path $PSScriptRoot '.tools\java-home.txt'
    if(Test-Path -LiteralPath $savedJava){$jdkHome=(Get-Content -LiteralPath $savedJava -Raw).Trim()}
    if(-not $jdkHome){throw 'JDK home missing; run setup.ps1 -NoLaunch first.'}
    $env:JAVA_HOME=$jdkHome
    & $maven '-Dmaven.repo.local=.tools/repository' -B verify javafx:jlink
    if($LASTEXITCODE -ne 0){throw "Packaging failed: $LASTEXITCODE"}
    $runtime=Join-Path $PSScriptRoot 'target\ParallelVisionLab'
    $destination=Join-Path $PSScriptRoot ('target\portable-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
    & (Join-Path $jdkHome 'bin\jpackage.exe') --type app-image --name ParallelVisionLab --dest $destination --runtime-image $runtime --module com.parallelvisionlab/com.parallelvisionlab.VisionLabLauncher --java-options '-XX:MaxRAMPercentage=50' --app-version 0.3.0 --vendor 'Parallel VisionLab Team' --description 'Parallel and Distributed Image Processing Laboratory'
    if($LASTEXITCODE -ne 0){throw 'Native application launcher packaging failed'}
    $application=Join-Path $destination 'ParallelVisionLab'
    $launch=Join-Path $application 'Start-ParallelVisionLab.cmd'
    '@echo off
cd /d "%~dp0"
start "" ParallelVisionLab.exe
' | Set-Content -LiteralPath $launch -Encoding ASCII
    $native=Join-Path $PSScriptRoot 'native\cuda\build\Release\visionlab_cuda.exe'
    if(Test-Path -LiteralPath $native){Copy-Item -LiteralPath $native -Destination $application}
    $notices=Join-Path $application 'THIRD_PARTY'
    New-Item -ItemType Directory -Force -Path $notices | Out-Null
    foreach($component in @('cuda_cudart','cuda_crt','cuda_nvcc','libnvvm','cccl')) {
        $unpack=Join-Path $PSScriptRoot ('.tools\unpack-'+$component)
        if(Test-Path -LiteralPath $unpack){
            $license=Get-ChildItem -LiteralPath $unpack -Recurse -File | Where-Object Name -eq 'LICENSE' | Select-Object -First 1
            if($license){Copy-Item -LiteralPath $license.FullName -Destination (Join-Path $notices ($component+'.LICENSE.txt'))}
        }
    }
    Push-Location $application
    try {
        & (Join-Path $application 'runtime\bin\java.exe') --module com.parallelvisionlab/com.parallelvisionlab.VisionLabLauncher --self-test-cpu
        if($LASTEXITCODE -ne 0){throw 'Packaged CPU self-test failed'}
    }finally{Pop-Location}
    Set-Content -LiteralPath (Join-Path $PSScriptRoot 'target\package-path.txt') -Value $application
    Compress-Archive -LiteralPath $application -DestinationPath (Join-Path $PSScriptRoot 'target\ParallelVisionLab-Windows.zip') -Force
    Write-Host "Packaged application: $application"
    Write-Host 'Distribute the entire ZIP. Recipients do not need a JDK, Maven, Git, or Python.'
}finally{Pop-Location}
