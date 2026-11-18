# Final dashboard handoff

The user-supplied Parallel-VisionLab-Demo.html defines the visual direction. It was
translated into native JavaFX controls, not embedded in a browser. The HTML file
is not a runtime dependency.

| Screen | Implemented behavior |
| --- | --- |
| Overview | Detected resources/backend availability and latest real measured comparison |
| Image Workspace | Background image load, first GIF frame, four filters, three CPU/two optional GPU backends, thread/threshold/Gaussian controls, original/output, fit/zoom, cancel, full-resolution PNG |
| Benchmark Lab | Loaded image/filter, warmups/repetitions, CPU workers, optional CUDA, progress/cancel, validated median/min/max, speedup/efficiency/throughput and separate GPU events |
| Live Hardware Monitor | CPU/JVM charts; optional actual NVIDIA utilization, VRAM, power and temperature; pause/resume; lifecycle cleanup |
| Experiment History | Persistent measured records, validation provenance and measured CSV export |
| Settings | Detected runtime, saved benchmark defaults consumed by Benchmark Lab |

## Styling and behavior

workspace.css retains the HTML reference's #0b101b canvas, #0e1523 sidebar,
#111a2a cards, #263247 borders, blue primary actions, muted text, rounded cards,
comparison bars and tables. The desktop minimum is 1000×650; the default is
1280×840. Pages scroll vertically. The HTML phone layout is not part of this
Windows desktop adaptation. Native file dialogs use the Windows theme.

State is shared across persistent pages. Benchmarks consume the loaded original
and selected operation. Busy state prevents conflicting image/config changes;
partial or invalid benchmark runs are not published as successful records.
Changing filter/backend/parameters clears stale processed output. Saving uses the
computed immutable pixels rather than the rendered preview.

GPU choices appear when the executable exists; operation failures clearly report
missing/incompatible hardware/driver. CPU per-core utilization and CPU
power/temperature remain unavailable instead of showing fabricated values.
No illustrative benchmark data remains in active pages.

## Verification boundary

The complete local suite passed 65 tests with UI and GPU tests enabled. The UI
tests apply CSS, traverse all pages, preserve workspace instances, verify filter,
Gaussian, worker and threshold gating, fit/zoom behavior, and execute the actual
benchmark button through measurement, persistence and archive restoration.

Earlier Overview/Benchmark layouts were visually inspected through computer use.
Subsequent work used code/toolkit/hardware tests without desktop control. A final
manual visual/file-dialog walkthrough and separate-machine installation are
still required; automated tests do not assert those checks have happened.
See [release-checklist.md](release-checklist.md).
