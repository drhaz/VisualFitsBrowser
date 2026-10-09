# VisualFitsBrowser

VisualFitsBrowser is a small desktop tool for anybody working with large numbers of
astronomical FITS images — in a lab, at the observatory, or during data reduction.
It lists the FITS files in a directory, watches that directory for new arrivals,
lets you attach notes to individual images, and sends images to
[SAOImage ds9](https://sites.google.com/cfa.harvard.edu/saoimageds9) for display.

![VisualFitsBrowser main window](images/VisualFitsBrowser-main.png)

## Key features

- **Directory listing** of all `.fit`, `.fits` and `.fits.fz` files, with the
  `OBJECT`, exposure time, `FILTER`, `AIRMASS` and `DATE-OBS` header values in a
  sortable table. New files are picked up automatically.
  See [File browser](user-guide/file-browser.md).
- **ds9 integration via SAMP**: double-click to display an image, send a whole
  selection to ds9 frames, or auto-display every new image as it arrives.
- **Comments** per image, stored outside the FITS file so the data is never
  modified. See [Comments](user-guide/comments.md).
- **PDF log sheets** of a night's images, including your comments, via a local
  LaTeX installation. See [Log sheets](user-guide/logsheets.md).
- **The Toolbox** for quick inspection: a searchable FITS header viewer and an
  imexam-style star/line profile analysis. See [The Toolbox](user-guide/toolbox.md).

## Getting started

1. [Install](installation.md) Java and download the latest release.
2. Start the program with `java -jar VisualFitsBrowser-<version>-jar-with-dependencies.jar`.
3. Choose **File → Change Directory ...** and pick a directory with FITS files.
4. Start ds9; it connects automatically to the SAMP hub that VisualFitsBrowser runs.

Settings are described in [Configuration](configuration.md). If you want to build
from source or extend the program, start with the [developer guide](developer/building.md).

## Getting help

Bug reports and feature requests are welcome on the
[issue tracker](https://github.com/drhaz/VisualFitsBrowser/issues).
