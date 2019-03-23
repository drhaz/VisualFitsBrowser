package org.cowjumping.VisualFitsBrowser.util;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.ProgressMonitor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.cowjumping.VisualFitsBrowser.util.FitsComments.FITSTextCommentImpl;
import org.cowjumping.VisualFitsBrowser.util.FitsComments.FitsCommentInterface;
import org.cowjumping.FitsUtils.OBSTYPE;
import org.cowjumping.FitsUtils.QuickHeaderInfo;

/**
 * Data holding class for an ODI Image.
 *
 * @author harbeck
 */

public class FitsFileEntry {

	private final static Logger myLogger = Logger.getLogger(FitsFileEntry.class);


	public enum TRANSFERSTATUS {
		INDEF, LOCAL, NOTIFIED_DTS, CONFIRMED_DTS, NOTIFIED_PPA, CONFIRMED_PPA, ERROR

	}

	public boolean mef = false;

	private static FitsCommentInterface theCommentInterface = new FITSTextCommentImpl();

	/**
	 * A predefined filer for ODI fits directories
	 */

	public static FileFilter theODIFileFilter1 = new FileFilter() {
		public boolean accept(File file) {

			boolean good_file = false;
			good_file = file.isDirectory() && //
					// || //
					(file.getName().matches("[dftbo]20.*[T-].*") || //
							file.getName().matches("focus20.*T.*"));
			if (!good_file) {
				myLogger.debug("Reject file " + file.getAbsolutePath());
			}
			return good_file;
		}
	};

	/**
	 * A filter generic fits filter
	 */

	public static FileFilter theFITSFileFilter = new FileFilter() {
		public boolean accept(File file) {

			boolean good_file = false;
			good_file = file.exists() && //
					// || //
					(file.getName().matches(".*\\.fits$") || file.getName().matches(".*\\.fits\\.fz$"));

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
	public Date DateObs;
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
		return createFromFile(f, null);
	}

	public static FitsFileEntry createFromFile(File f, String extraKey) {

		FitsFileEntry entry = null;
		OBSTYPE ot = OBSTYPE.INDEF;
		Float expTime = Float.NaN;

		String ObjName = "n/a";
		Date DateObs = null;
		String Filter = "n/a";
		int ponTime = 0;

//		if (!(ArchiveMode || !ODIMode)) {
//			if (!f.exists() || !new File(f.getAbsoluteFile() + "/temp/.finished").exists()) {
//				myLogger.debug("rejecting pending file " + f.getAbsolutePath());
//				return null;
//			}
//		} else {
//			myLogger.debug("Accepting image unfinished image candidate since in archive | noODI mode");
//		}

		if (f.exists()) {

			Vector<String> fitsHeader = QuickHeaderInfo.readFITSHeader(f);
			if (fitsHeader != null && fitsHeader.size() > 0) {

				expTime = QuickHeaderInfo.getExpTime(fitsHeader);
				ObjName = QuickHeaderInfo.getObject(fitsHeader);
				Filter = QuickHeaderInfo.getFilter(fitsHeader);
				ponTime = QuickHeaderInfo.getPONTime(fitsHeader);
				DateObs = QuickHeaderInfo.getDateObs(fitsHeader, true);

			} else {
				myLogger.error("Fitsheader for file " + f.getName() + " is empty.");
				ObjName = "[invalid readout]";
			}

			entry = new FitsFileEntry(f.getParent(), //
					f.getName(), ObjName, ot, //
					expTime, //
					Filter, //
					DateObs != null ? DateObs : new Date(), //
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

			// fetch user comment
			FitsFileEntry.theCommentInterface.readComment(entry);


		}

		return entry;

	}

	public String toString (){
		StringBuilder sb = new StringBuilder();
		sb.append ("FileName: %s".format (this.FName));

		return sb.toString();
	}

	/**
	 * Write back the meta information for an image.
	 * <p>
	 * For this ODI implementation, the comment is added to the ./metainf.xml
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
						 String Filter, Date dateObs, boolean selected) {
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

	public static Vector<FitsFileEntry> getImagesInDirectory(File RootDirectory, ProgressMonitor progressM) {
		Vector<FitsFileEntry> directoryImages = new Vector<FitsFileEntry>();

		File files[] = RootDirectory.listFiles(thefileFilter);

		if (files != null) {
			if (progressM != null)
				progressM.setMaximum(files.length);
			myLogger.info("| Found  " + files.length + " entries");
			int progress = 0;

			{

				for (File file : files) {

					if (myLogger.isDebugEnabled()) {

						myLogger.debug("Checking out file: " + file.getAbsolutePath());
					}

					FitsFileEntry e = FitsFileEntry.createFromFile(file);

					if (e != null) {
						myLogger.debug("Adding file: " + file.getAbsolutePath());
						directoryImages.add(e);

					} else {

						if (myLogger.isDebugEnabled())
							myLogger.debug("File " + file.getAbsolutePath() + " has been rejected.");

					}

					progress++;

					if (progressM != null)
						progressM.setProgress(progress);

				}
			}

		} else {

			myLogger.error("No images found, returning empty lists.");

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
