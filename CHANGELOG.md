# Changelog

All notable changes to VisualFitsBrowser are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Release notes on GitHub
are generated from the section whose heading matches the release tag.

Entries before 1.10.4 were reconstructed from the git history.

## [Unreleased]

## [1.10.4]

### Added
- **Help** menu with links to the online documentation and an About dialog.
- Documentation moved from the GitHub wiki into `docs/` and published at
  <https://drhaz.github.io/VisualFitsBrowser/>, together with the Javadoc API
  reference.

### Changed
- Compiled with `--release 8`, so the jar is guaranteed to run on Java 8 even when
  built with a newer JDK.
- Release notes are taken from this changelog.
- Updated table tooltips.

### Removed
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
