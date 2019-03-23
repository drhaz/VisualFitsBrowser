package org.cowjumping.VisualFitsBrowser.util.FitsComments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

public class FITSTextCommentImpl implements FitsCommentInterface {

	private final static Logger log = Logger.getLogger(FITSTextCommentImpl.class);

	private Properties readProperties(FitsFileEntry entry) {
		Properties myProps = new Properties();
		File ps = new File(entry.RootPath + "/.fitscomments");

		if (ps.exists()) {
			try {
				myProps.load(new FileInputStream(ps));
			} catch (Exception e) {
				log.error("Error while reading in comment file " + ps.getAbsolutePath(), e);
			}

		}
		return myProps;

	}

	
	public boolean readComment(FitsFileEntry e) {

		if (e == null)
			return false;
		Properties prop = readProperties(e);
		if (prop != null) {
			e.UserComment = prop.getProperty(e.FName, "");
		}
		return true;

	}

	public boolean writeComment(FitsFileEntry entry) {

		Properties myProps = readProperties(entry);
		myProps.setProperty(entry.FName, entry.UserComment);

		File ps = new File(entry.RootPath + "/.fitscomments");
		try {
			myProps.store(new FileOutputStream(ps), "FITS FILE COMMENTS BELOW");
		} catch (Exception e) {
			log.error("Error while writing fits comemnts: ", e);
			return (false);
		}
		return(true);
	}
}
