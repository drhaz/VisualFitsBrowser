# Changelog

All notable changes to VisualFitsBrowser are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Release notes on GitHub
are generated from the section whose heading matches the release tag.

Entries before 1.10.4 were reconstructed from the git history.

## [Unreleased]

### Fixed
- PDF log sheets failed with an error since 1.9.0 (observation times were
  formatted with the wrong date API).
- Log sheets no longer break on comments containing `\ { } ^ ~ < >`.
- External programs (pdflatex, funpack, PDF viewer, ds9) can no longer hang the
  program by filling their output buffer, and file paths with spaces work.
- Changing directory no longer waits for, or can hang on, the old directory watcher.
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
