package org.cowjumping.VisualFitsBrowser.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cowjumping.FitsUtils.OBSTYPE;
import org.cowjumping.FitsUtils.QuickHeaderInfo;
import org.cowjumping.VisualFitsBrowser.util.FitsComments.FITSTextCommentSQLITEImp;
import org.cowjumping.VisualFitsBrowser.util.FitsComments.FitsCommentInterface;
import org.cowjumping.guiUtils.Preferences;

import javax.swing.*;
import java.io.File;
import java.io.FileFilter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data holding class for a FITS image.
 *
 * @author harbeck
 */

public class FitsFileEntry {

	private final static Logger myLogger = LogManager.getLogger(FitsFileEntry.class);


	public enum TRANSFERSTATUS {
		INDEF, LOCAL, NOTIFIED_DTS, CONFIRMED_DTS, NOTIFIED_PPA, CONFIRMED_PPA, ERROR

	}

	public boolean mef = false;

	private static String commentdbfilename = new String(System.getProperty("user.home",".") + "/."
			+ "VisualFileBrowser.usercomments");
	private static FitsCommentInterface theCommentInterface = new FITSTextCommentSQLITEImp(commentdbfilename);


	/**
	 * Number of files whose headers are read at the same time. On network file systems such as S3 mounts each read
	 * waits for a remote request, so reading in parallel hides most of that latency.
	 */
	private final static String PROP_READTHREADS = FitsFileEntry.class.getCanonicalName() + ".READTHREADS";
	private final static int DEFAULT_READTHREADS = 16;

	public static void onExit () {
		if (theCommentInterface != null)
			theCommentInterface.close();

	}
	/**
	 * A filter generic fits filter
	 */

	public static FileFilter theFITSFileFilter = new FileFilter() {
		public boolean accept(File file) {

			boolean good_file = false;
			// No file.exists() check: the files come from a directory listing, and on network file systems every
			// check is a remote request.
			good_file = file.getName().matches(".*\\.fits?$") || file.getName().matches(".*\\.fits\\.fz$");

			if (!good_file) {
				//myLogger.debug("Reject file " + file.getAbsolutePath());
			}
			return good_file;
		}
	};

	/**
	 * A filter specialized for the NOAO Mosaic camera
	 */

	public static FileFilter theMosaicFITSFileFilter1 = new FileFilter() {
		public boolean accept(File file) {

			boolean good_file = false;
			good_file = file.exists() && //
					// || //
					(file.getName().matches("[dftbo].*\\.fits*"));

			if (!good_file) {
				myLogger.debug("Reject file " + file.getAbsolutePath());
			}
			return good_file;
		}
	};

	/**
	 * The default file filter which identifies astronomical image candidates in
	 * a given directory. There is no assumtopn made if those should be an
	 * directory or flat files.
	 */
    static FileFilter thefileFilter = theFITSFileFilter;

	public File DirectoryFile;
	public String RootPath;
	public String ObjName;
	public String Filter;
	public Float ExpTime;
	public float Airmass;
	public float Focus;
	public LocalDateTime DateObs;
	public Boolean Selected;
	public boolean isBinned = false;
	public String FName;
	public String UserComment = "";
	public OBSTYPE ObsType = OBSTYPE.INDEF;
	public Boolean hasVideo = Boolean.FALSE;
	public Integer PONTime = 0;
	public String ExtraKeyword;
	public String RA_String;
	public String Dec_String;
//	public TRANSFERSTATUS TransferStatus = TRANSFERSTATUS.INDEF;

	public boolean xmlSaved = true;

	public boolean isValidReadout = true;

	public static FitsFileEntry createFromFile(File f) {
		return createFromFile(f, "CONFMODE");
	}

	/** @return the entry for the file, or null if the file does not exist. */
	public static FitsFileEntry createFromFile(File f, String extraKey) {
		if (!f.exists())
			return null;
		FitsFileEntry entry = readFromFile(f, extraKey);
		FitsFileEntry.theCommentInterface.readComment(entry);
		return entry;
	}

