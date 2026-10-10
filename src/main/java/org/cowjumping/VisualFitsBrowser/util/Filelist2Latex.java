package org.cowjumping.VisualFitsBrowser.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cowjumping.VisualFitsBrowser.FileBrowserPanel;
import org.cowjumping.guiUtils.Preferences;
import org.cowjumping.guiUtils.ProcessRunner;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class Filelist2Latex {

	private final static Logger myLogger = LogManager.getLogger(Filelist2Latex.class);
	private final static DateTimeFormatter mDateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static FileBrowserPanel myFileBrowserPanel;

	public static void writeFileList2Latex(String Title,
                                           Vector<FitsFileEntry> fileLis, String fname) {

		StringBuilder sb = new StringBuilder();

		sb.append(generateLatexHeader(Title));
		sb.append(generateFileTable(fileLis));
		sb.append(generatelatexFooter());

		if (myLogger.isDebugEnabled()) {
			myLogger.debug(sb.toString());
		}

		try {
			myLogger.info("Writing latex file to: " + fname);
			FileUtils.writeStringToFile(new File(fname), sb.toString(), (Charset) null);
		} catch (IOException e) {
			myLogger.error("Error while writing latex source to File ", e);
		}

    }

	private static String generateFileTable(Vector<FitsFileEntry> fileList) {

		StringBuilder sb = new StringBuilder();
		StringBuilder CSV = new StringBuilder();
		if (fileList != null) {

			List<FitsFileEntry> sortList = new Vector<FitsFileEntry>(fileList);
			sortList.sort(new myComp());
			for (FitsFileEntry fe : sortList) {

				sb.append(generateFileItem(fe));
				CSV.append(generateFileItemforCVS(fe) + "\n");
			}
		}

		try {

			FileUtils.writeStringToFile(new File("/tmp/VisualFitsBrowser_Logfile.txt"),
					CSV.toString(), (Charset) null);

		} catch (IOException e) {
			myLogger.error("Error while writing csv table to File ", e);
		}
		return sb.toString();
	}

	private static String generateFileItem(FitsFileEntry fe) {
		StringBuilder sb = new StringBuilder();

		if (fe != null) {
			String s = "\\multirow{2}{*}{\\large "
					+ EscapeForLatex(fe.FName)
					+ " }& " //
					+ EscapeForLatex(fe.ObjName)
					+ " & "//
					+ "\\multirow{2}{*}{\\large "
					+ EscapeForLatex(fe.ExpTime + "")
					+ "} & " //
					+ "\\multirow{2}{*}{"
					+ EscapeForLatex(fe.Filter)
					+ "} & "//
					+ "\\multirow{2}{*}{"
					+ EscapeForLatex(formatTime(fe.DateObs))
					+ "} & " //
					+ String.format("% 6.1f", fe.Focus)
					+ " & " //
					+ "\\multirow{2}{*}{ \\vbox{" + EscapeForLatex(fe.UserComment)
					+ "}} \\\\*";

			sb.append(s);
			s = " & " //
					+ "{ \\small "
					+ EscapeForLatex(fe.RA_String + " " + fe.Dec_String)
					+ "} & " //
					+ "  & & & "
					+ String.format("% 5.2f", fe.Airmass)
					+ " &\\\\ \\hline \n\n";
			sb.append(s);
		}

		return sb.toString();

	}

	private static String generateFileItemforCVS(FitsFileEntry fe) {
		StringBuilder sb = new StringBuilder();

		if (fe != null) {
			String s = "" + EscapeForLatex(fe.FName)
					+ " & " //
					+ EscapeForLatex(fe.ObjName) + " & " + EscapeForLatex(fe.RA_String)
					+ " & " + EscapeForLatex(fe.Dec_String) + " & "//
					+ EscapeForLatex(fe.ExpTime + "") + " & " //
					+ EscapeForLatex(fe.Filter) + " & "//
					+ EscapeForLatex(formatTime(fe.DateObs)) + " & " //
					+ EscapeForLatex(fe.UserComment);

			sb.append(s);

		}

		return sb.toString();

	}

	private static String generateLatexHeader(String title) {
		String retVal = null;

		try {
			InputStream in = Filelist2Latex.class.getClassLoader()
					.getResourceAsStream("latexlog/LatexLog_header.tex");

			retVal = IOUtils.toString(in, (Charset) null);
		} catch (Exception e) {
			myLogger.error("Error while reading latex header: ", e);
		}

		title = EscapeForLatex(title);
        if (retVal != null) {
            retVal = retVal.replace("$TITLE$", title);
        }

        return retVal;

	}

	private static String generatelatexFooter() {
		String retVal = null;

		try {
			InputStream in = Filelist2Latex.class.getClassLoader()
					.getResourceAsStream("latexlog/LatexLog_footer.tex");

			retVal = IOUtils.toString(in, (Charset) null);
		} catch (Exception e) {
			myLogger.error("Error while reading latex footer: ", e);
		}
		return retVal;
	}

	/** Time of day of an observation for the log sheet, or an empty string if unknown. */
	static String formatTime(LocalDateTime t) {
		return t == null ? "" : mDateFormat.format(t);
	}

	/**
	 * Escape text so that it is printed literally by LaTeX, e.g. user comments and file names.
	 */
	static String EscapeForLatex(String s) {
		if (s == null)
			return "";

		StringBuilder sb = new StringBuilder(s.length() + 16);
		for (char c : s.toCharArray()) {
			switch (c) {
				case '\\':
					sb.append("\\textbackslash{}");
					break;
				case '{':
				case '}':
				case '#':
				case '$':
				case '%':
				case '&':
				case '_':
					sb.append('\\').append(c);
					break;
				case '^':
					sb.append("\\textasciicircum{}");
					break;
				case '~':
					sb.append("\\textasciitilde{}");
					break;
				case '<':
					sb.append("\\textless{}");
					break;
				case '>':
					sb.append("\\textgreater{}");
					break;
				default:
					sb.append(c);
			}
		}
		return sb.toString();
	}

	public static void main(String args[]) {
		Preferences.initPreferences("VisualFitsBrowserApp");
		Vector<FitsFileEntry> fileList = new Vector<FitsFileEntry>();

		fileList.add(new FitsFileEntry("", "filename", "Obj $ % & _ # ", null,
				3.2f, "G\'", LocalDateTime.now(), false));

		writeFileList2Latex("This is a title ", fileList, "/tmp/pODILogfile.tex");

		int ret = processLatex("/tmp/", "pODILogfile.tex");
		openLatexPDF("/tmp/pODILogfile.pdf");

	}

	/**
	 * Run pdflatex on a LaTeX file.
	 *
	 * @param Path  working directory; output files are written there.
	 * @param fname LaTeX file, relative to Path or absolute.
	 * @return pdflatex exit code, or -1 if it could not be run.
	 */
	public static int processLatex(String Path, String fname) {

		String pdfLatex = findPdfLatex();
		if (pdfLatex == null) {
			myLogger.error(PDFLATEX_NOT_FOUND);
			return -1;
		}

		return ProcessRunner.run(ProcessRunner.command(pdfLatex, "-interaction=nonstopmode", fname),
				new File(Path), myLogger);
	}

	static final String PROP_PDFLATEX = "org.cowjumping.VisualFitsBrowser.latex.pdflatex";

	static final String PDFLATEX_NOT_FOUND = "pdflatex was not found. Install a TeX distribution (TeX Live, "
			+ "MacTeX), or set " + PROP_PDFLATEX + " in ~/.VisualFitsBrowserApp to the full path of pdflatex.";

	/**
	 * Directories searched for pdflatex after PATH. Programs started from the macOS Finder or Dock do not see
	 * the shell's PATH, so the usual TeX installation directories are listed explicitly.
	 */
	static List<File> pdfLatexSearchDirs() {
		List<File> dirs = new ArrayList<File>();
		dirs.add(new File("/Library/TeX/texbin"));   // MacTeX
		dirs.add(new File("/opt/homebrew/bin"));     // Homebrew on Apple silicon
		dirs.add(new File("/usr/local/bin"));        // Homebrew on Intel, manual installs
		dirs.add(new File("/opt/local/bin"));        // MacPorts
		dirs.add(new File("/usr/bin"));              // Linux distribution packages

		// TeX Live installed from upstream: /usr/local/texlive/<year>/bin/<platform>, newest year first.
		File[] years = new File("/usr/local/texlive").listFiles();
		if (years != null) {
			Arrays.sort(years, Collections.reverseOrder());
			for (File year : years) {
				File[] platforms = new File(year, "bin").listFiles();
				if (platforms != null)
					dirs.addAll(Arrays.asList(platforms));
			}
		}
		return dirs;
	}

	/**
	 * The pdflatex program to use: the configured one if it exists, otherwise pdflatex found on PATH or in
	 * {@link #pdfLatexSearchDirs()}. The configuration is not changed.
	 *
	 * @return program (path, possibly followed by options), or null if no pdflatex was found.
	 */
	static String findPdfLatex() {
		String configured = Preferences.thePreferences.getProperty(PROP_PDFLATEX, "/usr/bin/pdflatex");
		String found = ProcessRunner.findExecutable(configured, "pdflatex", pdfLatexSearchDirs());
		if (found != null && !found.equals(configured))
			myLogger.warn("Configured pdflatex '" + configured + "' not found, using " + found);
		return found;
	}

	/**
	 * Open a PDF file in the configured viewer. Does not wait for the viewer to close.
	 */
	public static void openLatexPDF(String fname) {

		String OpenPDF = Preferences.thePreferences.getProperty(
				"org.cowjumping.VisualFitsBrowser.latex.openpdf", "/usr/bin/okular");

		ProcessRunner.launch(ProcessRunner.command(OpenPDF, fname), null, myLogger);
	}

	/**
	 * Generate a menu item that can be inserted into an application's menu.
	 *
	 * @param p
	 * @return
	 */
	public static JMenuItem getPDFLogFileMenuItem(FileBrowserPanel p) {
		myFileBrowserPanel = p;

		final JMenuItem item = new JMenuItem("Generate PDF logfile");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				myLogger.info("Invoking Generate PDF");

				if (myFileBrowserPanel == null || myFileBrowserPanel.mRootDirectory == null) {
					myLogger.warn("No filebrowser panel set for logfile conversion");
					return;
				}

				// Take a snapshot on the event thread; the list may change while the PDF is being built.
				final Vector<FitsFileEntry> fileList = new Vector<FitsFileEntry>(myFileBrowserPanel.mImageList);
				final File rootDirectory = myFileBrowserPanel.mRootDirectory;
				final String tempDir = Preferences.thePreferences.getProperty("VisualFitsBrowser.latex.tmp", "/tmp");

				item.setEnabled(false);

				new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
						String title = rootDirectory.getAbsolutePath();
						String LatexFname = rootDirectory.getName() + ".tex";

						if (findPdfLatex() == null)
							throw new IOException(PDFLATEX_NOT_FOUND);

						writeFileList2Latex(title, fileList, tempDir + "/" + LatexFname);
						processLatex(tempDir, tempDir + "/" + LatexFname);
						processLatex(tempDir, tempDir + "/" + LatexFname);
						processLatex(tempDir, tempDir + "/" + LatexFname);
						openLatexPDF(tempDir + "/" + LatexFname.replace(".tex", ".pdf"));
						return null;
					}

					@Override
					protected void done() {
						item.setEnabled(true);
						try {
							get();
						} catch (Exception e1) {
							Throwable cause = e1.getCause() != null ? e1.getCause() : e1;
							myLogger.error("Error while creating pdf log sheet", cause);
							JOptionPane.showMessageDialog(myFileBrowserPanel.getParent(),
									"Error while creating pdf log sheet:\n\n" + cause.getMessage());
						}
					}

				}.execute();
			}
		});


		return item;

	}
}

class myComp implements Comparator<FitsFileEntry> {


	public int compare(FitsFileEntry o1, FitsFileEntry o2) {
		return (int) (o1.DateObs.compareTo (o2.DateObs));

	}

}
