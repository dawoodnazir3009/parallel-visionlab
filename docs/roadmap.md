# Parallel VisionLab delivery roadmap

Product name: **Parallel VisionLab**, matching the repository and current window.
Target: a polished Windows desktop PDC application for a
three-person team in four weeks, with a fifth week available for stabilization.

## Current checkpoint

As of November 18, 2026, Phases 1-2 and the core of Phases 3-5 are implemented:
immutable ARGB pixels, a processing interface, sequential grayscale, background
input/output preview, and full-resolution PNG export. All 19 tests pass with the
opt-in desktop smoke test enabled. The latest processing work was tested without
desktop automation; the real picker/process/save walkthrough still needs a user
check. Charts and sample CSV remain illustrative, not recorded benchmarks.
CPU/JVM telemetry is available; parallel engines, GPU telemetry, CUDA kernels and
real benchmarks are not implemented.

Sequential-filter follow-up: generic convolution, Gaussian blur, Sobel and sharpen
are now implemented behind immutable processing requests. The workspace exposes
all four filters and bounded Gaussian parameters; changing configuration clears
stale output. The expanded suite passes 35 tests with the desktop smoke test
enabled. Full manual image/filter/save interaction still needs user validation.
Next: Phase 9 CI/team integration, then Phase 10 ExecutorService. The unchanged
sample benchmark/history screens do not represent these single filter runs.

## Delivery phases

| Phase | Status | Deliverables | Completion check |
| --- | --- | --- | --- |
| 1. Foundation | Done | Java 21 modular Maven app, JavaFX window, environment inventory, Git repository | Build passes and real window opens |
| 2. Image workspace | Implemented | File picker, asynchronous decoding, proportional preview, dimensions, clear, error messages | Three decoder tests pass; complete README manual UI walkthrough |
| 3. Pixel and engine contracts | Implemented | Immutable pixels, filter requests, operation dispatch and validated kernels | Input validation and ownership tests pass |
| 4. Sequential grayscale | Implemented | CPU reference implementation and known-color fixtures | Exact pixels, alpha, input ownership and interruption tests pass |
| 5. Processing workspace | Implemented; manual check pending | Original/result previews, background grayscale, save PNG; selectors fixed to supported operation/backend | Pipeline and export tests pass; manual load/process/save checklist pending |
| 6. Convolution core | Implemented | Validated kernels, correlation orientation, replicated borders, sharpen preset | Hand-calculated matrices, alpha and clamping tests pass |
| 7. Gaussian blur | Implemented | Normalized kernel generation, sigma/radius controls | Symmetry, normalization, impulse/constant-image tests pass |
| 8. Sobel edges | Implemented | Reference grayscale, gradient magnitude, clamping | Flat/narrow images and known-edge tests pass |
| 9. CI and team integration | Planned | Java build/test workflow, fixtures, PR checklist | Clean clone builds in CI; CUDA suite separate |
| 10. ExecutorService engine | Planned | Row/tile partitions, thread configuration, managed pool | Reference agreement; excess workers and pool cleanup |
| 11. Fork/Join engine | Planned | Recursive tasks, split threshold, parallelism controls | Reference agreement; threshold boundaries |
| 12. Validation service | Planned | Exact/tolerant comparisons, maximum/mean error, mismatch count | Deliberate mismatches detected; tolerance fixed in advance |
| 13. Benchmark runner | Planned | Warmups, repetitions, timing scopes, cancellation, run metadata | Stable lifecycle; invalid outputs excluded |
| 14. Metrics and CPU comparison | Planned | Median/spread, speedup, CPU efficiency, throughput | Formula tests and comparable baseline scopes |
| 15. CUDA toolchain | Planned | Compatible Toolkit/MSVC/CMake, device detection, native executable | Build and device smoke check on RTX 4050 |
| 16. Java/native bridge | Planned | Versioned protocol, process invocation, timeout, failure handling | Identity round trip, malformed output, missing executable, cleanup |
| 17. Basic CUDA filters | Planned | Custom grayscale, convolution/Gaussian, Sobel kernels | Reference comparison including partial blocks |
| 18. Shared-memory convolution | Planned | Cooperative tiles/halos, basic/optimized selector | Output agreement, resource checks, measured comparison |
| 19. GPU measurement | Planned | Kernel events, transfer and total bridge timings | Synchronization and timing scopes verified |
| 20. Performance dashboard | Planned | Charts, sortable results, run configuration and validation badges | Table/chart agreement; empty/error states |
| 21. Results export | Planned | CSV with metadata, units and scope; saved output images | CSV escaping and exported values tested |
| 22. Professional UX | Planned | Consistent branding/theme, keyboard actions, zoom/fit, progress/cancel, diagnostics, memory limits | Responsive workflows, accessible focus/contrast, actionable errors |
| 23. Windows distribution | Planned | Runtime bundle/installer, native executable discovery, setup guide | Runs on clean Windows without development JDK; CPU mode works without CUDA |
| 24. Final validation and submission | Planned | Reproducible experiments, screenshots, architecture/report, presentation, demo script | Full suite and demo rehearsal; limitations documented |

