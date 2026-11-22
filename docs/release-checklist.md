# Release and team acceptance

## Verified on the development computer

- Java 25 Maven build and regression suite.
- Actual RTX 4050 basic/tiled kernel tests for all four filters.
- Exact three-CPU-backend equality and declared GPU RGB tolerance/alpha rules.
- End-to-end JavaFX benchmark button, saved history and archive restoration.
- Windows/Linux hosted Java CI success on the integrated implementation.
- Published standalone setup: new clone, dependencies, build/tests.
- Native setup: local CUDA ZIP verification, compiler/build/device checks.
- Bundled Java runtime CPU/GPU no-display self-tests.
- Native Windows launcher generation; static CUDA/MSVC runtime imports checked.
- Deterministic five-backend measurement CSV and report.

## Manual checks still required before course submission

- Walk through all six pages at minimum/maximized window sizes. Review focus,
  readability, scroll behavior and status/error messages on the final design.
- Load PNG/JPEG/BMP/GIF, cancel picker, select a corrupt image, and try an
  oversized image. Verify previous state is preserved on failure.
- Process every filter/backend, change Gaussian/worker/threshold controls, cancel
  a long run, save/reopen PNG, and verify full resolution and original preservation.
- Run and cancel comparisons; confirm no partial experiment is saved as successful.
- Restart and restore history/settings. Export CSV with commas/quotes in filenames
  and test an unwritable destination. Back up local experiment files.
- Inspect actual NVIDIA monitor readings, pause/resume, and navigation lifecycle.
  Unsupported CPU/per-core sensors must remain labeled unavailable.
- Extract the portable ZIP on a separate Windows x64 computer with no JDK/Maven.
  Test CPU mode; on NVIDIA hardware, test a compatible driver and both GPU variants.
- Rehearse the report/demo with all three contributors; choose actual role ownership.

## Quick development commands

    powershell -NoProfile -ExecutionPolicy Bypass -File .\setup.ps1 -InstallNative -NoLaunch
    .\.tools\apache-maven-3.9.11\bin\mvn.cmd "-Dmaven.repo.local=.tools/repository" "-Dvisionlab.cudaTests=true" "-Dvisionlab.uiTests=true" verify
    powershell -NoProfile -ExecutionPolicy Bypass -File .\package.ps1

Distribute the entire target/ParallelVisionLab-Windows.zip. Keep the generated
runtime/legal and THIRD_PARTY notices. Source is on GitHub; local toolchains,
experiments and binaries are ignored. This checklist does not assert that the
pending separate-machine or final manual UI checks have already happened.
