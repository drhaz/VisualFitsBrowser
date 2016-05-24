package org.wiyn.util.FitsComments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.wiyn.util.ODIFitsFileEntry;

public class FITSTextCommentImpl implements FitsCommentInterface {

	private final static Logger log = Logger.getLogger(FITSTextCommentImpl.class);

	private Properties readProperties(ODIFitsFileEntry entry) {
		Properties myProps = new Properties();
		File ps = new File(entry.RootPath + "/.fitscomments");

		if (ps.exists()) {
			try {
				myProps.load(new FileInputStream(ps));
			} catch (Exception e) {
				log.error("Error whle reading in comment file " + ps.getAbsolutePath(), e);
			}

		}
		return myProps;

	}

	public void readComment(ODIFitsFileEntry e) {

		if (e == null)
			return;
		Properties prop = readProperties(e);
		if (prop != null) {
			e.UserComment = prop.getProperty(e.FName, "");
		}

	}

	public void writeComment(ODIFitsFileEntry entry) {

		Properties myProps = readProperties(entry);
		myProps.setProperty(entry.FName, entry.UserComment);

		File ps = new File(entry.RootPath + "/.fitscomments");
		try {
			myProps.store(new FileOutputStream(ps), "FITS FILE COMMENTS BELOW");
		} catch (Exception e) {
			log.error("Error while writing fits comemnts: ", e);
		}

	}
}