Phase numbers describe deliverables, not 24 serial work blocks. Each deliverable
must pass its checks before dependent work integrates. CI and CUDA environment
verification can start alongside Phases 3-5. Native kernels depend on the agreed
CPU contract; charts depend on real measured results.

## Four-week delivery with a fifth-week buffer

Planning starts Monday, November 16, 2026. Four-week delivery is December 13;
the optional fifth week ends December 20. These are planning targets, not a
confirmed course submission deadline. Reassess scope at every weekly review.

| Week | Dates | UI/integration owner A | CPU/validation owner B | CUDA owner C | Weekly acceptance gate |
| --- | --- | --- | --- | --- | --- |
| 1 | Nov 16-22 | Phases 5, 9; manual Phase 2 check | Phases 3, 4, 6-8 | Phase 15; agree Phase 16 protocol | All sequential filters usable; CI green; CUDA compiler/device verified |
| 2 | Nov 23-29 | Backend controls; Phase 20 layout | Phases 10-14 | Phases 16-17 | Three CPU backends validated; first CUDA results match reference |
| 3 | Nov 30-Dec 6 | Phases 20-21; start 22 | Complete benchmark/validation integration | Phases 18-19 | Four backends measured; shared-memory comparison; charts and CSV work |
| 4 | Dec 7-13 | Phases 22-23 | Regression, experiments and report | Native packaging, GPU experiments | Phase 24 release candidate; clean-machine test and demo rehearsal |
| 5 | Dec 14-20 | Usability fixes | Validation/report corrections | GPU reliability fixes | Final release and submission; no new core scope |

This is ambitious and assumes regular contributions from all three members.
If toolchain verification fails in Week 1, resolve it immediately rather than
deferring GPU work. Verify CUDA support for Visual Studio 2026; install a supported
host compiler if needed. Start a packaging prototype by Week 2 to uncover runtime
distribution problems early.

### Scope priorities

Required: four engines; grayscale/Gaussian/Sobel/convolution; basic and optimized
CUDA convolution; thread controls; original/output previews; correctness checks;
reproducible benchmarks; charts; CSV; saved images; responsive error handling;
Windows launch instructions; report and demo.

Professional polish: consistent visual hierarchy, bounded memory usage, keyboard
navigation, zoom/fit, progress/cancel, backend availability explanations, useful
empty states and diagnostics. Plan these into the release, not as last-day styling.

Stretch only after the Week 3 gate: batch image runs, persisted experiment history,
editable custom kernels, extra themes, and additional filters. No cloud accounts,
JNI, video processing, or unrelated features in the four-week core.

If behind schedule, defer stretch features and elaborate visual effects first.
Do not remove correctness validation or invent performance numbers to meet a date.

## Function-level commit policy

Commit after each completed function or small behavior, together with the tests
and minimal supporting code that make it correct. A half-connected function is
not a finished checkpoint: group necessary interface/call-site changes so the
commit builds. Use descriptive messages; push the task branch after verification.
Record UI checks when unit tests cannot cover behavior. No empty commits or
unnecessary rewrites to inflate activity.

The requested 1,000-commit minimum is not a delivery acceptance criterion. It
would require roughly 29-36 commits every calendar day across 4-5 weeks (about
10-12 per person daily), regardless of the work needed. Track completed functions,
passing checks and weekly gates instead. Preserve legitimate small commits in
PR merges; the final count should reflect real work.

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

1. Add a Java 21 Maven CI workflow and document task ownership.
2. Define disjoint row partitions and test odd sizes/excess worker counts.
3. Implement ExecutorService grayscale, then convolution and Sobel.
4. Test exact sequential/parallel agreement for all operations and parameters.
5. Add managed executor shutdown and backend/thread-count controls.
6. Complete the manual sequential filter walkthrough before parallel integration.

Split when a change has its own purpose and can be reviewed. Keep tightly coupled
code and tests together; each pushed commit should build. Commit count is an
outcome of completed work, not a target for artificial edits.
