# Wavefront analysis (experimental)

!!! warning "Experimental"
    This feature was built for in-house telescope commissioning. It depends on an
    external, unpublished Python tool and currently needs code changes to work on a
    different computer (see [Limitations](#limitations)).

VisualFitsBrowser can send a defocused star image ("donut") to the external fitter based on the DONUT algorithm (Tokovinin & Heathcote 2006,
PASP 118, 1165), which estimates the
first 11 Zernike wavefront coefficients and returns a text summary and a plot.

## Usage

1. Open the result window with **Wavefront → Show Wavefront Frame**.
2. Display an extra-focal image in ds9.
3. Choose **Wavefront → DONUT from ds9 pick**, then click on a donut in ds9.
4. The fitter runs in the background on a 250 × 250 pixel box around the clicked
   position. When it finishes, the **Donut Display** window shows the fitted
   coefficients and the fit image.

## How it works

The program runs:

```
<executable> -i <image> -p <config.json> -o /tmp/donutfit_<timestamp> --extra -x <x> -y <y> -w 250
```

from the executable's directory, then reads `/tmp/donutfit_<timestamp>.txt` and
`.png`.

## Limitations

- The paths to the fitter executable and its JSON configuration are currently
  hard-coded in `org.cowjumping.donut.pyDonutBridge`. The configuration keys
  `donutbridge.executable`, `donutbridge.donutconfig` and `donutbridge.tmpdir` exist
  in that class, but the menu action uses a constructor that does not read them.
- Only extra-focal images are supported from the menu.
- For multi-extension files the extension the user clicked on is ignored.