	/**
	 * Create an entry from the file's FITS header, without the user comment. Does not check that the file exists.
	 * Safe to call from several threads at once.
	 */
	private static FitsFileEntry readFromFile(File f, String extraKey) {

		FitsFileEntry entry = null;
		OBSTYPE ot = OBSTYPE.INDEF;
		Float expTime = Float.NaN;

		String ObjName = "n/a";
		LocalDateTime DateObs = null;
		String Filter = "n/a";

		Vector<String> fitsHeader = QuickHeaderInfo.readFITSHeader(f);
		if (fitsHeader != null && fitsHeader.size() > 0) {

			expTime = QuickHeaderInfo.getExpTime(fitsHeader);
			ObjName = QuickHeaderInfo.getObject(fitsHeader);
			Filter = QuickHeaderInfo.getFilter(fitsHeader);
			DateObs = QuickHeaderInfo.getDateObs(fitsHeader, true);

		} else {
			// Only checked after a failed read, to avoid an extra request per file on network file systems.
			if (!f.exists())
				return null;
			myLogger.error("Fitsheader for file " + f.getName() + " is empty.");
			ObjName = "[invalid readout]";
		}

		entry = new FitsFileEntry(f.getParent(), //
				f.getName(), ObjName, ot, //
				expTime, //
				Filter, //
				DateObs != null ? DateObs : LocalDateTime.now(), //
				false);

		entry.DirectoryFile = f;
		// Now fetching additional header information:

		if (fitsHeader != null) {
			myLogger.debug("Adding image: " + entry);
			entry.RA_String = QuickHeaderInfo.getStringValue(fitsHeader, "RA");
			entry.Dec_String = QuickHeaderInfo.getStringValue(fitsHeader, "DEC");
			entry.RA_String = entry.RA_String != null ? entry.RA_String.trim() : "INDEF";
			entry.Dec_String = entry.Dec_String != null ? entry.Dec_String.trim() : "INDEF";
			entry.Airmass = QuickHeaderInfo.getFloatValue(fitsHeader, "AIRMASS");
			entry.Focus = QuickHeaderInfo.getFloatValue(fitsHeader, "TELFOCUS");
			entry.isBinned = QuickHeaderInfo.isBinned(fitsHeader);
			entry.mef = QuickHeaderInfo.getBooleanValue (fitsHeader, "EXTEND");
			myLogger.debug("Added image: " + entry);
		}


		if (extraKey != null && fitsHeader != null) {
			String temp = QuickHeaderInfo.getStringValue(fitsHeader, extraKey);
			if (temp != null)
				entry.ExtraKeyword = temp;
			else
				entry.ExtraKeyword = "n/a";
		}

		return entry;

	}

	public String toString (){
		StringBuilder sb = new StringBuilder();
		sb.append ("Filename: " + this.FName);

		return sb.toString();
	}

	/**
	 * Write back the meta information for an image.
	 * <p>
	 * For this implementation, the comment is added to the ./metainf.xml
	 * file.
	 * <p>
	 * To provide another feedback mechanism, override this procedure.
	 */

	public void writeBackMetaInformation() {

		FitsFileEntry.theCommentInterface.writeComment(this);

	}

	public boolean isCalibration() {
		return (ObsType == OBSTYPE.BIAS || ObsType == OBSTYPE.DARK || ObsType == OBSTYPE.DOMEFLAT);
	}

	public FitsFileEntry(String rootPath, String fname, String ObjName, OBSTYPE obsType, Float expTime,
						 String Filter, LocalDateTime dateObs, boolean selected) {
		super();
		this.RootPath = rootPath;
		this.FName = fname;
		this.ObjName = ObjName;
		this.ObsType = obsType;
		this.ExpTime = expTime;
		this.DateObs = dateObs;
		this.Selected = selected;
		this.Filter = Filter;

	}

