# Log sheets (PDF export)

VisualFitsBrowser can turn the file list of the current directory into a printable
PDF observing log that includes your [comments](comments.md).

## Creating a log sheet

Choose **File → Generate PDF logfile**. The program will:

1. write a LaTeX file named `<directory name>.tex` into the temporary directory
   (`/tmp` by default);
2. run `pdflatex` on it three times (needed for the "page X of Y" footer);
3. open the resulting PDF in your PDF viewer (see [Opening the PDF](#opening-the-pdf)).

The menu item is disabled while this runs. A plain-text version of the table
(columns separated by `&`) is also written to `/tmp/VisualFitsBrowser_Logfile.txt`.

## What the log sheet contains

The PDF is in landscape format, titled with the full directory path, and lists one
entry per image, sorted by observation time:

| Column | Content |
|---|---|
| Obs. ID | File name |
| Object / R.A. Dec | `OBJECT`, and below it `RA` and `DEC` |
| T<sub>exp</sub> | Exposure time in seconds |
| Filter | `FILTER` |
| Time | Time part of `DATE-OBS` (`HH:MM:SS`) |
| Focus / Airmass | `TELFOCUS`, and below it `AIRMASS` |
| User comments | Your comment |

!!! note
    The column header says "TIME-Obs [MST]", but the time is taken from
    `DATE-OBS` unchanged, so it is in whatever time system your camera writes
    (usually UTC). Images without a readable `DATE-OBS` get the time they were
    loaded.

## Requirements and settings

You need a LaTeX installation with the `longtable`, `geometry`, `fancyhdr`,
`multirow` and `lastpage` packages (all part of a standard TeX Live or MacTeX).

The programs used are set in the [configuration file](../configuration.md):

| Key | Default | Typical alternatives |
|---|---|---|
| `org.cowjumping.VisualFitsBrowser.latex.pdflatex` | `/usr/bin/pdflatex` | `/Library/TeX/texbin/pdflatex` (MacTeX) |
| `org.cowjumping.VisualFitsBrowser.latex.openpdf` | `/usr/bin/okular` | `/usr/bin/open` (macOS), `/usr/bin/xdg-open`, `/usr/bin/evince` |
| `VisualFitsBrowser.latex.tmp` | `/tmp` | any writable directory |

### Finding pdflatex

If the configured `pdflatex` does not exist, VisualFitsBrowser looks for
`pdflatex` itself and uses the first one found in:

1. the directories on your `PATH`;
2. `/Library/TeX/texbin` (MacTeX), `/opt/homebrew/bin`, `/usr/local/bin`,
   `/opt/local/bin` (MacPorts), `/usr/bin`;
3. `/usr/local/texlive/<year>/bin/<platform>` (TeX Live installed from
   upstream), newest year first.

The search is needed on macOS in particular: programs started from the Finder or
the Dock do not see the `PATH` of your shell. A warning in the log shows which
program was used; the setting itself is not changed. If no `pdflatex` is found,
an error dialog asks you to install TeX or set the full path.

### Opening the PDF

If the configured PDF viewer does not exist (the default `/usr/bin/okular` is often
not installed), the PDF is opened the usual way for your system:

- **macOS**: with `open`, i.e. in your default PDF application (normally Preview).
- **Linux and other Unix systems**: with the first available of `xdg-open` and
  `gio open` (your desktop's default PDF application), then `evince`, `okular`,
  `atril`, `xreader`, `zathura`, `qpdfview`, `mupdf`, `xpdf`.
- Otherwise, through Java's desktop integration.

A viewer configured with a full path is also found if it is installed in a
different directory. A warning in the log shows which program was used; the setting
is not changed. If no viewer can be started, an error dialog shows where the PDF
was written.

## Customising the layout

The LaTeX preamble and closing lines come from the templates
[`LatexLog_header.tex`](https://github.com/drhaz/VisualFitsBrowser/blob/master/src/main/java/resources/latexlog/LatexLog_header.tex)
and
[`LatexLog_footer.tex`](https://github.com/drhaz/VisualFitsBrowser/blob/master/src/main/java/resources/latexlog/LatexLog_footer.tex),
which are built into the jar. `$TITLE$` in the header is replaced by the directory
path. To change the layout, edit these files and
[rebuild](../developer/building.md).
