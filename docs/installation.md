# Installation

## Requirements

| Component | Required? | Notes |
|---|---|---|
| Java runtime 8 or newer | yes | Any vendor (e.g. [Eclipse Temurin](https://adoptium.net/)). |
| [SAOImage ds9](https://sites.google.com/cfa.harvard.edu/saoimageds9) | recommended | Needed to display images and for the imexam tool. Version 8.6 or newer is recommended; with older versions the ds9 status indicator may stay greyed out even though display works. |
| `pdflatex` (TeX Live, MacTeX, ...) | optional | Only needed for [PDF log sheets](user-guide/logsheets.md). |
| `funpack` (CFITSIO) | optional | Only needed to uncompress `.fits.fz` files before display (shift + double-click). |

## Download

Pre-built releases are published on the
[GitHub Releases page](https://github.com/drhaz/VisualFitsBrowser/releases/latest).
Download the file named `VisualFitsBrowser-<version>-jar-with-dependencies.jar`;
it contains all required libraries.

## Start the program

```sh
java -jar VisualFitsBrowser-<version>-jar-with-dependencies.jar
```

Replace `<version>` with the version you downloaded. On start-up the program prints
its version and starts a SAMP hub. The window title also shows the version.

On first start the program opens the file system root `/`. Use
**File → Change Directory ...** to pick your data directory; the choice is
remembered for the next start.

### Connecting ds9

VisualFitsBrowser runs its own SAMP hub. Start ds9 after VisualFitsBrowser (or
start it with `ds9 -samp connect`) so that it registers with the hub.

The **DS9** label on the right side of the menu bar shows whether ds9 is
connected (it is greyed out when not). Double-clicking the greyed-out label
launches a new ds9 instance. The program looks for ds9 in `/usr/local/bin/ds9`,
`/usr/bin/ds9` and `~/bin/ds9`; set the `DS9EXEC` key described in
[Configuration](configuration.md) if your ds9 lives elsewhere.

## Building from source

See [Building from source](developer/building.md).
