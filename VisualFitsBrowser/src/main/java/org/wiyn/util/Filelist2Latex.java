package org.wiyn.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.wiyn.guiUtils.Preferences;

public class Filelist2Latex {

	private final static Logger myLogger = Logger.getLogger(Filelist2Latex.class);
	private final static SimpleDateFormat mDateFormat = new SimpleDateFormat("HH:mm:ss");

	public static String writeFileList2Latex(String Title, Vector<ODIFitsFileEntry> fileLis, String fname) {

		StringBuilder sb = new StringBuilder();

		sb.append(generateLatexHeader(Title));
		sb.append(generateFileTable(fileLis));
		sb.append(generatelatexFooter());

		if (myLogger.isDebugEnabled()) {
			myLogger.debug(sb.toString());
		}

		try {
			FileUtils.writeStringToFile(new File(fname), sb.toString());
		} catch (IOException e) {
			myLogger.error("Error while writing latex source to File ", e);
		}

		return sb.toString();

	}

	private static String generateFileTable(Vector<ODIFitsFileEntry> fileList) {

		StringBuilder sb = new StringBuilder();
		StringBuilder CSV = new StringBuilder();
		if (fileList != null) {

			List<ODIFitsFileEntry> sortList = new Vector<ODIFitsFileEntry>(fileList);
			Collections.sort(sortList, new myComp());
			for (ODIFitsFileEntry fe : sortList) {

				sb.append(generateFileItem(fe));
				CSV.append(generateFileItemforCVS(fe) + "\n");
			}
		}

		try {
			FileUtils.writeStringToFile(new File("/tmp/pODILogfile.txt"), CSV.toString());
		} catch (IOException e) {
			myLogger.error("Error while writing csv table to File ", e);
		}
		return sb.toString();
	}

	private static String generateFileItem(ODIFitsFileEntry fe) {
		StringBuilder sb = new StringBuilder();

		if (fe != null) {
			String s = "\\multirow{2}{*}{\\large " + EscapeForLatex(fe.FName) + " }& " //
					+ EscapeForLatex(fe.ObjName) + " & "//
					+ "\\multirow{2}{*}{\\large " + EscapeForLatex(fe.ExpTime + "") + "} & " //
					+ "\\multirow{2}{*}{" + EscapeForLatex(fe.Filter) + "} & "//
					+ "\\multirow{2}{*}{" + EscapeForLatex(mDateFormat.format(fe.DateObs)) + "} & " //
					+ String.format("% 6.1f", fe.Focus) + " & " //
					+ "\\multirow{2}{*}{ \\vbox{" + EscapeForLatex(fe.UserComment) + "}} \\\\*";

			sb.append(s);
			s = " & " //
					+ "{ \\small " + EscapeForLatex(fe.RA_String + " " + fe.Dec_String) + "} & " //
					+ "  & & & " + String.format("% 5.2f", fe.Airmass) + " &\\\\ \\hline \n";
			sb.append(s);
		}

		return sb.toString();

	}

	private static String generateFileItemforCVS(ODIFitsFileEntry fe) {
		StringBuilder sb = new StringBuilder();

		if (fe != null) {
			String s = "" + EscapeForLatex(fe.FName) + " & " //
					+ EscapeForLatex(fe.ObjName) + " & " + EscapeForLatex(fe.RA_String) + " & "
					+ EscapeForLatex(fe.Dec_String) + " & "//
					+ EscapeForLatex(fe.ExpTime + "") + " & " //
					+ EscapeForLatex(fe.Filter) + " & "//
					+ EscapeForLatex(mDateFormat.format(fe.DateObs)) + " & " //
					+ EscapeForLatex(fe.UserComment);

			sb.append(s);

		}

		return sb.toString();

	}

	private static String generateLatexHeader(String title) {
		String retVal = null;

		try {
			InputStream in = Filelist2Latex.class.getClassLoader()
					.getResourceAsStream("resources/pODILatexLog_header.tex");

			retVal = IOUtils.toString(in);
		} catch (Exception e) {
			myLogger.error("Error while reading latex header: ", e);
		}
		retVal = retVal.replace("$TITLE$", title);
		return retVal;

	}

