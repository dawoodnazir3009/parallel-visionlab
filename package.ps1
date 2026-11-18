#requires -Version 5.1
[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
Push-Location $PSScriptRoot
try {
    $maven=Join-Path $PSScriptRoot '.tools\apache-maven-3.9.11\bin\mvn.cmd'
    if(-not(Test-Path -LiteralPath $maven)){throw 'Run setup.ps1 -NoLaunch first.'}
    & $maven '-Dmaven.repo.local=.tools/repository' -B verify javafx:jlink
    if($LASTEXITCODE -ne 0){throw "Packaging failed: $LASTEXITCODE"}
    $runtime=Join-Path $PSScriptRoot 'target\ParallelVisionLab'
    $launch=Join-Path $runtime 'Start-ParallelVisionLab.cmd'
    '@echo off
cd /d "%~dp0"
call bin\ParallelVisionLab.bat
' | Set-Content -LiteralPath $launch -Encoding ASCII
    $native=Join-Path $PSScriptRoot 'native\cuda\build\Release\visionlab_cuda.exe'
    if(Test-Path -LiteralPath $native){Copy-Item -LiteralPath $native -Destination $runtime}
    Compress-Archive -LiteralPath $runtime -DestinationPath (Join-Path $PSScriptRoot 'target\ParallelVisionLab-Windows.zip') -Force
    Write-Host "Packaged runtime: $runtime"
    Write-Host 'Distribute the entire ZIP. Recipients do not need a JDK, Maven, Git, or Python.'
}finally{Pop-Location}
