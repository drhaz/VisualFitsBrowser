# Architecture overview

VisualFitsBrowser is a single-process Java Swing application. All source code is
under `src/main/java/org/cowjumping/`.

## Packages

| Package | Role |
|---|---|
| `VisualFitsBrowser` | Application entry point (`VisualFitsBrowserApp`: menus, windows, SAMP message handlers) and the file table (`FileBrowserPanel`). |
| `VisualFitsBrowser.util` | `FitsFileEntry` (one row of the table, created from a FITS header), `DirectoryListener` (polls for new files), `Filelist2Latex` (PDF log sheets). |
| `VisualFitsBrowser.util.FitsComments` | `FitsCommentInterface` and its implementations: SQLite (`FITSTextCommentSQLITEImp`, in use) and a per-directory text file (`FITSTextCommentImpl`, legacy). |
| `VisualFitsBrowser.ImageActions` | The Toolbox: `ImageToolBoxPanel` and the tools derived from `ImageEvaluator` (`FITSHeaderInspection`, `ImexamDisplay`). |
| `FitsUtils` | FITS helpers: fast header reading (`QuickHeaderInfo`), image buffers (`ImageContainer`), centroiding and photometry (`odiCentroidSupport`, `RadialProfile`), `funpack` wrapper. |
| `guiUtils` | Reusable Swing components (plots, z-scale selector, tables), `Preferences` (settings file) and `SAMPUtilities` (all communication with ds9). |

Several class names (`ODI…`, `OTA…`) date from the program's origin as a tool for
the WIYN One Degree Imager.

## Main flows

**Start-up.** `VisualFitsBrowserApp.main` configures logging, starts an embedded
SAMP hub (`SAMPUtilities.initHubConnector`), registers SAMP response handlers, and
builds the main window. `Preferences.initPreferences("VisualFitsBrowserApp")`
loads `~/.VisualFitsBrowserApp`.

**Reading a directory.** `FileBrowserPanel.readDirectory` stops the old
`DirectoryListener`, reads every matching file in a `SwingWorker`
(`FitsFileEntry.getImagesInDirectory` → `QuickHeaderInfo.readFITSHeader`), loads
each file's comment, then starts a new `DirectoryListener` thread. The listener
calls back `FileBrowserPanel.addSingleNewItem` for each new file.

**Threading rule for the file table.** `FileBrowserPanel.mImageList` and the table
model are changed only on the Swing event thread. Slow work (reading FITS headers)
runs in the background and hands its result to the event thread: `readDirectory`
applies the worker's result in `done()` and discards it if a newer directory read
has started; `addSingleNewItem` reads the header in the listener thread and adds
the row with `SwingUtilities.invokeLater`. Code on other threads that needs the
list must take a snapshot or hold the `Vector`'s lock while iterating.
`FileBrowserPanelTest` checks this.

**Displaying in ds9.** All ds9 commands go through `SAMPUtilities`, which sends
`ds9.set` notifications (`file fits …`, `frame …`, `lock …`) to every connected
client. Calls that need an answer (`ds9.get`, e.g. imexam) are
sent with a tag such as `imexam` or `imagecutout`; the response handlers
registered in `VisualFitsBrowserApp.initSampHub` dispatch on that tag.

**Comments.** Editing the comment cell calls `FitsFileEntry.writeBackMetaInformation`,
which hands the entry to the static `FitsCommentInterface`. The SQLite
implementation writes on a background thread.

**Settings.** `Preferences.thePreferences` is a process-wide singleton.
`getProperty(key, default)` also stores the default, so the settings file grows
to list every key that was used. The file is written in `onExit`.
See [Configuration](../configuration.md) for all keys.

## Logging

log4j2 writes to the console, configured by `src/main/java/resources/log4j2.xml`
(log level `INFO`). Start the program from a terminal to see what it is doing; the
`-debug` option switches the root logger to `DEBUG`.

External programs (pdflatex, funpack, the PDF viewer, ds9) are started through
`guiUtils.ProcessRunner`, which merges their stderr into stdout so they cannot
block on a full output pipe, and passes file names as separate arguments so paths
with spaces work. Use it for any new external tool.
