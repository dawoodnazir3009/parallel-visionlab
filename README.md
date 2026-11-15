# Parallel VisionLab

Java 21 / JavaFX desktop project for comparing sequential, ExecutorService,
Fork/Join, and custom CUDA image processing. Phase 1 contains only the application
window. No processing algorithms or benchmark results are implemented yet.

## Build and run (PowerShell)

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

Phase 1: compile/package the modular application and launch the real JavaFX window.
Algorithm phases: add JUnit tests with small known images, boundary cases, and
cross-backend output comparisons before adding benchmark measurements. CUDA
compiler/host-compiler compatibility must be checked before native development.

Configuration follows the [OpenJFX Maven guide](https://openjfx.io/openjfx-docs/#maven).
