# Parallel VisionLab delivery roadmap

Product name: **Parallel VisionLab**, matching the repository and current window.
Target: a polished Windows desktop PDC application for a
three-person team in four weeks, with a fifth week available for stabilization.

## Current checkpoint

November 18, 2026: the core application is implemented as version 0.3.0 release
candidate. It retains the supplied HTML-style JavaFX interface. All three Java
CPU engines and both CUDA variants are connected, validated and benchmarked.
Actual RTX 4050 tests, a standalone setup clone, and bundled-runtime CPU/GPU
self-tests passed. The latest complete local suite has 65 tests with GPU and UI
tests enabled. Hosted Windows/Linux Java CI passed; hosted workers do not provide
the hardware tests.

Remaining acceptance work is manual: final visual/file-dialog walkthrough,
genuinely separate Windows computer, a broader performance dataset and team demo
rehearsal. These checks are not marked complete merely because code exists.
The original 4-5 week schedule below remains a planning baseline for that work.

## Delivery phases

| Phase | Status | Delivered behavior / evidence |
| --- | --- | --- |
| 1. Foundation | Implemented | JDK 25, modular Maven/JavaFX, GitHub |
| 2. Image workspace | Implemented | Background load, first GIF frame, dimensions, clear, fit/zoom |
| 3. Pixel/engine contracts | Implemented | Immutable ARGB, kernels, filter requests and backend selection |
| 4. Sequential grayscale | Verified | Fixed weights/rounding, alpha, known-color tests |
| 5. Processing workspace | Implemented | Original/output, background execution, PNG export |
| 6. Convolution | Verified | Orientation, replicated borders, bounds, sharpen preset |
| 7. Gaussian | Verified | Normalized kernels, bounded radius/sigma, impulse tests |
| 8. Sobel | Verified | Grayscale stage, gradients, magnitude/clamping, edge tests |
| 9. CI/team integration | Verified Java CI | Windows/Linux matrix and contribution workflow |
| 10. ExecutorService | Verified | Reusable pool, disjoint rows, worker controls, failure/shutdown |
| 11. Fork/Join | Verified | Recursive rows, threshold, cancellation, exact CPU agreement |
| 12. Validation | Verified | Shape, mismatched pixels, max/mean ARGB error; alpha exact |
| 13. Benchmark runner | Verified | Warmups, iterations, cancellation, validation outside timing |
| 14. CPU metrics | Verified | Median/min/max, speedup, CPU efficiency, MP/s |
| 15. CUDA toolchain | Verified locally | Official component ZIPs, SHA-256, CUDA 13.5.37, MSVC 19.51 |
| 16. Native bridge | Verified | Versioned files, timeout, process errors, output checks/cleanup |
| 17. Basic CUDA | Hardware verified | All four filters; odd/narrow/partial-block inputs |
| 18. Shared-memory convolution | Hardware verified | 16×16 tiles and halos; same contract and real comparison |
| 19. GPU measurements | Hardware verified | Event kernel/upload/download; separate total bridge wall time |
| 20. Dashboard | Implemented | Real overview/benchmark charts and comparison tables |
| 21. Export/history | Verified | PNG, measured CSV, versioned persistent experiment files |
| 22. Professional UX | Implemented; manual QA pending | Six pages, progress/cancel, fit/zoom, memory limits, live NVIDIA telemetry |
| 23. Distribution | Built/self-tested; separate-PC check pending | Native Windows launcher, bundled Java/CUDA, portable ZIP |
| 24. Submission | Report/demo docs ready; rehearsal pending | Reproducible example, architecture, report, release checklist |

See [project report](project-report.md), [native setup/protocol](native-cuda.md),
[release checklist](release-checklist.md), and [UI handoff](ui-dashboard.md).
No fake timing samples remain in the active UI. GPU process startup overhead is
visible; the tiled kernel is not assumed to be universally faster.

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

## Established contracts and future extensions

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

## Remaining acceptance checkpoints

1. Run the six-page manual UI/file-dialog walkthrough with each teammate.
2. Test the portable ZIP on a separate Windows computer without development tools.
3. Benchmark a wider image/filter/resolution/worker-count dataset and retain CSV.
4. Review source, error paths, report and measurement interpretation together.
5. Rehearse the presentation/demo and update the final release checklist.

Split when a change has its own purpose and can be reviewed. Keep tightly coupled
code and tests together; each pushed commit should build. Commit count is an
outcome of completed work, not a target for artificial edits.
