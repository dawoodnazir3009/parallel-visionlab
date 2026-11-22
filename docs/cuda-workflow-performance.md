# Persistent CUDA workflow measurements

Measured on 22 November 2026 on the development Windows computer with an NVIDIA
RTX 4050 Laptop GPU and Temurin JDK 25.0.5.1. The comparison uses the same rebuilt
native executable for the legacy file CLI and persistent pipe modes.

## Workload and scope

The input is a deterministic 512 by 512 ARGB gradient. Both variants apply a
Gaussian kernel with radius 2 and sigma 1.2. Each transport has two warmups and
five measured repetitions. Every output is compared with the sequential CPU
reference, using RGB tolerance 1 and exact alpha. Validation is outside timing.

| CUDA variant | File process median ms | Persistent pipe median ms | File time divided by pipe time |
| --- | ---: | ---: | ---: |
| Basic | 151.467 | 2.408 | 62.90 |
| Tiled | 145.005 | 2.807 | 51.66 |

Raw observations are recorded in [cuda-workflow-comparison.csv](cuda-workflow-comparison.csv).

The legacy measurement includes request-file writing, process and CUDA-context
startup, allocation, transfers, computation, output-file reading and pixel
decoding. It starts a fresh process for every repetition. The persistent
measurement includes request/response pipe exchange, computation, result transfer
and Java pixel decoding, with an already initialized worker and cached immutable
input. Startup and the first image upload occur during warmups in this case.

These ratios describe request wall time for this specific cached-input workload;
they are not GPU-versus-CPU speedups or a universal performance guarantee. New
images still require an upload. Returning a full-resolution result to Java still
has a cost, particularly for large images. Both modes use the same filter kernels.
This short experiment does not provide confidence intervals or control all
thermal, driver and background-workload variation.

## Reproduce

Rebuild the native executable with build-cuda.ps1, then run:

```powershell
mvn -Dvisionlab.cudaWorkflowBenchmark=true -Dtest=CudaWorkflowBenchmarkTest test
```

The test writes a new target/cuda-workflow-comparison.csv. It checks correctness
but intentionally does not assert that one transport must be faster.

The complete verified suite contained 83 passing tests with no skips, including
protocol framing, timeout, cancellation, close during exchange, worker restart,
CPU reference comparisons, JavaFX navigation and actual 8064 by 6048 photo tests
for all four filters with both CUDA variants.
