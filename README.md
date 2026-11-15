# Parallel VisionLab

Java 21 / JavaFX desktop project for comparing sequential, ExecutorService,
Fork/Join, and custom CUDA image processing. Phase 1 contains only the application
window. Phase 2 adds asynchronous image loading, a resizable preview, image
dimensions, clear/reset, and friendly error handling. No processing algorithms
or benchmark results are implemented yet.

**Current stage:** Phase 2 implemented; next is sequential grayscale processing.
The expanded plan targets a professional **Parallel VisionLab** release in 4-5 weeks,
with 24 delivery phases.
See the [complete roadmap and current checkpoint](docs/roadmap.md) and the
[three-person contribution workflow](CONTRIBUTING.md).

## Build and run (PowerShell)

### Automatic Windows setup

Download [setup.ps1](https://raw.githubusercontent.com/dawoodnazir3009/parallel-visionlab/main/setup.ps1)
into an empty folder, open PowerShell there, and run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup.ps1
```

The script installs missing Git and JDK 21 through Windows Package Manager,
clones `main` into `parallel-visionlab`, downloads checksum-verified Maven 3.9.11,
resolves JavaFX/JUnit through Maven, runs tests, and opens the application.
When run inside a clone, it uses that checkout without pulling or resetting files.
Repeated runs reuse dependencies. Close the application to finish setup.

Requires Windows x64, PowerShell 5.1+, internet access, and Microsoft App Installer
(winget) if Git or Java is missing. Vendor installers may request administrator
approval; a restart or reopened terminal may be required. This is a source setup,
not an offline installer. No Python installation is needed.

Options:

```powershell
# Build and test without opening the UI
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup.ps1 -NoLaunch
# Include the future native CUDA development toolchain
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup.ps1 -InstallNative
# Choose a clone destination when using a standalone copy of this script
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup.ps1 -Destination C:\Projects\parallel-visionlab
```

`-InstallNative` installs missing Visual Studio 2022 C++ Build Tools, CMake, and
CUDA Toolkit through winget. Expect several GB and possible elevation/reboot.
It does not validate CUDA/compiler compatibility or build kernels, which do not
exist yet. An NVIDIA driver and CUDA-capable GPU are required for future GPU mode;
the current JavaFX application works without them. The script does not replace
an existing GPU driver explicitly, though CUDA's vendor installer can include one.

`requirements.txt` intentionally contains comments only: pip cannot install this
Java/C++ toolchain. Java library versions are defined in `pom.xml`.

Setup uses [Microsoft winget](https://learn.microsoft.com/en-us/windows/package-manager/winget/install)
and [Apache Maven binaries](https://maven.apache.org/download.cgi).

### Manual development commands

With Maven installed, run `mvn clean verify` and then `mvn javafx:run`.
This workspace also has an ignored local Maven installation:

```powershell
cd parallel-visionlab
& .\.tools\apache-maven-3.9.11\bin\mvn.cmd "-Dmaven.repo.local=.tools/repository" clean verify
& .\.tools\apache-maven-3.9.11\bin\mvn.cmd "-Dmaven.repo.local=.tools/repository" javafx:run
```

The first build needs internet access to download plugins, JavaFX, and Windows
native libraries. A separate JavaFX SDK installation is unnecessary. Expect a
window titled **Parallel VisionLab**; close it to end the run command.
The JAR is not a standalone bundled application; use the Maven run command.

## Structure

```text
pom.xml
src/main/java/module-info.java
src/main/java/com/parallelvisionlab/VisionLabApplication.java
docs/environment.md             environment inventory
```

The original empty `../project/abc.java` remains outside this new project.

As later phases need them, add `ui`, `model`, `service`, `engine` (sequential,
executor, forkjoin, cuda), and `benchmark` packages under `com.parallelvisionlab`;
CSS/FXML under `src/main/resources`; JUnit tests under `src/test/java`; and the
CMake executable project under `native/cuda`. Keep GUI code separate from
processing and benchmarking. CUDA will use a process bridge, without JNI.

## Validation plan

Run `mvn verify` for image decoder tests (known pixel, corrupt input, missing file,
and closed file handles). For a manual UI check:

1. Launch the application and click **Open image**.
2. Select a PNG/JPEG/BMP/GIF; confirm the filename, dimensions, and preview.
3. Resize the window; confirm the image keeps its proportions.
4. Cancel the picker; confirm the current image stays visible.
5. Choose a corrupt file using **All files**; confirm an error appears and the
   previous image remains. Then load another valid image.
6. Click **Clear** to return to the empty workspace.

Image decoding runs on a daemon worker; controls prevent overlapping loads.
GIF files are accepted, but animation/frame processing is outside this phase.
Images are decoded at full resolution; very large files may exhaust available
memory. Preview scaling does not change the stored image dimensions.

Phase 1: compile/package the modular application and launch the real JavaFX window.
Algorithm phases: add JUnit tests with small known images, boundary cases, and
cross-backend output comparisons before adding benchmark measurements. CUDA
compiler/host-compiler compatibility must be checked before native development.

Configuration follows the [OpenJFX Maven guide](https://openjfx.io/openjfx-docs/#maven).
