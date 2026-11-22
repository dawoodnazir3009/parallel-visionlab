# Parallel VisionLab

Java 25 / JavaFX desktop laboratory comparing **Sequential Java**, **ExecutorService**,
**Fork/Join**, and custom **CUDA basic/tiled kernels**. The six-page desktop interface
follows the supplied HTML design. Version 0.3.0 is a feature-complete core release
candidate; see the remaining manual release checks below.

## Run on Windows

Download [setup.ps1](https://raw.githubusercontent.com/dawoodnazir3009/parallel-visionlab/main/setup.ps1)
into an empty folder and run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup.ps1
```

This installs missing Git/JDK 25, clones the repository, downloads checksum-verified
Maven, builds/tests and launches the app. Requires Windows x64, PowerShell 5.1+,
internet, and winget if Git/Java is missing. Existing checkouts are preserved,
without an automatic pull/reset. Source copies without a .git directory are also
recognized and built in place. Python/pip is unnecessary; requirements.txt is
intentionally comments only.

For GPU development/build as well:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup.ps1 -InstallNative
```

Native setup reuses installed Visual Studio C++ tools or installs missing Build
Tools, downloads about 100 MB of SHA-256-verified official CUDA 13.5 component
ZIPs into .tools/cuda, builds with CMake, and checks the device. An NVIDIA driver
and CUDA-capable GPU are separate prerequisites. System installers may request
administrator approval/restart; the local CUDA ZIP setup does not replace drivers.

Inside this repository:

```powershell
# Build/test without launching
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup.ps1 -NoLaunch
# Subsequent application launch
& .\.tools\apache-maven-3.9.11\bin\mvn.cmd "-Dmaven.repo.local=.tools/repository" javafx:run
# Full hardware + JavaFX toolkit test suite
& .\.tools\apache-maven-3.9.11\bin\mvn.cmd "-Dmaven.repo.local=.tools/repository" "-Dvisionlab.cudaTests=true" "-Dvisionlab.uiTests=true" verify
```

With system Maven installed, use mvn verify / mvn javafx:run instead.

## Use the application

1. **Image Workspace:** choose PNG/JPEG/BMP/GIF; GIF uses the first frame.
2. Select Grayscale, Gaussian Blur, Sobel Edge Detection or Sharpen Convolution.
3. Choose sequential, multithreading, Fork/Join, or CUDA basic/tiled when built.
   Set CPU workers, Fork/Join leaf rows or Gaussian radius/sigma as applicable.
4. Apply Filter; compare original/output, use fit or 25-200% pixel zoom, and Save PNG.
   Every non-sequential result is checked against sequential before acceptance.
5. **Benchmark Lab:** uses the loaded original and selected filter. Set warmups,
   iterations, CPU workers and Fork/Join threshold; optionally include CUDA. Run
   comparisons or cancel. Every measured output is validated.
6. **Overview / History:** inspect actual measured medians, speedup, CPU efficiency,
   throughput and GPU event timings. History persists across restarts. Export
   measured CSV with image hash, parameters, timing scope and hardware metadata.
7. **Settings:** save benchmark defaults. **Live Monitor:** real CPU/JVM telemetry
   and optional NVIDIA utilization, VRAM, power and temperature via nvidia-smi.

Changing a filter/backend/parameter clears stale output. Background tasks keep
controls responsive; processing/benchmarks can be cancelled. Pools and processes
are closed on exit. Oversized file/header checks run before decoding: 256 MB file
limit, and at most min(64 million, JVM max heap / 96) pixels. The CUDA executable
also accepts up to 64 million pixels and checks free GPU memory before allocation.
Actual memory pressure
can still vary. CPU per-core utilization, CPU power and CPU temperature are
explicitly unavailable rather than simulated.

Windows application launches restrict the Java process to at most sixteen eligible
logical processors using process affinity. CPU processing pools and UI controls
are capped at sixteen workers; Automatic selects up to sixteen. Saved preferences above
sixteen are reduced when loaded, while historical measurements retain their original
worker counts. Settings shows the verified affinity mask. Select ExecutorService
or Fork/Join for parallel CPU processing; Sequential intentionally uses one worker.
The cap does not guarantee full utilization of all sixteen processors.

After a benchmark worker finishes, temporary output references are released and
garbage collection is requested outside the measured work. The loaded source and
workspace previews remain available; use **Clear** in Image Workspace to remove
them. Saved benchmark results contain metadata and measurements, not image buffers.
Java may keep unused heap committed for reuse, so system RAM does not necessarily
return to its pre-run value immediately.

History/settings are local to .tools/experiments and .tools/preferences.properties
under the current working folder. Keep a portable installation in a writable
folder; back up .tools if preserving experiments matters.

## Timing and correctness

CPU: engine wall-clock including allocation, scheduling and synchronization.
GPU: persistent-worker wall-clock including binary pipe exchange, transfers,
kernel work and result decoding. The first request starts a native worker and CUDA
context; later requests reuse that worker, GPU buffers and the same immutable
input. Warmups normally exclude this startup from measured samples. Zero warmups
can include startup in the measured samples. New runs do not use temporary image files.
CUDA events record kernel, upload and download times separately; do not compare
GPU kernel-only time directly against end-to-end CPU time as equivalent scopes.

Warmups are excluded; measured repetitions report median/min/max. Decoding,
output validation, rendering and PNG export are excluded. Backend order is fixed;
background work, JVM state and thermal changes affect timings. Small inputs may
run slower on GPU because pipe exchange, decoding and transfers still have a cost.
New CSV exports identify persistent-pipe timings; older archived file-bridge runs
retain their original scope. Rebuild the native executable with build-cuda.ps1
after updating to the worker protocol. Clearing/replacing the workspace image,
finishing a benchmark, or closing the application terminates owned GPU workers
and releases their host/device buffers. Worker requests retain a 60-second timeout
covering startup and pipe I/O; failures never silently fall back to CPU.
See the [same-session transport measurements](docs/cuda-workflow-performance.md)
for a validated file-bridge versus persistent-worker comparison and reproduction command.

CPU validation is exact. GPU permits at most 1 level of RGB error and requires
alpha to match exactly. Invalid outputs are not recorded as successful runs.
See [filter contracts](docs/filter-contract.md) and
[the measured example/report](docs/project-report.md).

## Portable Windows package

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\package.ps1
```

Distribute target/ParallelVisionLab-Windows.zip, not an individual JAR/EXE.
Extract the entire folder and run ParallelVisionLab.exe (or Start-ParallelVisionLab.cmd).
The ZIP includes a native launcher and Java runtime; recipients need no JDK,
Maven, Git or Python. If CUDA was built, its executable is included; GPU use still
requires compatible NVIDIA hardware/driver. Runtime notices are under runtime/legal
and THIRD_PARTY. Generated binaries and downloaded toolchains are not committed.

Package locations are recorded in target/package-path.txt. No-display verification:

```powershell
# From the extracted application's folder
.\runtime\bin\java.exe --module com.parallelvisionlab/com.parallelvisionlab.VisionLabLauncher --self-test-cpu
# Also exercise installed CUDA
.\runtime\bin\java.exe --module com.parallelvisionlab/com.parallelvisionlab.VisionLabLauncher --self-test
# Reproduce the supplied generated-image benchmark
.\runtime\bin\java.exe --module com.parallelvisionlab/com.parallelvisionlab.VisionLabLauncher --benchmark-demo results.csv
```

## Architecture and collaboration

JavaFX UI → immutable pixels/filter requests → processing engines → validation →
benchmark records → persistent archive / charts / CSV. CUDA is a separate C++/CUDA
executable with a versioned file protocol; there is no JNI.

See [roadmap/status](docs/roadmap.md), [contribution workflow](CONTRIBUTING.md),
[environment](docs/environment.md), [native protocol](docs/native-cuda.md),
[UI handoff](docs/ui-dashboard.md), and [release/demo checks](docs/release-checklist.md).

Java 25 CI builds/tests Windows and Linux. Desktop/GPU tests are opt-in and were
run locally on Windows with an RTX 4050. Remaining manual checks include visual
review of all states, native file-dialog errors, and a genuinely separate Windows
computer; a bundled-runtime self-test is not a clean-VM certification.
