# HTML dashboard adaptation handoff

The reference was the user-supplied `Parallel-VisionLab-Demo.html`. Its layout and
colors were translated into native JavaFX controls, not embedded in a browser.
The HTML remains outside the repository; it is not a runtime dependency.

## Implemented screens

| Screen | Available behavior | Availability boundary |
| --- | --- | --- |
| Overview | Runtime core count/heap limit, shortcut navigation, sample comparison bars | GPU and processing engine cards show unavailable/not connected |
| Image Workspace | Background image loading, sequential grayscale, original/result previews, full-resolution PNG save, clear; state retained across navigation | Selectors fixed to grayscale/sequential; other filters and backends are not connected |
| Benchmark Lab | Five sample bars, summary cards and sortable comparison table | Every timing is illustrative; no real runs exist |
| Live Hardware Monitor | Actual OS CPU load when available, JVM heap, detected core inventory, two bounded history charts, pause/resume | No invented GPU/temperature/power/per-core readings; heap is not total RAM |
| Experiment History | Empty real-history state, sample CSV save dialog and export status/errors | CSV marks every row as sample; no persistence yet |
| Settings | Detected Java/OS/core data, session-only preference selection/feedback | No benchmark engine consumes or persists these preferences yet |

## Design

The reference's #0b101b canvas, #0e1523 sidebar, #111a2a cards, #263247 borders,
blue actions, muted labels, rounded cards, and colored comparison bars are defined
in `workspace.css`. Sidebar selection and keyboard focus remain visible. Pages
scroll vertically; the desktop minimum is 1000 × 650, with a default 1280 × 840
window. Unlike the HTML's mobile media query, a phone layout is outside this
desktop adaptation. JavaFX uses the system Segoe UI font rather than downloading
web fonts. Native file dialogs keep the Windows theme.

## Verification

On November 18, 2026, the build passed all eight tests, including the opt-in
JavaFX smoke test. That test applies CSS, traverses all six navigation targets,
checks active links, and checks workspace instance retention. Seven non-UI tests
cover decoding/file ownership, runtime measurement bounds, sample CSV provenance,
locale-safe formatting, and sample speedup arithmetic. The app was launched;
Overview and Benchmark pages were visually inspected through computer use.

Remaining manual walkthrough:

1. Open Image Workspace, choose a valid image, navigate away and return; confirm
   preview and dimensions remain. Test corrupt input, cancellation and Clear.
2. Resize between minimum and maximized sizes; confirm vertical scrolling and no
   overlapping cards or clipped controls.
3. Visit the monitor and observe changing history; pause/resume it. Navigate away
   and back; sampling should stop while the page is detached.
4. Export the sample CSV, open it in a text editor and confirm `data_type=sample`.
   Cancel the save dialog and test an unwritable destination.
5. Change Settings selections, apply and navigate away/back; session selections
   remain, but restarting the application resets them as documented.
6. Apply Grayscale after loading; confirm original stays unchanged and output
   appears. Save/reopen PNG, verify resolution, and test cancellation/failure of
   the save dialog. Clear and loading a new image must remove stale output.
7. Confirm processing is disabled without input and saving without output; every
   chart still identifies its sample or real measurement source.

Processing follow-up on November 18: all 19 tests passed, including pixel
ownership/shape, known-color rounding, alpha, interruption, PNG round trip and
JavaFX conversion pipeline checks. The six-page smoke test still passes. These
checks used code/tests only, without desktop control. Manual filter interaction
remains pending. Single-execution elapsed time is not a benchmark record.

Known limits: JavaFX 21.0.2 still emits the existing dependency-model warnings.
The opt-in UI test runs JavaFX on the test classpath and emits a configuration
warning; the application itself runs as a named module through Maven. No claim
of performance, full hardware monitoring or implemented filters follows from
these UI previews.