	private FitsFileEntry(String fname, String comment) {
		super();
		this.FName = fname;
		this.UserComment = comment;

	}

	public static FitsFileEntry createFromComment(String fname, String comment) {
		return new FitsFileEntry(fname,comment);
	}

	public String getAbsolutePath() {
		return RootPath + "/" + FName;
	}

	/**
	 * Read the headers of all FITS files in a directory. Typically called from a background thread; progress is
	 * reported to the (optional) ProgressMonitor on the Swing event thread.
	 * <p>
	 * The headers are read by several threads at once (see {@link #PROP_READTHREADS}); the user comments are then
	 * read one after the other, since the comment database is not meant for concurrent use.
	 */
	public static Vector<FitsFileEntry> getImagesInDirectory(File RootDirectory, final ProgressMonitor progressM) {
		Vector<FitsFileEntry> directoryImages = new Vector<FitsFileEntry>();

		File files[] = RootDirectory.listFiles(thefileFilter);

		if (files == null) {
			myLogger.error("No images found, returning empty lists.");
			return directoryImages;
		}

		final int nFiles = files.length;
		if (progressM != null)
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					progressM.setMaximum(nFiles);
				}
			});
		myLogger.info("| Found  " + files.length + " entries");
		if (nFiles == 0)
			return directoryImages;

		int nThreads = DEFAULT_READTHREADS;
		if (Preferences.thePreferences != null) {
			String value = Preferences.thePreferences.getProperty(PROP_READTHREADS, "" + DEFAULT_READTHREADS);
			try {
				nThreads = Integer.parseInt(value.trim());
			} catch (NumberFormatException e) {
				myLogger.warn("Invalid " + PROP_READTHREADS + " " + value + ", using " + DEFAULT_READTHREADS);
			}
		}
		nThreads = Math.max(1, Math.min(nThreads, nFiles));

		ExecutorService pool = Executors.newFixedThreadPool(nThreads, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "FITS header reader");
				t.setDaemon(true);
				return t;
			}
		});

		final AtomicInteger progress = new AtomicInteger();
		List<Future<FitsFileEntry>> results = new ArrayList<Future<FitsFileEntry>>(nFiles);
		for (final File file : files) {
			results.add(pool.submit(() -> {
				myLogger.debug("Checking out file: " + file.getAbsolutePath());
				FitsFileEntry e = readFromFile(file, "CONFMODE");

				final int done = progress.incrementAndGet();
				if (progressM != null)
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							progressM.setProgress(done);
						}
					});
				return e;
			}));
		}

		try {
			for (int ii = 0; ii < nFiles; ii++) {
				try {
					FitsFileEntry e = results.get(ii).get();
					if (e != null) {
						FitsFileEntry.theCommentInterface.readComment(e);
						directoryImages.add(e);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) {
					myLogger.error("While reading header of " + files[ii].getAbsolutePath(), e);
				}
			}
		} finally {
			pool.shutdownNow();
		}

		return directoryImages;

	}

//	public static JMenuItem getArchiveModemenuItem() {
//		JMenuItem item = new JCheckBoxMenuItem("Local archive Mode");
//
//		item.setSelected(FitsFileEntry.ArchiveMode);
//
//		item.addChangeListener(new ChangeListener() {
//
//			public void stateChanged(ChangeEvent evt) {
//				FitsFileEntry.ArchiveMode = ((JCheckBoxMenuItem) evt.getSource()).getState();
//			}
//
//		});
//
//		return item;
//	}

//	public String getAsPTICommentXMLElement() {
//		String retVal = "<Exposure>\n" //
//				+ "<EXPOSUREID>" + this.FName + "</EXPOSUREID>\n" //
//				+ "<USERCOMMENT>" + StringEscapeUtils.escapeXml(this.UserComment) + "</USERCOMMENT>\n" //
//				+ "</Exposure>\n";
//		return retVal;
//	}
}
