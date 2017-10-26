package org.cowjumping.guiUtils;

import java.io.IOException;

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

/**
 * Tools to use via SAMP. This is mostly concerning interaction with ds9.
 * 
 * @author harbeck
 *
 */

public class SAMPUtilities {
	private final static Logger log = Logger.getLogger(SAMPUtilities.class);
	private static HubConnector sampHubConnector = null;
	private static Hub theHub = null;

	private static void initHubConnector(String name, String Descr, boolean runHub) {

		if (runHub) {
			try {
				theHub = Hub.runHub(HubServiceMode.MESSAGE_GUI);
			} catch (IOException e) {
				log.error("Error while starting SAMP hub. SAMP Services will not be available.\n    "
						+ e.getLocalizedMessage());
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

    /**
     *  Call this procedure when your application quits.
     *
     *  Do a controlled shutdown of the SAMP hub.
     */

	public static void onExit() {

		if (theHub != null) {
			theHub.shutdown();
		}
	}

	public synchronized static HubConnector getHubConnector() {

		if (sampHubConnector == null) {

			initHubConnector("no name", "uninitialized ODI Utilitis SAMP hub.", false);

		}

		return sampHubConnector;
	}



	public static void selectFrameDS9(int frameNumber) {
		log.debug("SAMP: Select image frame in ds9: " + frameNumber);
		try {

			Message m = new Message("ds9.set");
			m.addParam("cmd", "frame " + frameNumber);

			getHubConnector().getConnection().notifyAll(m);

		} catch (Exception e) {
			log.error("Could set ds9 frame number", e);
		}
	}

	public static void loadImageDS9(String fname, int frameNumber) {
		log.debug("SAMP: Loading fits image to ds9: " + fname);
        selectFrameDS9((frameNumber));

        try {

			Message m = new Message("ds9.set");
			String escapedFitsname = fname.replace (" ", "\\ ");
			m.addParam("cmd", "file fits " + escapedFitsname);

			getHubConnector().getConnection().notifyAll(m);

		} catch (Exception e) {
			log.error("Could not send fits file to ds9 via SAMP", e);
		}
	}


	public static void launchds9(String pathToBinary) {
		try {

				Runtime.getRuntime().exec(pathToBinary);

		} catch (IOException e) {

			log.error ("While launching ds9: ", e);

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

			log.error(e);
		}

		return retVal;

	}


	public static void getDS9Cursor() {
		log.debug("SAMP: Requesting ds9 cursor selection");
		if (!isClientAvailable("DS9")) {
			JOptionPane.showMessageDialog(null,
					"DS9 is not connected. DS9 conencts to SAMP only on startup, so if SAMP restarted, you need to restart ds9 as well.",
					"SAMP Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {

            HubConnector h = getHubConnector();
            String id = (String) h.getConnection().getSubscribedClients("ds9.get").keySet().iterator().next();


			if (h != null && h.getConnection() != null & id != null) {
                Message m = new Message("ds9.get");
                m.addParam("cmd", "iexam $filename $x $y $regions");

				h.getConnection().call(id,"donut", m);

			} else {
				SoundSignal.fail2();
				JOptionPane.showMessageDialog(null,
						"No SAMP hub is available. Please start a SAMP hub; the OTAListener will provide one.",
						"SAMP Error", JOptionPane.ERROR_MESSAGE);

			}

		} catch (Exception e) {
			log.error("While requesting cursor selection:", e);
		}
	}

	public static void loadMosaicDS9(String fname, int fno) {
		log.debug("SAMP: Loading Mosiac image to ds9: " + fname);

		if (!isClientAvailable("DS9")) {
			JOptionPane.showMessageDialog(null,
					"DS9 is not connected. DS9 conencts to SAMP only on startup, so if SAMP restarted, you need to restart ds9 as well.",
					"SAMP Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		SAMPUtilities.selectFrameDS9(fno);
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
			log.error("Could not get filename for xpa display call", e);
		}

	}


	public static void registercallBack(CallableClient client) {
		HubConnector h = getHubConnector();
		try {
			if (client != null && h != null)
				h.getConnection().setCallable(client);
		} catch (SampException e) {

			log.error("Error while registering SAMP callback", e);
		}
	}

	public static void main(String args[]) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		System.out.println(isClientAvailable("DS9"));
		System.exit(0);
	}

}
