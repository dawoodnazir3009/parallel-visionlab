# Current development stage

Run and test the application from source using setup.ps1 or Maven while the
project is being finalized.

Portable EXE and ZIP generation is deferred until the user explicitly requests
the final portable release. Do not run package.ps1, javafx:jlink or jpackage, or
regenerate portable distributions during ordinary development and verification.
The packaging script is retained for that final release.

The native visionlab_cuda.exe built by build-cuda.ps1 remains necessary for GPU
processing from source. Keep this development engine and the installed toolchains;
they are distinct from portable desktop application bundles.
