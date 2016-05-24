package org.wiyn.odi.ODIFitsReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

/* makes a system call to xpaset
 * 
 */

public class XpaSetWrapper {

    private static final Logger myLogger = Logger
	    .getLogger (XpaSetWrapper.class.getCanonicalName ());

    private static final String XPA_FileName = "xpaset";
    private static final String OTAOV_FileName = "otaov";

    private static final String[] xpaLocationCandidates = { "/usr/bin",
	    "/usr/local/bin", "/opt/bin", "~/bin" };

    private static String xpaBinaryPath = findXPABinary ();

    private static String xpa_command_mosaic = "-p  ds9 file  mosaicimage iraf ";
    private static String xpa_command_single = "-p  ds9 file ";
    private static String otaov_command = "-t -m vertical";

    private static String findXPABinary () {

	File F = null;
	for (String path : xpaLocationCandidates) {
	    F = new File (path + "/" + XPA_FileName);
	    if (F.exists ()) {
		xpaBinaryPath = path + "/" + XPA_FileName;

		myLogger.info ("xpaset binary found at: " + xpaBinaryPath);

		return xpaBinaryPath;

	    }
	}
	myLogger.warn ("Could not find xpaset binary. No XPA servie available.");
	return null;

    }

    public static void xpaDisplaySingleImage (String filename) {
	if (xpaBinaryPath != null && filename != null) {

	    final String mFname = filename;

	    final StringBuffer Command = new StringBuffer (xpaBinaryPath);
	    if (mFname != null)
		Command.append (" " + xpa_command_single + mFname);

	    execXPACommand (Command);

	} else {
	    myLogger.error ("XPA binary file was not configured. No XPA has been called");
	}
    }

    public static void xpaDisplayImage (String filename) {

	if (filename == null) {
	    myLogger.error ("xpaDislayImage: filename is [null]. ");
	    return;
	}

	if (xpaBinaryPath != null) {

	    final String mFname = filename;

	    final StringBuffer Command = new StringBuffer (xpaBinaryPath);
	    if (mFname != null)
		Command.append (" " + xpa_command_mosaic + " " + mFname);

	    execXPACommand (Command);

	} else {
	    myLogger.error ("XPA binary file was not configured. No XPA has been called");
	}

    }

    private static void execXPACommand (final StringBuffer Command) {
	new Thread (new Runnable () {

	    public void run () {

		Runtime rt = Runtime.getRuntime ();
		Process proc = null;

		try {
		    myLogger.info ("Call xpaset " + Command.toString ());
		    proc = rt.exec (Command.toString (), null, null);

		    BufferedReader err = new BufferedReader (
			    new InputStreamReader (proc.getErrorStream ()));
		    BufferedReader br = new BufferedReader (
			    new InputStreamReader (proc.getInputStream ()));

		    String sline = null;
		    String eline = null;

		    while ((sline = br.readLine ()) != null
			    || (eline = err.readLine ()) != null) {

			if (myLogger.isDebugEnabled () && sline != null)
			    myLogger.debug ("xpa return: " + sline);

			if (eline != null)

			    myLogger.error ("xpa error: " + eline);
		    }

		    proc.waitFor ();

		} catch (Exception e) {

		    myLogger.error ("Error while executing xpaset: ", e);

		}
	    }
	}).start ();
    }

    public static void main (String[] args) {

	xpaDisplayImage ("/Volumes/snow/dharbeck/o4766g0173o/o4766g0173o33.fits");
    }
}
