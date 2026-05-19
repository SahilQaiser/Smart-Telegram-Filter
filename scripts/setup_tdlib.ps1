#Requires -Version 5.1
<#
.SYNOPSIS
    Build TDLib for Android using Docker and install artifacts into the project.
.DESCRIPTION
    Outputs:
        app\libs\tdlib.jar                          - Java bindings JAR
        app\src\main\jniLibs\<ABI>\libtdjni.so     - Native libs
    Prerequisites: Docker Desktop (running), git, Java SDK (javac + jar in PATH)
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoUrl    = "https://github.com/tdlib/td.git"
$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir    = Split-Path -Parent $ScriptDir
$WorkDir    = Join-Path $RootDir ".tdlib_build"
$OutDir     = Join-Path $WorkDir "output"
$ClassesDir = Join-Path $WorkDir "classes"
$Abis       = @("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

function Write-Green($msg) { Write-Host $msg -ForegroundColor Green }
function Write-Blue($msg)  { Write-Host $msg -ForegroundColor Cyan  }
function Write-Red($msg)   { Write-Host $msg -ForegroundColor Red   }

# Resolve javac/jar — fall back to Android Studio's bundled JBR when not on PATH
$script:JavacExe = "javac"
$script:JarExe   = "jar"

function Resolve-JavaTools {
    if (Get-Command javac -ErrorAction SilentlyContinue) { return }

    $candidates = @(
        "$env:LOCALAPPDATA\Programs\Android Studio\jbr\bin",
        "C:\Program Files\Android\Android Studio\jbr\bin",
        "C:\Program Files\Android Studio\jbr\bin"
    )
    $jbrBin = $candidates | Where-Object { Test-Path "$_\javac.exe" } | Select-Object -First 1
    if ($jbrBin) {
        Write-Blue "Using Android Studio JBR: $jbrBin"
        $script:JavacExe = Join-Path $jbrBin "javac.exe"
        $script:JarExe   = Join-Path $jbrBin "jar.exe"
    } else {
        throw "javac not found on PATH and no Android Studio JBR detected. Add a JDK to PATH and re-run."
    }
}

function Test-Prerequisites {
    $missing = $false
    foreach ($cmd in @("docker", "git")) {
        if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
            Write-Red "Missing prerequisite: $cmd"
            $missing = $true
        }
    }
    if ($missing) { throw "Install the missing tools and re-run." }

    docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker daemon is not running. Start Docker Desktop and re-run."
    }

    Resolve-JavaTools
}

function Invoke-CloneOrUpdate {
    $tdDir = Join-Path $WorkDir "td"
    if (Test-Path (Join-Path $tdDir ".git")) {
        Write-Blue "Updating existing td clone..."
        git -C $tdDir fetch --depth=1 origin master
        git -C $tdDir checkout FETCH_HEAD
    } else {
        Write-Blue "Sparse-cloning tdlib/td (example/android only)..."
        New-Item -ItemType Directory -Path $tdDir -Force | Out-Null
        git -C $tdDir init
        git -C $tdDir remote add origin $RepoUrl
        git -C $tdDir config core.sparseCheckout true
        Set-Content -Path (Join-Path $tdDir ".git\info\sparse-checkout") -Value "example/android/"
        git -C $tdDir fetch --depth=1 origin master
        git -C $tdDir checkout FETCH_HEAD
    }
}

function Invoke-DockerBuild {
    $androidDir = Join-Path $WorkDir "td\example\android"
    New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
    Write-Blue "Running Docker build (20-40 min on first run)..."
    $dockerfilePath = Join-Path $ScriptDir "Dockerfile.tdlib"
    docker build -f $dockerfilePath --output $OutDir $androidDir
    if ($LASTEXITCODE -ne 0) { throw "Docker build failed with exit code $LASTEXITCODE" }
}

function Expand-Artifacts {
    $zip = Join-Path $OutDir "tdlib.zip"
    if (-not (Test-Path $zip)) { throw "Expected $zip was not produced by Docker build." }
    Write-Blue "Extracting tdlib.zip..."
    $extractDir = Join-Path $WorkDir "extracted"
    if (Test-Path $extractDir) { Remove-Item $extractDir -Recurse -Force }
    New-Item -ItemType Directory -Path $extractDir -Force | Out-Null
    Expand-Archive -Path $zip -DestinationPath $extractDir
    return $extractDir
}

function Install-Natives($extractDir) {
    Write-Blue "Installing native libraries..."
    foreach ($abi in $Abis) {
        $src    = Join-Path $extractDir "tdlib\libs\$abi\libtdjni.so"
        $dstDir = Join-Path $RootDir "app\src\main\jniLibs\$abi"
        if (Test-Path $src) {
            New-Item -ItemType Directory -Path $dstDir -Force | Out-Null
            Copy-Item $src -Destination (Join-Path $dstDir "libtdjni.so") -Force
            Write-Green "  Installed $abi/libtdjni.so"
        } else {
            Write-Host "  Skipping $abi (not present in build output)"
        }
    }
}

function Build-Jar($extractDir) {
    $javaSrc = Join-Path $extractDir "tdlib\java"
    if (-not (Test-Path $javaSrc)) { throw "Java source directory not found at $javaSrc" }

    # TdApi.java uses @IntDef from androidx.annotation — needed for compilation only
    $annotationJar = Join-Path $WorkDir "annotation-1.4.0.jar"
    if (-not (Test-Path $annotationJar)) {
        Write-Blue "Downloading androidx.annotation jar..."
        Invoke-WebRequest -Uri "https://maven.google.com/androidx/annotation/annotation/1.4.0/annotation-1.4.0.jar" `
                          -OutFile $annotationJar -UseBasicParsing
    }

    Write-Blue "Compiling TDLib Java sources..."
    if (Test-Path $ClassesDir) { Remove-Item $ClassesDir -Recurse -Force }
    New-Item -ItemType Directory -Path $ClassesDir -Force | Out-Null

    $javaFiles = Get-ChildItem -Path $javaSrc -Filter "*.java" -Recurse |
                 Select-Object -ExpandProperty FullName
    if ($javaFiles.Count -eq 0) { throw "No .java files found under $javaSrc" }

    $rspFile = Join-Path $WorkDir "sources.rsp"
    $javaFiles | Set-Content $rspFile
    & $script:JavacExe --release 8 -cp $annotationJar -d $ClassesDir "@$rspFile"
    if ($LASTEXITCODE -ne 0) { throw "javac failed" }

    $jarDir = Join-Path $RootDir "app\libs"
    New-Item -ItemType Directory -Path $jarDir -Force | Out-Null
    $jarPath = Join-Path $jarDir "tdlib.jar"
    & $script:JarExe cf $jarPath -C $ClassesDir .
    if ($LASTEXITCODE -ne 0) { throw "jar packaging failed" }
    Write-Green "  Created app\libs\tdlib.jar ($($javaFiles.Count) source files)"
}

# ── Main ──────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Blue "=== SmartTelegramFilter - TDLib setup ==="
Write-Host ""

Test-Prerequisites
New-Item -ItemType Directory -Path $WorkDir -Force | Out-Null
Invoke-CloneOrUpdate
Invoke-DockerBuild
$extractDir = Expand-Artifacts
Install-Natives $extractDir
Build-Jar $extractDir

Write-Host ""
Write-Green "=== Done! TDLib artifacts installed successfully ==="
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Add TELEGRAM_API_ID and TELEGRAM_API_HASH to local.properties"
Write-Host "  2. Open the project in Android Studio and build"
Write-Host ""
