# Parallel VisionLab roadmap

## Current checkpoint

As of November 15, 2026, Phases 1 and 2 are implemented. The latest application
commit is `1aaad23`, published on GitHub `main`. Maven verification passed with
three decoder tests, and the Phase 2 window launched and responded. The manual
picker/resize/error checklist in README still needs a full user walkthrough.
There are no processing engines, CUDA kernels, benchmarks, or result charts yet.
The next implementation milestone is Phase 3: sequential grayscale.

## Delivery phases

| Phase | Status | Deliverables | Completion check |
| --- | --- | --- | --- |
| 1. Foundation | Done | Java 21 modular Maven app, JavaFX window, environment inventory, Git repository | Build passes and real window opens |
| 2. Image workspace | Implemented | File picker, asynchronous decoding, proportional preview, dimensions, clear, error messages | Three decoder tests pass; complete README manual UI walkthrough |
| 3. Sequential grayscale | Next | Backend-neutral pixel model, processing interface, sequential engine, grayscale action, original/output preview, save output | Known-color/alpha tests, original remains unchanged, exported image matches result |
| 4. Sequential filters | Planned | Generic convolution, Gaussian blur, Sobel magnitude, parameter validation | Known small matrices, constant images, borders, narrow images, alpha cases |
| 5. ExecutorService backend | Planned | Configurable worker count, independent row/tile partitions, reusable managed executor, UI backend selection | Exact agreement with sequential results; 1 worker, excess workers, odd dimensions, shutdown |
| 6. Fork/Join backend | Planned | Recursive row/tile tasks, configurable parallelism, measured split threshold | Agreement with sequential results; threshold boundaries and pool cleanup |
| 7. CPU benchmarking | Planned | Warmups, repeated measurements, baseline, timing scope, speedup/efficiency/throughput, correctness gate | Metric formulas tested; invalid output excluded; reproducible run metadata |
| 8. CUDA toolchain and bridge | Planned | Verified Toolkit/MSVC/CMake combination, standalone native executable, versioned file protocol, Java process bridge | Native build and identity round trip; timeout, failure, missing executable, cleanup |
| 9. Basic CUDA kernels | Planned | Custom grayscale, convolution/Gaussian and Sobel kernels, explicit transfers, CUDA error handling | Compare against CPU on reference fixtures and odd/non-block-aligned sizes |
| 10. Shared-memory CUDA convolution | Planned | Cooperative tile/halo loading, shared-memory convolution, selectable basic/optimized variants | Same output contract as basic kernel; boundary cases, hardware resource checks, measured comparison |
| 11. Performance dashboard | Planned | CPU/GPU comparison charts, result table, CSV export, parameter/image metadata | Export round trip, chart/table agreement, repeated-run variability and timing labels |
| 12. Integration and release | Planned | UI cancellation/progress, packaging, CI, team documentation, report, demo assets | Clean-machine build, full correctness suite, native/GPU checks on RTX 4050, reproducible demo |

Each phase is delivered and checked before the next. CI for Java builds should be
added early when the team starts parallel work; release packaging stays in Phase 12.
CUDA installation may happen earlier, but GPU implementation depends on the CPU
output contract. CUDA support for the installed Visual Studio 2026 host compiler
must be verified; install a supported compiler if necessary.

## Contracts to settle before parallel engines

- Store pixels independently of JavaFX, using a documented ARGB channel order,
  dimensions, and immutable input ownership. Convert only at the UI boundary.
- Fix grayscale weights and rounding, Gaussian kernel size/sigma/normalization,
  Sobel magnitude/clamping, convolution orientation, and border handling.
- Preserve alpha consistently; document whether transparent RGB channels contribute
  to convolution. Decide how to handle animated images (initially first frame).
- Use exact comparisons for integer-equivalent operations. Define any floating
  point channel tolerance before CUDA comparisons; report maximum/mean error and
  mismatch counts rather than silently accepting discrepancies.
- Enforce valid dimensions/kernel sizes and add practical memory limits before
  running large-image benchmarks. Keep expensive work off the JavaFX thread.

## Benchmark rules

Use the same source pixels, parameters, border rules, and output validation on all
backends. Exclude decoding and preview rendering from processing measurements.
Record separate processing and end-to-end scopes instead of mixing them.

- Use `System.nanoTime()` for Java elapsed time; warm up the JVM, repeat runs,
  and report the median with spread and repetition count.
- Speedup = sequential time / backend time for matching scope and parameters.
- CPU efficiency = speedup / configured worker count; do not apply CPU worker
  efficiency to GPU cores. Report GPU efficiency as not applicable.
- Throughput = image pixel count / elapsed seconds; label units as pixels/s or MP/s.
- For CUDA, record kernel time with CUDA events and host-device transfers
  separately. Also record process startup, file communication, and total bridge
  time so the overhead of the executable architecture is visible.
- Capture image dimensions/hash, kernel settings, backend, threads, warmups,
  repetitions, Java/CUDA versions, hardware, and validation result. Never present
  unmeasured performance claims; optimized kernels may be slower on small inputs.

## Three-person work allocation

Use these as roles, not fixed assignments; choose owners together before work.

| Role | Primary work | Coordination point |
| --- | --- | --- |
| A | JavaFX workspace, controls, charts, export, user experience | Consume agreed service/result interfaces |
| B | Pixel contracts, sequential/Executor/ForkJoin engines, validation, CPU metrics | Publish common fixtures and engine interfaces before parallel work |
| C | CUDA toolchain, executable protocol, kernels, GPU measurements | Match the shared pixel/filter contract and fixtures |

Until Phase 3 establishes interfaces, teammates can independently prepare CI,
reference fixtures, or CUDA environment verification. Avoid implementing three
incompatible image formats. Integration happens through reviewed pull requests.

## Next small commits

1. Add the pixel model and shape/input validation tests.
2. Define the processing interface and filter contract.
3. Add sequential grayscale with known-color, alpha, and boundary tests.
4. Connect grayscale to a background UI task and show original/result previews.
5. Add PNG result export with round-trip validation.
6. Update the manual checklist and record the Phase 3 handoff.

Split when a change has its own purpose and can be reviewed. Keep tightly coupled
code and tests together; each pushed commit should build. Commit count is an
outcome of completed work, not a target for artificial edits.
