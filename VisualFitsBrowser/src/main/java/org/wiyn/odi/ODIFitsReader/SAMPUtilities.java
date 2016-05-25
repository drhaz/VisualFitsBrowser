package org.wiyn.odi.ODIFitsReader;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.CallableClient;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;
import org.astrogrid.samp.xmlrpc.StandardClientProfile;
import org.wiyn.VisualFitsBrowser.util.ODIFitsFileEntry;
import org.wiyn.guiUtils.Preferences;
import org.wiyn.guiUtils.SoundSignal;

/**
 * Tools to talks via SAMP
 * 
 * @author harbeck
 *
 */

public class SAMPUtilities {
	final static Logger myLogger = Logger.getLogger(SAMPUtilities.class);
	public static boolean ready = false;
	private static HubConnector sampHubConnector = null;
	static Hub theHub = null;

	public static void initHubConnector(String name, String Descr, boolean runHub) {

		if (runHub) {
			try {
				theHub = Hub.runHub(HubServiceMode.MESSAGE_GUI);
				ready = true;
			} catch (IOException e) {

				myLogger.error("Error while starting SAMP hub. SAMP Servies will not be available.\n    "
						+ e.getLocalizedMessage());
				ready = false;
			}

		}

		ClientProfile profile = StandardClientProfile.getInstance();
		sampHubConnector = new HubConnector(profile);
		Metadata meta = new Metadata();
		meta.setName(name);
		meta.setDescriptionText(Descr);
		sampHubConnector.declareMetadata(meta);

		sampHubConnector.declareSubscriptions(sampHubConnector.computeSubscriptions());
		sampHubConnector.setAutoconnect(10);
	}

	public static void onExit() {
		if (theHub != null) {
			theHub.shutdown();
		}
	}

	public static HubConnector getHubConnector() {

		if (sampHubConnector == null) {

			initHubConnector("no name", "uninitialized ODI Utilitis SAMP hub.", false);

		}

		return sampHubConnector;
	}

	public static void sendImageToListener(String ImagePath) {
		File f = new File(ImagePath);
		if (ImagePath != null && f.exists() && f.isDirectory())
			try {
				Message m = new Message("odi.image.load");
				m.addParam("filename", ImagePath);

				SAMPUtilities.getHubConnector().getConnection().notifyAll(m);
				myLogger.debug("Sending image to OTAListener: " + ImagePath);

			} catch (Exception e) {
				myLogger.error("Could not execute SAMP call", e);
			}

	}

	/**
	 * Send a request to quickreduce to generate new master calib products based
	 * on the submitted list of files.
	 * 
	 * @param fList
	 */
	public static void sendCalibrationProductList(Vector<ODIFitsFileEntry> fList) {

		Message m = new Message("qr.mastercal");
		StringBuilder sb = new StringBuilder();

		for (ODIFitsFileEntry entry : fList) {
			String file = entry.getAbsolutePath();
			File f = new File(file);
			if (f.exists()) {
				if (sb.length() > 0)
					sb.append(",");

				sb.append(file);
			}
		}

		m.addParam("filelist", sb.toString());

		try {
			SAMPUtilities.getHubConnector().getConnection().notifyAll(m);
			myLogger.debug("Sending calibration list to SAMP: " + sb);
		} catch (Exception e) {
			myLogger.error("Error while sending samp message " + m, e);
		}
	}

	public static void broadcastODINonSiderealGuideRate(double rateX, double rateY) {

		Message m = new Message("odi.tracking.nonsidereal.rate");
		m.addParam("rateX", rateX + "");
		m.addParam("rateY", rateY + "");
		try {
			SAMPUtilities.getHubConnector().getConnection().notifyAll(m);
		} catch (SampException e) {
			myLogger.error("Error while sending non-sidereal guide rate: " + rateX + " " + rateY, e);
		}

	}

	public static void broadcastProgramID(String programID) {
		myLogger.debug("boradcasting Proposal ID  " + programID);
		try {

			Message m = new Message("odi.broadCastProposalID");

			m.addParam("PROPID", programID);
			SAMPUtilities.getHubConnector().getConnection().notifyAll(m);

		} catch (Exception e) {
			myLogger.error("Error while boradcasting proposal ID: " + programID);

		}
	}

	public static void broadcastListenerDisplayedImage(String fname) {
		try {

			Message m = new Message("odi.otalistener.displayedImage");

			m.addParam("fname", fname);
			SAMPUtilities.getHubConnector().getConnection().notifyAll(m);

		} catch (Exception e) {
			myLogger.error("Error while boradcasting displayed file name: " + fname);

		}
	}

	public static void selectFrameDS9(int frameNumber) {
		myLogger.debug("SAMP: Select image frame in ds9: " + frameNumber);
		try {

			Message m = new Message("ds9.set");
			m.addParam("cmd", "frame " + frameNumber);

			getHubConnector().getConnection().notifyAll(m);

		} catch (Exception e) {
			myLogger.error("Could set ds9 frame number", e);
		}
	}

