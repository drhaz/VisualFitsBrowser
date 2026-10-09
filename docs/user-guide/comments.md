# Comments

Comments let you keep notes on individual images ("clouds", "focus run 3",
"saturated") without modifying the FITS files.

## Adding and editing a comment

Click into the **Comment** column of a row in the file table, type your note and
press `Enter` (or click elsewhere). The comment is saved immediately.

Comments appear in the table, in tooltips, and in [PDF log sheets](logsheets.md).

## Where comments are stored

All comments are kept in a single [SQLite](https://sqlite.org) database in your
home directory:

```
~/.VisualFileBrowser.usercomments
```

The database has one table:

```sql
CREATE TABLE usercomments (filename TEXT PRIMARY KEY, comment TEXT);
```

Because the database lives in your home directory, you do **not** need write
access to the image directory to add comments.

!!! warning "Comments are keyed by file name only"
    The directory is not part of the key. Two files with the same name in different
    directories share one comment. This is usually fine for observatory data,
    where file names contain the date and a sequence number, but be careful with
    generic names such as `test.fits`.

### Reading comments from other programs

Any SQLite client can read the database, for example:

```sh
sqlite3 ~/.VisualFileBrowser.usercomments \
  "SELECT filename, comment FROM usercomments WHERE comment <> '' ORDER BY filename;"
```

### Backup and moving to another computer

Copy `~/.VisualFileBrowser.usercomments` while VisualFitsBrowser is not running.

### Versions before 1.8

Older versions stored comments in a `.fitscomments` text file inside each image
directory. Those files are no longer read. They use the Java properties format
(`filename=comment` per line). To migrate, insert their entries into the database,
for example with the script below. It does not undo Java's backslash escapes
(`\:`, `\uXXXX`), so check comments that contain special characters afterwards.

```sh
cd /path/to/images
python3 - <<'PY'
import sqlite3, os, configparser
db = sqlite3.connect(os.path.expanduser("~/.VisualFileBrowser.usercomments"))
db.execute("CREATE TABLE IF NOT EXISTS usercomments (filename TEXT PRIMARY KEY, comment TEXT)")
cp = configparser.ConfigParser(delimiters=("=", ":"), interpolation=None)
cp.optionxform = str
cp.read_string("[c]\n" + open(".fitscomments").read())
db.executemany("INSERT OR REPLACE INTO usercomments VALUES (?, ?)", cp["c"].items())
db.commit()
PY
```
