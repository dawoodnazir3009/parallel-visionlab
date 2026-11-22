#requires -Version 5.1
<#
.SYNOPSIS
Bootstraps Parallel VisionLab on Windows x64, verifies it, and launches JavaFX.
.DESCRIPTION
Run from a local source folder or download this file alone. Local source does not
require Git. Existing repositories are never reset or automatically pulled.
-InstallNative installs native prerequisites and builds the CUDA engine. Existing
CUDA builds are refreshed when stale and checked for persistent worker protocol 2.
Missing C++ tools may request elevation.
#>
[CmdletBinding()]
param(
    [string]$Destination = '',
    [switch]$InstallNative,
    [switch]$NoLaunch
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$repositoryUrl = 'https://github.com/dawoodnazir3009/parallel-visionlab.git'
if (-not $Destination) { $Destination = Join-Path $PSScriptRoot 'parallel-visionlab' }

function Invoke-Checked {
    param([string]$Executable, [string[]]$Arguments)
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Executable failed with exit code $LASTEXITCODE" }
}

function Refresh-ToolPath {
    $env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' +
        [Environment]::GetEnvironmentVariable('Path', 'User') + ';' + $env:Path
}

function Install-WingetPackage {
    param([string]$Id, [string[]]$ExtraArguments = @())
    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
        throw 'Install Microsoft App Installer (winget), reopen PowerShell, and rerun setup.'
    }
    Invoke-Checked 'winget' (@('install', '--id', $Id, '--exact', '--source', 'winget',
        '--accept-source-agreements', '--accept-package-agreements', '--disable-interactivity') + $ExtraArguments)
    Refresh-ToolPath
}

function Get-Java25Home {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $javaCommand = Get-Command javac -ErrorAction SilentlyContinue
    if ($javaCommand) { $candidates += Split-Path (Split-Path $javaCommand.Source) }
    foreach ($base in @("$env:ProgramFiles\Eclipse Adoptium", "$env:LOCALAPPDATA\Programs\Eclipse Adoptium")) {
        if (Test-Path -LiteralPath $base) {
            $candidates += @(Get-ChildItem -LiteralPath $base -Directory -Filter 'jdk-25*' | Select-Object -ExpandProperty FullName)
        }
    }
    foreach ($candidate in $candidates) {
        $releaseFile = Join-Path $candidate 'release'
        if ((Test-Path -LiteralPath $releaseFile) -and
            ((Get-Content -LiteralPath $releaseFile -Raw) -match 'JAVA_VERSION="25[.\"]') -and
            (Test-Path -LiteralPath (Join-Path $candidate 'bin\javac.exe'))) { return $candidate }
    }
    return $null
}

function Test-ProjectRoot {
    param([string]$Path)
    return (Test-Path -LiteralPath (Join-Path $Path 'pom.xml')) -and
        (Test-Path -LiteralPath (Join-Path $Path 'src\main\java\com\parallelvisionlab\VisionLabLauncher.java'))
}

function Test-CudaWorker {
    param([string]$Executable)
    $process=$null
    $errors=$null
    $script:cudaCheckError=''
    try {
        $start=[Diagnostics.ProcessStartInfo]::new()
        $start.FileName=$Executable
        $start.Arguments='--worker basic'
        $start.UseShellExecute=$false
        $start.CreateNoWindow=$true
        $start.RedirectStandardInput=$true
        $start.RedirectStandardOutput=$true
        $start.RedirectStandardError=$true
        $process=[Diagnostics.Process]::Start($start)
        $errors=$process.StandardError.ReadToEndAsync()
        $bytes=New-Object byte[] 4
        $offset=0
        $deadline=[DateTime]::UtcNow.AddSeconds(15)
        while($offset -lt 4){
            $remaining=[int][Math]::Max(1,($deadline-[DateTime]::UtcNow).TotalMilliseconds)
            $read=$process.StandardOutput.BaseStream.ReadAsync($bytes,$offset,4-$offset)
            if(-not $read.Wait($remaining)){throw 'CUDA worker startup timed out.'}
            if($read.Result -eq 0){throw 'CUDA worker exited before its ready handshake.'}
            $offset+=$read.Result
        }
        if([BitConverter]::ToString($bytes) -ne '50-56-57-32'){throw 'CUDA worker protocol version mismatch.'}
        $process.StandardInput.Close()
        if(-not $process.WaitForExit(5000)){throw 'CUDA worker did not shut down after verification.'}
        if($process.ExitCode -ne 0){throw "CUDA worker exited with code $($process.ExitCode)."}
        return $true
    } catch {
        $script:cudaCheckError=$_.Exception.Message
        if($null -ne $process){
            if(-not $process.HasExited){$process.Kill();[void]$process.WaitForExit(5000)}
            if($null -ne $errors -and $errors.Wait(1000)){
                $detail=$errors.Result.Trim()
                if($detail.Length -gt 1000){$detail=$detail.Substring(0,1000)}
                if($detail){$script:cudaCheckError+=' '+$detail}
            }
        }
        return $false
    } finally {
        if($null -ne $process){
            if(-not $process.HasExited){$process.Kill();[void]$process.WaitForExit(5000)}
            $process.Dispose()
        }
    }
}

