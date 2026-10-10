# Configuration

## Files in your home directory

| File | Purpose |
|---|---|
| `~/.VisualFitsBrowserApp` | Settings (Java properties format, `key=value`) |
| `~/.VisualFileBrowser.usercomments` | SQLite database with your [comments](user-guide/comments.md) |

Note the different spelling: settings use *Fits*, the comment database uses *File*.

## Editing the settings file

The settings file is created on the first exit. Whenever the program looks up a
setting that is not in the file yet, it adds that setting with its default value,
so after one session the file lists most of the keys below.

!!! warning "Quit the program before editing"
    Settings are read once at start-up and written back when the program exits.
    Changes made while VisualFitsBrowser is running are overwritten on exit.

Most settings below are also changed through the menus; the remaining ones are
external program paths that you set only in this file.

## Reference

### External programs

| Key | Default | Description |
|---|---|---|
| `org.cowjumping.VisualFitsBrowser.VisualFitsBrowserApp.DS9EXEC` | first of `/usr/local/bin/ds9`, `/usr/bin/ds9`, `~/bin/ds9` that exists | ds9 binary launched when you double-click the **DS9** label |
| `org.cowjumping.VisualFitsBrowser.latex.pdflatex` | `/usr/bin/pdflatex` | `pdflatex` used for [log sheets](user-guide/logsheets.md). If it does not exist, `pdflatex` is searched automatically (see [Log sheets](user-guide/logsheets.md#finding-pdflatex)). |
| `org.cowjumping.VisualFitsBrowser.latex.openpdf` | `/usr/bin/okular` | Program that opens the log-sheet PDF. Options may follow the program, e.g. `/usr/bin/open -a Preview`. If it does not exist, the system's default PDF application is used (see [Log sheets](user-guide/logsheets.md#opening-the-pdf)). |
| `VisualFitsBrowser.latex.tmp` | `/tmp` | Directory for log-sheet LaTeX and PDF files |
| `cowumping.funpack.exec` | `/usr/bin/funpack` | `funpack` used on shift + double-click (the key really is spelled `cowumping`) |
| `tempdir` | system temp directory | Where uncompressed files from `funpack` are written |

### Behaviour

| Key | Default | Menu | Description |
|---|---|---|---|
| `org.cowjumping.VisualFitsBrowser.FileBrowserPanel.LASTDIRECTORY` | `/` | File → Change Directory | Directory opened at start-up |
| `org.cowjumping.VisualFitsBrowser.VisualFitsBrowserApp.AUTODISPLAY` | `false` | File → Auto display new image in ds9 | Send new images to ds9 automatically |
| `org.cowjumping.VisualFitsBrowser.VisualFitsBrowserApp.SHOWUTILITIES` | `false` | File → Show ToolBox | Toolbox window visible |
| `directorylistener.ignorediremodified` | `False` | — | `true` rescans the directory every 2 s even if its modification time did not change. Use this on network file systems where new files do not show up. |

### Window positions

Each window stores `.x`, `.y` and `.visible` under these prefixes. Delete these
lines to reset a window that ended up off-screen.

| Prefix | Window |
|---|---|
| `org.cowjumping.VisualFitsBrowser.VisualFitsBrowserApp.WindowLocation` | Main window |
| `org.cowjumping.VisualFitsBrowser.VisualFitsBrowserApp.ToolsBoxWindowLocation` | Toolbox |

## Example

```properties
org.cowjumping.VisualFitsBrowser.FileBrowserPanel.LASTDIRECTORY=/data/raw/20240315
org.cowjumping.VisualFitsBrowser.VisualFitsBrowserApp.AUTODISPLAY=true
org.cowjumping.VisualFitsBrowser.VisualFitsBrowserApp.DS9EXEC=/Applications/SAOImageDS9.app/Contents/MacOS/ds9
org.cowjumping.VisualFitsBrowser.latex.pdflatex=/Library/TeX/texbin/pdflatex
org.cowjumping.VisualFitsBrowser.latex.openpdf=/usr/bin/open
```
