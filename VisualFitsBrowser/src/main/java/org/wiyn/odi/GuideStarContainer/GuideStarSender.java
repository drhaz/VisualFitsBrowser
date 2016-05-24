package org.wiyn.odi.GuideStarContainer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Class to send a GuideStarContainer object over a network connection
 * 
 * @author harbeck
 * 
 */
public class GuideStarSender {

	private final static Logger myLogger = Logger.getLogger(GuideStarSender.class);

	private boolean finish = false;
	private Socket mySocket = null;
	private OutputStream myOS = null;
	private String targetHost = null;
	private int targetPort = 0;

	final LinkedBlockingQueue<GuideStarContainer> myGSQueue;

	public GuideStarSender(String targetHost) {

		this(targetHost, GuideStarReceiver.DEFAULT_PORT);

	}

	public GuideStarSender(String targetHost, int targetPort) {

		this.targetHost = targetHost;
		this.targetPort = targetPort;
		myGSQueue = new LinkedBlockingQueue<GuideStarContainer>(15);

		initConnection();

		Thread t = new Thread(new Runnable() {

			public void run() {
				while (!finish) {
					try {
						GuideStarContainer gs = myGSQueue.take();
						if (gs != null)
							sendGuideStarStream(gs);
					} catch (InterruptedException e) {

						myLogger.error("Error while taking guidestar from sender queue:", e);
					}

				}
			}
		});
		t.start();
		t.setName("Guide Star send queue worker");

	}

	private void initConnection() {

		try {

			mySocket = new Socket(targetHost, targetPort);

		} catch (UnknownHostException e) {

			myLogger.warn("Could not find host: " + targetHost + ". Connection failed.");
			mySocket = null;

		} catch (Exception e) {

			myLogger.warn("Could not connect to host: " + targetHost + ". Connection failed (see error below)", e);
			mySocket = null;
			return;
		}

		try {

			mySocket.setKeepAlive(true);

		} catch (SocketException e) {

			myLogger.error("setAlive() failed on Socket " + mySocket, e);

		}

		try {
			this.myOS = mySocket.getOutputStream();
		} catch (Exception e) {
			myLogger.error("Error while trying to get OutputStream for TCP socket\n:", e);
		}
	}

	public void closeConnection() {

		try {
			if (myOS != null) {
				this.myOS.flush();
				this.myOS.close();
				myOS = null;
			}
			if (mySocket != null) {
				mySocket.close();
				mySocket = null;
			}

		} catch (IOException e) {
			myLogger.error("Error while closing my outputstream or socket. I wonder why?", e);
		}

	}

	public void queueGuideStarObject(GuideStarContainer gs) {

		if (!this.myGSQueue.offer(gs))
			myLogger.warn("Could not add guide star to queue: " + gs.getID() + "  " + gs.getCycle()
					+ " Start in queue : " + myGSQueue.size());

	}

	public static void dumpGuideStarCatalog(String fname, List<GuideStarContainer> gsL) {

		try {
			myLogger.debug("Saving Guide Star catalog to file: " + fname);
			FileOutputStream os = new FileOutputStream(fname);

			safeGuideStarStream(os, GuideStarContainer.getStartCatalogPackage(gsL.size()));
			for (GuideStarContainer gs : gsL) {
				safeGuideStarStream(os, gs);

			}
			safeGuideStarStream(os, GuideStarContainer.getEndCatalogPackage());
			os.flush();
			os.close();
		} catch (Exception e) {
			myLogger.error("Could not sage guide star cagtalog to file: " + fname, e);
		}
	}

	public static void safeGuideStarStream(OutputStream os, GuideStarContainer gs) {

		ByteBuffer bb = null;
		byte ba[] = null;
		try {

			os.write("GSContainer\n".getBytes());
			os.write(gs.Hash2String().getBytes());

			if (gs.getRawImageBuffer() != null) {

				os.write(("Image " + gs.getImageDimX() + " " + gs.getImageDimY() + "\n").getBytes());

				// Initialize the byte buffer if neccessary
				if (bb == null || ba == null || bb.capacity() < gs.getImageDimX() * gs.getImageDimY() * 2) {
					ba = new byte[gs.getImageDimX() * gs.getImageDimY() * 2];
					bb = ByteBuffer.wrap(ba);

				}
				bb.clear();
				gs.imageToByteBuffer(bb);
				os.write(ba, 0, gs.getImageDimX() * gs.getImageDimY() * 2);
				bb.clear();
				ba = null;

			} else {

				os.write("Image null\n".getBytes());
			}

		} catch (Exception e) {
			myLogger.error("Something went wrong while sending the guide star package: ", e);
		}
	}

