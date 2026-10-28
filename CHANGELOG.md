# Changelog

All notable changes to VisualFitsBrowser are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Release notes on GitHub
are generated from the section whose heading matches the release tag.

Entries before 1.10.4 were reconstructed from the git history.

## [Unreleased]

## [1.12.0] - 2026-10-28

### Added
- **Display** menu for options that change how the file table looks.
- **Display → Hide .fits suffix** shows file names without `.fit`, `.fits` or
  `.fits.fz`.
- **Display → Shorten long text in the middle** (on by default) replaces the
  middle of an OBJECT name, extra column value or comment too long for its column
  with an ellipsis (`…`), so its start and end stay visible.
- **Display → Extra column keyword ...** chooses the FITS keyword shown in the
  extra column, which used to always show `CONFMODE`. Type a keyword or pick one
  of the keywords of the top image from a drop-down list. The column title now
  shows the keyword.
- Column widths can be changed by dragging the column borders and are remembered
  for the next start. **Display → Reset column widths** goes back to the defaults.
  The first four columns are no longer resized to fit their content when a
  directory is read.

### Changed
- The main window can now be resized horizontally. Its width is remembered for
  the next start.
- **Vegan icons** moved from the Help menu to the Display menu.

### Fixed
- Filter names no longer carry the blanks that pad FITS string values, e.g.
  `'rp      '` is shown as `rp`.
- Quitting with Command-Q on macOS now saves window positions and settings. They
  used to be saved only by **File → Exit** or closing the window.

## [1.11.1] - 2026-10-23

### Changed
- Opening a directory is faster, especially on network file systems such as S3
  mounts: the FITS headers of several files are read at the same time (setting
  `READTHREADS`, default 16), each header is read in one request instead of several
  small ones, and redundant checks whether a file exists were removed. These checks
  also ran for every file once per second while a directory was being watched.

## [1.11.0] - 2026-10-21

### Added
- A button between `<` and `>` switches between the `raw` and `processed` version of
  the current directory. Its icon shows which one is displayed: a cow and minced
  meat, or wheat and bread with **Help → Vegan icons**.
- Clicking the directory path opens the directory chooser.
- A directory given on the command line is opened at start-up instead of the last
  one.

## [1.10.6] - 2026-10-14

### Changed
- Removed leftover references to the WIYN One Degree Imager (ODI): the SAMP client
  now registers as "VisualFitsBrowser", `odiCentroidSupport` is renamed to
  `CentroidSupport`, and unused ODI-specific code was deleted.
- The About dialog shows the cowjumping logo and links to cowjumping.net.

### Removed
- The handlers for the ODI SAMP messages `odi.otalistener.displayedImage` and
  `odi.iraf.imageLoadReply`. VisualFitsBrowser no longer subscribes to them.

### Fixed
- Log sheet file names are now built from the name of the current directory.
  Previously they assumed a path containing `podi` and otherwise cut off the first
  characters of the path.

## [1.10.5] - 2026-10-09

### Fixed
- Log sheets: if the configured PDF viewer (default `/usr/bin/okular`) does not
  exist, the PDF is opened with `open` on macOS, or with `xdg-open`, `gio open` or
  a common PDF viewer on Linux. If no viewer is found, an error dialog shows where
  the PDF was written. A failed pdflatex run is now reported instead of opening a
  stale PDF from an earlier run.
- Log sheets: if the configured `pdflatex` does not exist, `pdflatex` is now
  searched on `PATH` and in the usual TeX installation directories (MacTeX,
  Homebrew, MacPorts, TeX Live). If none is found, an error dialog explains how
  to fix it instead of failing silently.
- PDF log sheets failed with an error since 1.9.0 (observation times were
  formatted with the wrong date API).
- Log sheets no longer break on comments containing `\ { } ^ ~ < >`.
- External programs (pdflatex, funpack, PDF viewer, ds9) can no longer hang the
  program by filling their output buffer, and file paths with spaces work.
- Changing directory no longer waits for, or can hang on, the old directory watcher.
- The file table is now only updated on the Swing event thread. Fixes duplicated
  rows after quickly reloading or switching directories, and files from a
  previously shown directory appearing in the table.
- The ds9 indicator and the free-disk-space bar are updated on the Swing event
  thread; the disk-space bar is shown right at start-up.
- The `-debug` option works, the log configuration file is actually used, and the
  default log level is `INFO` instead of always `DEBUG`.

### Changed
- The PDF viewer is started without waiting for it to close, so
  **Generate PDF logfile** becomes available again right away.
- `-h` prints the command-line options.

### Security
- log4j 2.17.1 → 2.25.5 (GHSA-vc5p-v9hr-52mj, GHSA-6hg6-v5c8-fphq,
  GHSA-3pxv-7cmr-fjr4, GHSA-qv9r-c865-cp47).
- commons-io 2.11.0 → 2.20.0 (GHSA-78wr-2p64-hpwj).

### Removed
- Unused commons-lang3 dependency (GHSA-j288-q9x7-2f5v).

## [1.10.4] - 2026-10-09

### Added
- **Help** menu with links to the online documentation and an About dialog.
- Documentation moved from the GitHub wiki into `docs/` and published at
  <https://drhaz.github.io/VisualFitsBrowser/>, together with the Javadoc API
  reference.

### Changed
- Compiled with `--release 8`, so the jar is guaranteed to run on Java 8 even when
  built with a newer JDK.
- Release notes are taken from this changelog.
- Tests use JUnit 4.13.2 (JUnit 3 is no longer supported by current Maven Surefire).
- Updated table tooltips.

### Removed
- The experimental **Wavefront** menu (DONUT wavefront analysis). Imexam remains
  available from the Toolbox.
- Audible signal when a new image arrives.
- Binary jar of version 1.6 from the source repository; use GitHub Releases.

## [1.10.3] - 2026-05-08

### Changed
- Fixes to the automated release workflow.

## [1.10.2] - 2026-05-08

### Changed
- Fixes to the automated release workflow.

## [1.10.1] - 2026-05-07

### Changed
- Releases are built and published by GitHub Actions when a tag is pushed.
  Travis CI was removed.

## [1.10.0] - 2025-10-29

### Added
- Imexam `x` key: line profile along x, for measuring the width of spectral lines.

### Changed
- sqlite-jdbc updated to 3.41.2.2.

## [1.9.1] - 2025-01-31

### Fixed
- ds9 8.6 and newer are detected again (ds9 changed its SAMP name from `DS9` to `ds9`).
- Compiled for Java 8 again.

## [1.9.0] - 2022-10-05

### Changed
- Dependency updates; dates are handled with `java.time.LocalDateTime`.

### Fixed
- log4j logger initialisation on Java versions newer than 8.

## [1.8.3] - 2021-03-24

### Added
- `directorylistener.ignorediremodified` setting for file systems that do not
  update the directory modification time.

### Changed
- ds9 is launched with `-samp connect`.

## [1.8.1] - 2020-03-10

### Changed
- Comments are stored in a SQLite database in the home directory instead of a
  `.fitscomments` file in each image directory, and are written in the background.
- Logging migrated to log4j 2.

## [1.7] - 2019-03-19

### Fixed
- Parsing of the `CCDSUM` keyword.

## [1.6] - 2018-03-06

### Fixed
- New files were not detected when data transfer was slow.
- Highlighting of the displayed image for binned images.
- More robust `DATE-OBS` parsing.