try {
    if ($env:OS -ne 'Windows_NT' -or -not [Environment]::Is64BitOperatingSystem -or
        $env:PROCESSOR_ARCHITECTURE -eq 'ARM64') { throw 'This setup supports Windows x64.' }

    if (Test-ProjectRoot $PSScriptRoot) {
        $projectRoot = $PSScriptRoot
    } else {
        $projectRoot = [IO.Path]::GetFullPath($Destination)
        if (Test-Path -LiteralPath $projectRoot) {
            if (-not (Test-ProjectRoot $projectRoot)) { throw "Destination exists but does not contain Parallel VisionLab source: $projectRoot" }
        } else {
            if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Install-WingetPackage 'Git.Git' }
            if (-not (Get-Command git -ErrorAction SilentlyContinue)) { throw 'Git was installed; reopen PowerShell and rerun setup.' }
            Invoke-Checked 'git' @('clone', '--branch', 'main', $repositoryUrl, $projectRoot)
        }
    }
    if (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'pom.xml'))) { throw 'Missing project pom.xml.' }
    Write-Host "Using project source: $projectRoot"

    $jdkHome = Get-Java25Home
    if (-not $jdkHome) {
        Install-WingetPackage 'EclipseAdoptium.Temurin.25.JDK'
        $jdkHome = Get-Java25Home
    }
    if (-not $jdkHome) { throw 'JDK 25 not found after installation. Reopen PowerShell and rerun setup.' }
    $env:JAVA_HOME = $jdkHome
    $env:Path = (Join-Path $jdkHome 'bin') + ';' + $env:Path

    $toolsPath = Join-Path $projectRoot '.tools'
    New-Item -ItemType Directory -Force -Path $toolsPath | Out-Null
    Set-Content -LiteralPath (Join-Path $toolsPath 'java-home.txt') -Value $jdkHome -Encoding UTF8
    $mavenPath = Join-Path $toolsPath 'apache-maven-3.9.11\bin\mvn.cmd'
    if (-not (Test-Path -LiteralPath $mavenPath)) {
        $mavenUrl = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip'
        $archive = Join-Path $toolsPath 'maven.zip'
        Invoke-WebRequest $mavenUrl -UseBasicParsing -OutFile $archive
        $checksum = (Invoke-WebRequest "$mavenUrl.sha512" -UseBasicParsing).Content.Trim().Split(' ')[0]
        if ((Get-FileHash -LiteralPath $archive -Algorithm SHA512).Hash -ne $checksum) { throw 'Maven checksum mismatch.' }
        Expand-Archive -LiteralPath $archive -DestinationPath $toolsPath -Force
    }

    if ($InstallNative) {
        $vswhere = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
        $msvc = $null
        if (Test-Path -LiteralPath $vswhere) {
            $msvc = & $vswhere -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
        }
        if (-not $msvc) {
            Install-WingetPackage 'Microsoft.VisualStudio.2022.BuildTools' @('--override', '--wait --passive --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended --norestart')
        }
        Invoke-Checked 'powershell' @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $projectRoot 'install-cuda.ps1'))
        Invoke-Checked 'powershell' @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $projectRoot 'build-cuda.ps1'))
    }

    $native=Join-Path $projectRoot 'native\cuda\build\Release\visionlab_cuda.exe'
    $cudaReady=$false
    if(Test-Path -LiteralPath $native){
        $inputs=@(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'native\cuda') -File |
            Where-Object {$_.Extension -in @('.cu','.cuh','.cpp','.h','.hpp') -or $_.Name -eq 'CMakeLists.txt'})
        $inputs+=Get-Item -LiteralPath (Join-Path $projectRoot 'build-cuda.ps1')
        $built=(Get-Item -LiteralPath $native).LastWriteTimeUtc
        $stale=@($inputs | Where-Object {$_.LastWriteTimeUtc -gt $built}).Count -gt 0
        if($stale){
            Write-Host 'CUDA source is newer than its executable; rebuilding with installed tools.'
            Invoke-Checked 'powershell' @('-NoProfile','-ExecutionPolicy','Bypass','-File',(Join-Path $projectRoot 'build-cuda.ps1'))
        }
        $cudaReady=Test-CudaWorker $native
        if(-not $cudaReady -and -not $InstallNative -and -not $stale){
            Write-Warning "CUDA worker check failed: $script:cudaCheckError Rebuilding the native executable."
            Invoke-Checked 'powershell' @('-NoProfile','-ExecutionPolicy','Bypass','-File',(Join-Path $projectRoot 'build-cuda.ps1'))
            $cudaReady=Test-CudaWorker $native
        }
        if(-not $cudaReady){throw "CUDA verification failed: $script:cudaCheckError Check the NVIDIA driver; use -InstallNative if build tools are missing."}
        Write-Host 'CUDA persistent worker protocol 2 verified. GPU correctness tests are enabled.'
    } else {
        Write-Host 'CUDA executable is not built. CPU backends are available; use -InstallNative to enable GPU processing.'
    }

    Push-Location $projectRoot
    try {
        $verify=@('-Dmaven.repo.local=.tools/repository','-B','verify')
        if($cudaReady){$verify+='-Dvisionlab.cudaTests=true'}
        Invoke-Checked $mavenPath $verify
        Write-Host "Setup and tests passed: $projectRoot"
        if (-not $NoLaunch) { Invoke-Checked $mavenPath @('-Dmaven.repo.local=.tools/repository', 'javafx:run') }
    } finally { Pop-Location }
} catch {
    Write-Error "Setup failed: $($_.Exception.Message)" -ErrorAction Continue
    exit 1
}