	private void sendGuideStarStream(GuideStarContainer gs) {

		if (mySocket != null && mySocket.isConnected() && myOS != null && gs != null) {
			ByteBuffer bb = null;
			byte ba[] = null;
			try {

				myOS.write("GSContainer\n".getBytes());
				myOS.write(gs.Hash2String().getBytes());

				if (gs.getRawImageBuffer() != null) {

					myOS.write(("Image " + gs.getImageDimX() + " " + gs.getImageDimY() + "\n").getBytes());

					// Initialize the byte buffer if neccessary
					if (bb == null || ba == null || bb.capacity() < gs.getImageDimX() * gs.getImageDimY() * 2) {
						ba = new byte[gs.getImageDimX() * gs.getImageDimY() * 2];
						bb = ByteBuffer.wrap(ba);

					}
					bb.clear();
					gs.imageToByteBuffer(bb);
					myOS.write(ba, 0, gs.getImageDimX() * gs.getImageDimY() * 2);
					bb.clear();
					ba = null;

				} else {

					myOS.write("Image null\n".getBytes());
				}
				myOS.flush();
			} catch (Exception e) {
				myLogger.error("Something went wrong while sending the guide star package. Shutting down sender ", e);
				try {
					if (myOS != null)
						myOS.close();

					if (mySocket != null)
						mySocket.close();

				} catch (Exception e1) {

					myLogger.error("Error while shutting down defective output socket.", e1);
				} finally {
					myOS = null;
					mySocket = null;
				}

			}
		} else {
			myLogger.warn("Cannot send gudie star stream");
		}
	}

	/**
	 * Send special package to flag a new exposure
	 * 
	 * @param nGuideStars
	 */

	public void sendNewExposure(int nGuideStars) {

		GuideStarContainer gs = GuideStarContainer.getStartVideoPackage(nGuideStars);

		queueGuideStarObject(gs);

	}

	public void sendEndExposure() {

		GuideStarContainer gs = GuideStarContainer.getEndVideoPackage();
		queueGuideStarObject(gs);

	}

	public void sendNewCatalog(int nGuideStars) {

		GuideStarContainer gs = GuideStarContainer.getStartCatalogPackage(nGuideStars);

		queueGuideStarObject(gs);

	}

	public void sendEndCatalog() {

		GuideStarContainer gs = GuideStarContainer.getEndCatalogPackage();
		queueGuideStarObject(gs);

	}

	public boolean isAlive() {

		return (mySocket != null && mySocket.isConnected());
	}

	public void reset() {
		this.closeConnection();
		this.initConnection();
	}

	public void sendGuideStarcatalog(Vector<GuideStarContainer> myCatalog) {

		myLogger.debug("Sending out Guide star catalog with: " + myCatalog.size() + " entries.");
		sendGuideStarStream(GuideStarContainer.getStartCatalogPackage(myCatalog.size()));
		int n = 0;
		for (GuideStarContainer gs : myCatalog) {
			sendGuideStarStream(gs);

		}
		sendGuideStarStream(GuideStarContainer.getEndCatalogPackage());
		this.closeConnection();
	}

	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		GuideStarSender mySender = new GuideStarSender("localhost", 8090);

		GuideStarContainer gs = new GuideStarContainer();
		gs.setImage(new short[64 * 64], 64, 64);
		long start = System.currentTimeMillis();
		int ii;
		mySender.sendNewExposure(4);
		for (ii = 0; ii < 4; ii++) {
			gs.setCycle(ii);
			mySender.queueGuideStarObject(gs);
		}

		long end = System.currentTimeMillis();

		myLogger.debug(
				"Sent " + ii + " Guidestar Packages. It takes " + ((end - start) / (float) ii) + "ms per package\n");
	}
}