	public static void loadImageDS9(String fname, int frameNumber) {
		myLogger.debug("SAMP: Loading fits image to ds9: " + fname);

		selectFrameDS9((frameNumber));
		try {

			Message m = new Message("ds9.set");
			m.addParam("cmd", "file fits " + fname);

			getHubConnector().getConnection().notifyAll(m);

		} catch (Exception e) {
			myLogger.error("Could not send fits file to ds9 via SAMP", e);
		}
	}

	public static boolean isClientAvailable(String idContain) {
		boolean retVal = false;
		try {
			if (getHubConnector().getConnection() != null) {
				String clients[] = getHubConnector().getConnection().getRegisteredClients();

				for (String client : clients) {

					String clientName = getHubConnector().getConnection().getMetadata(client).getName();

					if (clientName != null) {

						if (clientName.contains(idContain))
							retVal = true;
					}
				}
			}
		} catch (SampException e) {

			myLogger.error(e);
		}

		return retVal;

	}

	public static void callQRStack(Vector<ODIFitsFileEntry> fileList, double track_ra, double track_dec, String xArgs) {

		StringBuilder sb = new StringBuilder();

		for (ODIFitsFileEntry entry : fileList) {
			if (sb.length() > 0)
				sb.append(",");
			String file = entry.getAbsolutePath();
			sb.append(file);
		}

		myLogger.debug("SAMP: CallQRStack " + sb);

		try {
			Message m = new Message("qr.stack");
			m.addParam("filelist", sb.toString());
			m.addParam("trackrate", track_ra + "," + track_dec);
			HubConnector h = getHubConnector();
			if (h != null && h.getConnection() != null) {
				h.getConnection().notifyAll(m);
			}
		} catch (Exception e) {
			myLogger.error("Error while sending QRStack message:", e);
		}

	}

	public static void loadMosaicDS9(String fname) {
		myLogger.debug("SAMP: Loading Mosiac image to ds9: " + fname);

		if (!isClientAvailable("DS9")) {
			JOptionPane.showMessageDialog(null,
					"DS9 is not connected. DS9 conencts to SAMP only on startup, so if SAMP restarted, you need to restart ds9 as well.",
					"SAMP Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {

			Message m = new Message("ds9.set");
			m.addParam("cmd", "file  mosaicimage iraf " + fname);
			HubConnector h = getHubConnector();
			if (h != null && h.getConnection() != null) {

				h.getConnection().notifyAll(m);

			} else {
				SoundSignal.fail2();
				JOptionPane.showMessageDialog(null,
						"No SAMP hub is available. Please start a SAMP hub; the OTAListener will provide one.",
						"SAMP Error", JOptionPane.ERROR_MESSAGE);

			}

		} catch (Exception e) {
			myLogger.error("Could not get filename for xpa display call", e);
		}

	}

	public static void loadmkPODIMef(String fname) {
		myLogger.debug("SAMP: Loading Mosiac image to ds9: " + fname);

		if (!isClientAvailable("IRAF")) {
			JOptionPane.showMessageDialog(null,
					"IRAF is not connected. Make sure you started vocl (not ecl), and you might want to enter \"samp on\" in vocl.",
					"SAMP Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			String fitsName = fname;
			int idx = fname.lastIndexOf("/");
			if (idx >= 0) {
				fitsName = fname.substring(idx);
			}
			Message m = new Message("client.cmd.exec");

			String irafOutDirectory = Preferences.thePreferences.getProperty("samp.iraf.outdir", "/tmp/");
			String cmd = "mkpodimef " + fname + " output=" + irafOutDirectory + fitsName
					+ "  overscan=yes adjust=none overrid=yes";
			myLogger.debug("Iraf callout string: " + cmd);
			m.addParam("cmd", cmd);
			m.addParam("msgID", "" + System.currentTimeMillis());
			HubConnector h = getHubConnector();
			if (h != null && h.getConnection() != null) {

				h.getConnection().notifyAll(m);

			} else {
				SoundSignal.fail2();
				JOptionPane.showMessageDialog(null,
						"No SAMP hub is available. Please start a SAMP hub; the OTAListener will provide one.",
						"SAMP Error", JOptionPane.ERROR_MESSAGE);

			}

		} catch (Exception e) {
			myLogger.error("Could not get filename for xpa display call", e);
		}

	}

	public static void registercallBack(CallableClient client) {
		HubConnector h = getHubConnector();
		try {
			if (client != null && h != null)
				h.getConnection().setCallable(client);
		} catch (SampException e) {

			myLogger.error("Error while registering SAMP callback", e);
		}
	}

	public static void main(String args[]) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		System.out.println(isClientAvailable("DS9"));
		loadmkPODIMef("/Volumes/odifile/archive/podi/test/2012.06.14/d20120614T101044.2");
		System.exit(0);
	}

}
