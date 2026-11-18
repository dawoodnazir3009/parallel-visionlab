# Verified development environment

Updated November 18, 2026 (Asia/Karachi).

| Component | Observed status |
| --- | --- |
| OS | Windows 11 x64 |
| Java | Temurin JDK 21.0.12.1 |
| Git | 2.56.0.windows.2 |
| Maven | Project-local 3.9.11; dependency cache under .tools/repository |
| JavaFX | Maven-managed 21.0.2, named application module |
| GPU | NVIDIA GeForce RTX 4050 Laptop GPU, 6 GB |
| NVIDIA driver | 610.47; nvidia-smi available |
| CUDA compiler | 13.5.37, official component ZIPs installed under .tools/cuda |
| C++ compiler | MSVC 19.51.36260, Visual Studio Build Tools 2026 |
| CMake / Ninja | Bundled with Visual Studio; build-cuda.ps1 discovers them |
| Native runtime | Static CUDA/MSVC linkage; executable imports KERNEL32.dll |
| Python/pip | Not required |

Verified operations: local CUDA compiler version, native CMake build, device query,
basic/tiled GPU output tests, three CPU engines, portable runtime CPU/GPU self-test,
standalone setup clone/build, and hosted Windows/Linux Java CI.

The initial full CUDA installer attempt stalled; official, SHA-256-verified
component ZIPs provided the working local installation. No NVIDIA driver upgrade
was performed. The version shown by nvidia-smi describes driver support and is not
proof of an installed compiler; the working nvcc and compiled tests provide that.

Original empty ../project/abc.java was preserved. The Maven application lives in
parallel-visionlab. Git author identity is local to the repository. Generated
toolchains, archives, experiments and target binaries are ignored by Git.

Known tool warnings: JavaFX 21.0.2 dependency-model warnings, classpath JavaFX
configuration warning in the opt-in UI tests, and a deprecated jlink compression
option from javafx-maven-plugin 0.0.8. They did not prevent the verified builds.
The application itself launches as a named module; tests intentionally use the
classpath. A genuinely separate Windows machine remains a manual release check.
