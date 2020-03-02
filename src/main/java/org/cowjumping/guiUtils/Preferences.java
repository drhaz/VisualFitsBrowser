package org.cowjumping.guiUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;


/**
 * A wrapper class around the java Properties class for use in OTAListener
 * <p>
 * This class internally keeps a Property list to store properties. If the class
 * is initialized without a filename the properties are loaded from
 * ~/.OTALIsterner.rc. Upon termination of this class (e.g., when the program is
 * finished), the properties will be written back to the file they were read
 * from.
 * <p>
 * A possible extension of this class is to provide a user interface to
 * interactively change properties, or to reload them at run time.
 */

public class Preferences {

	final static private Logger myLogger = LogManager.getLogger();
	private String FileName;

	private Properties myProperties = null;

	public static Preferences thePreferences = null;

	public static void initPreferences(String fname) {
		if (thePreferences == null) {
			thePreferences = new Preferences(fname);
		} else {
			myLogger.error("Preferences are already initialized");
		}
	}

	private Preferences(String fname) {

		if (thePreferences != null) {
			myLogger.warn("Preferences is already instanciated!");
			return;

		}
		FileName = fname;
		if (FileName == null)
			FileName = new String(System.getProperty("user.home")
					+ "/.OTAListener.rc");
		else {
			FileName = new String(System.getProperty("user.home") + "/."
					+ fname);
		}

		reInit();

		// Now comes the trick: we allow only one property ting per application!
		// But after that, one can statically access the just created instance.
		thePreferences = this;
	}

	protected void finalize() {
		if (FileName != null)
			saveProperties(FileName);
	}

	/**
	 * Reload the properties
	 */
	public void reInit() {

		myProperties = new Properties();
		loadProperties(FileName);

	}

	public String getProperty(String key, String defaultValue) {
		if (thePreferences != null) {

			String retVal = thePreferences.priv_getProperty(key, defaultValue);
			if ( (retVal != null) && retVal.equals(defaultValue))
				thePreferences.setProperty(key, defaultValue);
			return retVal;
		} else {

			myLogger.error("getPropoerty: Preferences not  initialized.");
		}
		return (defaultValue);
	}

	private String priv_getProperty(String key, String defaultValue) {

		String retval = myProperties.getProperty(key);

		if (retval == null && defaultValue != null) {

			myProperties.setProperty(key, defaultValue);
			retval = defaultValue;
		}
		return retval;

	}

	public void setProperty(String key, String value) {
		if (thePreferences != null)
			myProperties.setProperty(key, value);
		else
			myLogger.warn("Preferences system is not initialized! Not setting property: "
					+ key + ":" + value);

	}

	public void storeWindowLocation(JFrame frame, String PropertyRootName) {

		if (frame == null)
			return;
		Point d = frame.getLocation();
		Preferences.thePreferences.setProperty(PropertyRootName + ".x", d.x
				+ "");
		Preferences.thePreferences.setProperty(PropertyRootName + ".y", d.y
				+ "");

		boolean isVisible = frame.isVisible();
		Preferences.thePreferences.setProperty(PropertyRootName + ".visible",
				isVisible + "");

	}

	public void restoreWindowLocation(JFrame frame, String PropertyRootName) {

		if (frame == null)
			return;
		int wx = Integer.parseInt(Preferences.thePreferences.getProperty(
				PropertyRootName + ".x", "1"));
		int wy = Integer.parseInt(Preferences.thePreferences.getProperty(
				PropertyRootName + ".y", "1"));
		frame.setLocation(wx, wy);
		boolean b = Boolean.parseBoolean(Preferences.thePreferences
				.getProperty(PropertyRootName + ".visible", "true"));
		frame.setVisible(b);
	}

	protected void loadProperties(String fname) {

		if (myProperties == null) {

			myLogger.error("Cannot open Properties file:  properties not initialised");
			return;
		}

		File F = new File(fname);
		if (F.exists()) {
			try {
				FileInputStream in = new FileInputStream(fname);
				myProperties.load(in);
				in.close();

			} catch (Exception e) {

				myLogger.info("Could not open properties file. Using default configuration.\n");

			}
		} else {

			myLogger.warn("No existing .rc file found");
		}

	}

	protected void saveProperties(String fname) {

		myLogger.debug("Saving Properties to: " + fname);
		try {

			FileOutputStream out = new FileOutputStream(fname);
			myProperties
					.store(out,
							"This is a maschine generated configuration file. Do not delete. Edit only if you know what you are doing.");
			out.flush();
			out.close();
		} catch (Exception e) {

			myLogger.error("Cannot save Properties to file " + fname, e);
		}
	}

	public void save() {
		if (this.FileName != null)
			this.saveProperties(FileName);

	}
}
