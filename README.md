# VisualFitsBrowser

[![Latest release](https://img.shields.io/github/v/release/drhaz/VisualFitsBrowser)](https://github.com/drhaz/VisualFitsBrowser/releases/latest)
[![Documentation](https://img.shields.io/badge/docs-online-blue)](https://drhaz.github.io/VisualFitsBrowser/)
[![License: MIT](https://img.shields.io/badge/license-MIT-green)](LICENSE)

VisualFitsBrowser is a small utility program to help organizing astronomical images in the FITS format, and
is meant to assist a workflow of inspecting images in SAOImage ds9 in an observatory or lab environment.

The center of the application is a list view of all FITS images in a directory, sortable by selected
header keywords (OBJECT, DATE-OBS, FILTER, ...), that updates automatically when new images arrive.
Double-clicking on a file sends the image to ds9 for display. You can store comments for each image and
generate a PDF log sheet via a local LaTeX installation.

![VisualFitsBrowser main window](docs/images/VisualFitsBrowser-main.png)

## Requirements

- Java 8 or newer
- [SAOImage ds9](https://sites.google.com/cfa.harvard.edu/saoimageds9) (recommended, for image display)
- `pdflatex` (optional, for PDF log sheets)

## Quick start

1. Download `VisualFitsBrowser-<version>-jar-with-dependencies.jar` from the
   [latest release](https://github.com/drhaz/VisualFitsBrowser/releases/latest).
2. Run it:

   ```sh
   java -jar VisualFitsBrowser-<version>-jar-with-dependencies.jar
   ```

3. Choose **File → Change Directory ...**, and start ds9 so it connects to VisualFitsBrowser.

## Documentation

The full documentation is at **<https://drhaz.github.io/VisualFitsBrowser/>** (also in [`docs/`](docs/index.md)):

- [Installation](docs/installation.md)
- User guide: [file browser](docs/user-guide/file-browser.md), [comments](docs/user-guide/comments.md),
  [log sheets](docs/user-guide/logsheets.md), [the Toolbox](docs/user-guide/toolbox.md)
- [Configuration reference](docs/configuration.md)
- [Building from source](docs/developer/building.md) and other developer topics
- [Changelog](CHANGELOG.md)

## Contributing

Bug reports and pull requests are welcome on the [issue tracker](https://github.com/drhaz/VisualFitsBrowser/issues).
To build: `mvn package`. If your change affects what users see, please update the matching page in `docs/`.

## License

MIT, see [LICENSE](LICENSE). Included third-party libraries are listed in [CREDITS](CREDITS).