	private static String generatelatexFooter() {
		String retVal = null;

		try {
			InputStream in = Filelist2Latex.class.getClassLoader()
					.getResourceAsStream("resources/pODILatexLog_footer.tex");

			retVal = IOUtils.toString(in);
		} catch (Exception e) {
			myLogger.error("Error while reading latex footer: ", e);
		}
		return retVal;
	}

	public static String EscapeForLatex(String s) {
		String ret = "";
		if (s == null)
			return ret;
		System.err.println("Escape input:" + s);
		String a = s.replaceAll("#", "\\\\#");
		ret = a.replaceAll("&", "\\\\&");
		ret = ret.replaceAll("\\$", "\\\\\\$");
		ret = ret.replaceAll("_", "\\\\_");
		ret = ret.replaceAll("%", "\\\\%");
		System.err.println("Escape return:" + ret);
		return ret;

	}

	public static void main(String args[]) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		Vector<ODIFitsFileEntry> fileList = new Vector<ODIFitsFileEntry>();

		fileList.add(new ODIFitsFileEntry("", "filename", "Obj $ % & _ # ", null, 3.2f, "G\'", new Date(), false));
		Logger.getRootLogger().setLevel(Level.DEBUG);
		writeFileList2Latex("This is a title ", fileList, "test");
		processLatex("/tmp/", "pODILogfile.tex");
		int ret = processLatex("/tmp/", "pODILogfile.tex");
		openLatex("/tmp/pODILogfile.pdf");

	}

	public static int processLatex(String Path, String fname) {

		Runtime rt = Runtime.getRuntime();
		Process proc = null;
		StringBuffer output = new StringBuffer();

		String pdfLatex = Preferences.thePreferences.getProperty("org.wiyn.odi.FileBrowser.latex.pdflatex",
				"/usr/local/texlive/2012/bin/x86_64-darwin/pdflatex");

		StringBuffer Command = new StringBuffer(pdfLatex);
		Command.append(" " + fname);

		try {
			myLogger.debug("Executing pdflatex: " + Command.toString());
			proc = rt.exec(Command.toString(), null, new File(Path));
			if (proc == null) {
				myLogger.error("Proceess for pdflatex returned null. Aborting");
				return -1;
			}

			BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			String sline = null;
			String eline = null;

			while ((sline = br.readLine()) != null || (eline = err.readLine()) != null) {
				if (output != null && sline != null)
					output.append(sline);
				if (eline != null)
					if (myLogger.isDebugEnabled())
						myLogger.debug(eline);
			}

			proc.waitFor();

		} catch (Exception e) {

			e.printStackTrace();
		}
		myLogger.debug("PDFLatex output\n " + output.toString());
		int exitVal = proc.exitValue();
		return exitVal;

	}

	public static int openLatex(String fname) {

		Runtime rt = Runtime.getRuntime();
		Process proc = null;
		StringBuffer output = new StringBuffer();

		String OpenPDF = Preferences.thePreferences.getProperty("org.wiyn.odi.FileBrowser.latex.openpdf", "open");

		StringBuffer Command = new StringBuffer(OpenPDF + " " + fname);

		try {
			myLogger.debug("Opening logfile: " + Command.toString());
			proc = rt.exec(Command.toString());
			if (proc == null) {
				myLogger.error("Proceess for pdflatex returned null. Aborting");
				return -1;
			}

			BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			String sline = null;
			String eline = null;

			while ((sline = br.readLine()) != null || (eline = err.readLine()) != null) {
				if (output != null && sline != null)
					output.append(sline);
				if (eline != null)
					if (myLogger.isDebugEnabled())
						myLogger.debug(eline);
			}

			proc.waitFor();

		} catch (Exception e) {

			e.printStackTrace();
		}
		myLogger.debug("open output\n " + output.toString());
		int exitVal = proc.exitValue();
		return exitVal;

	}

}

class myComp implements Comparator<ODIFitsFileEntry> {

	public int compare(ODIFitsFileEntry o1, ODIFitsFileEntry o2) {
		return (int) (o1.DateObs.getTime() - o2.DateObs.getTime());
	}

}
