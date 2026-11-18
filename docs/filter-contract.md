# Sequential filter reference contract

All engines must eventually implement the same contract. Inputs and outputs use
immutable row-major straight-alpha ARGB pixels. Every operation returns a new
image of the original dimensions; it does not mutate the input. Each UI run
starts from the original image, not from the previous output.

## Shared rules

- Replicate borders: clamp each sample coordinate to the nearest valid pixel.
  This includes 1-pixel-wide/tall images and kernels larger than the image.
- Preserve the alpha of the center/output coordinate. RGB channels are filtered
  independently in their stored 8-bit encoding, without linear-light conversion.
  Transparent input pixels' stored RGB participates in neighborhood operations.
  This simple policy may produce color fringes on transparent images; it is
  intentional for reference consistency, not a premultiplied-alpha blur.
- Round channel values with `Math.round`, then clamp to [0,255]. Do not clamp
  intermediate gradient/channel sums. Grayscale uses the integer rule below.
- JavaFX may quantize translucent RGB on decode/display. PNG export uses computed
  pixel values directly, not the scaled/possibly quantized preview.
- Kernels are applied in the given row-major orientation without reversal
  (cross-correlation, commonly called convolution in image-processing tools).
  Symmetric blur/sharpen kernels are unaffected by this distinction.

## Operations

| Operation | Rule |
| --- | --- |
| Grayscale | `(299R + 587G + 114B + 500) / 1000`, integer division; write value to all RGB channels |
| Generic convolution | Sum each sampled RGB channel times the corresponding kernel coefficient; preserve center alpha |
| Gaussian | `exp(-(x²+y²)/(2σ²))`, normalized to total weight 1; use generic convolution |
| Sharpen | `[[0,-1,0],[-1,5,-1],[0,-1,0]]`; use generic convolution |
| Sobel | Convert to reference grayscale; Gx=`[[-1,0,1],[-2,0,2],[-1,0,1]]`, Gy=`[[-1,-2,-1],[0,0,0],[1,2,1]]`; output rounded/clamped `hypot(Gx,Gy)` in RGB |

Convolution kernels must be odd, square, size 1-31; all weights finite and total
absolute weight at most 1,000,000. Gaussian UI/API parameters: radius 1-7 (kernel
size 3-15), sigma 0.1-10. Invalid values are rejected rather than silently changed.
The UI exposes the sharpen preset, not a custom kernel editor. The generic kernel
API supports arbitrary validated coefficients for future work.

## UI and timing

Changing operation or Gaussian parameters clears stale output. During loading,
processing or saving, action/configuration controls are disabled and an activity
indicator appears. Gaussian controls are enabled only for Gaussian blur. Save
names include the operation suffix; the result is encoded as full-resolution PNG.

Elapsed time covers one engine `process` call, including kernel creation where
applicable, image allocations and copies. It excludes input decoding, preview
conversion/rendering and PNG export. It is not a warmed-up repeated benchmark and
is not added to sample dashboard/history data.

The sequential loops check thread interruption; interactive cancellation and a
managed task lifecycle remain future work. Large images and large kernels can
be slow and memory-intensive. No optimized/separable implementation is claimed.

## Validation and next engines

Fixtures cover orientation with an asymmetric kernel, border replication,
constant images, impulses, known Sobel ramps, narrow images, alpha, input
ownership, rounding/clamping, invalid parameters and interruption. PNG round-trip
tests run every exposed filter. The opt-in JavaFX test checks selection and
Gaussian-control availability without desktop automation.

ExecutorService and Fork/Join must first match the sequential fixtures exactly.
CUDA floating-point tolerances must be explicitly chosen and reported before
cross-backend acceptance; do not silently loosen them after a mismatch.
