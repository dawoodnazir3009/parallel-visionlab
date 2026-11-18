#requires -Version 5.1
<#
.SYNOPSIS
Bootstraps Parallel VisionLab on Windows x64, verifies it, and launches JavaFX.
.DESCRIPTION
Run from a clone or download this file alone. Existing repositories are never
reset or automatically pulled. -InstallNative adds verified local CUDA compiler
components and builds the native engine. Missing C++ tools may request elevation.
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

function Get-Java21Home {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $javaCommand = Get-Command javac -ErrorAction SilentlyContinue
    if ($javaCommand) { $candidates += Split-Path (Split-Path $javaCommand.Source) }
    foreach ($base in @("$env:ProgramFiles\Eclipse Adoptium", "$env:LOCALAPPDATA\Programs\Eclipse Adoptium")) {
        if (Test-Path -LiteralPath $base) {
            $candidates += @(Get-ChildItem -LiteralPath $base -Directory -Filter 'jdk-21*' | Select-Object -ExpandProperty FullName)
        }
    }
    foreach ($candidate in $candidates) {
        $releaseFile = Join-Path $candidate 'release'
        if ((Test-Path -LiteralPath $releaseFile) -and
            ((Get-Content -LiteralPath $releaseFile -Raw) -match 'JAVA_VERSION="21[.\"]') -and
            (Test-Path -LiteralPath (Join-Path $candidate 'bin\javac.exe'))) { return $candidate }
    }
    return $null
}

try {
    if ($env:OS -ne 'Windows_NT' -or -not [Environment]::Is64BitOperatingSystem -or
        $env:PROCESSOR_ARCHITECTURE -eq 'ARM64') { throw 'This setup supports Windows x64.' }

    if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Install-WingetPackage 'Git.Git' }
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) { throw 'Git was installed; reopen PowerShell and rerun setup.' }

    if ((Test-Path -LiteralPath (Join-Path $PSScriptRoot 'pom.xml')) -and
        (Test-Path -LiteralPath (Join-Path $PSScriptRoot '.git'))) {
        $projectRoot = $PSScriptRoot
    } else {
        $projectRoot = [IO.Path]::GetFullPath($Destination)
        if (Test-Path -LiteralPath $projectRoot) {
            if (-not (Test-Path -LiteralPath (Join-Path $projectRoot '.git'))) {
                throw "Destination already exists and is not a clone: $projectRoot"
            }
            $remote = & git -C $projectRoot remote get-url origin
            if ($LASTEXITCODE -ne 0 -or $remote.TrimEnd('/') -notin @($repositoryUrl, $repositoryUrl.Replace('.git', ''))) {
                throw 'Existing destination does not have the expected GitHub origin.'
            }
        } else { Invoke-Checked 'git' @('clone', '--branch', 'main', $repositoryUrl, $projectRoot) }
    }
    if (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'pom.xml'))) { throw 'Missing project pom.xml.' }

    $jdkHome = Get-Java21Home
    if (-not $jdkHome) {
        Install-WingetPackage 'EclipseAdoptium.Temurin.21.JDK'
        $jdkHome = Get-Java21Home
    }
    if (-not $jdkHome) { throw 'JDK 21 not found after installation. Reopen PowerShell and rerun setup.' }
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

    Push-Location $projectRoot
    try {
        Invoke-Checked $mavenPath @('-Dmaven.repo.local=.tools/repository', '-B', 'verify')
        Write-Host "Setup and tests passed: $projectRoot"
        if (-not $NoLaunch) { Invoke-Checked $mavenPath @('-Dmaven.repo.local=.tools/repository', 'javafx:run') }
    } finally { Pop-Location }
} catch {
    Write-Error "Setup failed: $($_.Exception.Message)" -ErrorAction Continue
    exit 1
}
