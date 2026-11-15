# Environment inventory

Verified on Windows, November 15, 2026.

| Component | Observed status |
| --- | --- |
| JDK / javac | Temurin 21.0.12.1; JAVA_HOME configured |
| Git | 2.56.0.windows.2 |
| NVIDIA GPU | GeForce RTX 4050 Laptop GPU, 6 GB |
| NVIDIA driver | 610.47; nvidia-smi works |
| CUDA Toolkit / nvcc | Not found on PATH or in standard toolkit directory |
| Maven | Not on PATH; project-local 3.9.11 bootstrap used |
| JavaFX | Maven-managed 21.0.2; no standalone SDK required |
| MSVC | Visual Studio Build Tools 2026; MSVC 14.51.36231 found |
| CMake | 4.3.1-msvc1; bundled with Visual Studio, not on PATH |

The CUDA version shown by nvidia-smi describes driver support, not an installed
CUDA compiler. Install a CUDA Toolkit with a supported MSVC host compiler before
the CUDA phase; the installed Visual Studio version is not yet validated for CUDA.
Use a Visual Studio developer shell for native tools. The MSVC executable reports
compiler version 19.51.36260 for x64 when invoked directly; a complete native
compile/link test is deferred to the CUDA phase.

The workspace initially had no Git repository and only an empty `project/abc.java`.
No original files were replaced. Git author name and email were not configured at
inspection time. The user subsequently supplied an identity, configured locally
for this repository before the Phase 1 milestone commit.

The Maven project lives in `parallel-visionlab/`, as requested during setup.
An initial `clean verify` and real JavaFX window launch passed before relocation.
Maven reports JavaFX dependency-model warnings for the pinned 21.0.2 artifacts;
these did not prevent compilation or the verified desktop launch.
