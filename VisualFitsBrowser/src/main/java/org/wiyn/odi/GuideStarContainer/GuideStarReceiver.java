package org.wiyn.odi.GuideStarContainer;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JOptionPane;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Class to receive guide star packages from several senders
 * 
 * @author harbeck
 * 
 */

public class GuideStarReceiver implements Runnable {

	public final static int DEFAULT_PORT = 8090;
	public final static int TELESCOPEGUIDER_Port = DEFAULT_PORT + 1;

	int myPort = 0;

	protected Vector<GuideStarContainerListener> myListeners = new Vector<GuideStarContainerListener>();
	protected Vector<GuideStarReceiverStatusListener> myStatusListeners = new Vector<GuideStarReceiverStatusListener>();

	ServerSocket ServerSock = null;

	Vector<GuideStarClient> openConnections;
	@SuppressWarnings("unused")
	private GuideStarReceiver theGuideStarReceiver = null;
	final LinkedBlockingQueue<GuideStarContainer> myGSQueue = new LinkedBlockingQueue<GuideStarContainer>(
			10);
	private static Logger myLogger = Logger.getLogger(GuideStarReceiver.class);

	public GuideStarReceiver() {
		this(DEFAULT_PORT);
	}

	public int getGuideStarsInQueue() {
		return myGSQueue.size();

	}

	public GuideStarReceiver(int port) {
		super();
		theGuideStarReceiver = this;
		myPort = port;

		openConnections = new Vector<GuideStarClient>();
		try {

			ServerSock = new ServerSocket(myPort);

		} catch (java.net.BindException e) {

			String msg = "Cannot bind to video listener port.\n"
					+ ""
					+ "There is probably another instance of this application\n"
					+ "running on this computer, but only one instance is allowed.\n\n"
					+ "Please close all instances of this applications running here.\n"
					+ "I am quitting for now.";

			myLogger.error(msg, e);
			JOptionPane.showMessageDialog(null, msg, "OTA Listener Error",
					JOptionPane.ERROR_MESSAGE);

			System.exit(1);

		} catch (IOException e) {

			myLogger.error("Error while opening listening port: " + myPort, e);
			this.notifyStatusAll(GuideStarReceiverStatusListener.STATUS_ERROR_OPEN);

		}

		myLogger.info("Now waiting for incomming connections.");
		this.notifyStatusAll(GuideStarReceiverStatusListener.STATUS_WAITING);
		Thread t = new Thread(this);
		t.setName("GuideStarContainer.GuideStarReceiver TCP server port thread");
		t.start();

		// TODO: Implement way to gracefully end this thread with the master
		// thread.
		Thread distThread = new Thread(new Runnable() {

			
			public void run() {
				boolean valid = true;
				while (valid) {
					if (myGSQueue == null) {
						myLogger.fatal("Guide Star Receiver Guide Star Queue null. Must not be.");
						continue;
					}
					GuideStarContainer gs;
					try {
						gs = myGSQueue.take();
					} catch (InterruptedException e) {
						myLogger.error(
								"Error while reading from Guide Star Distribution Queue",
								e);
						// valid = false;
						continue;
					}

					if (gs != null)

						for (GuideStarContainerListener gsL : myListeners) {
							gsL.pushNewGuideStarContainer(new GuideStarContainer(
									gs, true));

						}
					gs = null;

				}

				myLogger.error("Guide Star Distribution thread ending, but it should always be active.");
			}

		});

		distThread.setName("Guide Star Distribution Thread");
		distThread.start();

	}

	public void close() {
		if (ServerSock != null) {
			try {
				ServerSock.close();
			} catch (IOException e) {

				myLogger.error("Error while closing server socket.");
			}
		}
	}

	public void addListener(GuideStarContainerListener l) {

		myListeners.add(l);
		myLogger.info("Number of listeners for guidestar container: "
				+ myListeners.size());

	}

	public void distributeGuideStar(GuideStarContainer gs) {
		for (GuideStarContainerListener gsL : myListeners) {
			gsL.pushNewGuideStarContainer(gs);

		}
	}

	public void addStatusListener(GuideStarReceiverStatusListener r) {
		myStatusListeners.add(r);
	}

	public void notifyStatusAll(int s) {
		for (GuideStarReceiverStatusListener r : myStatusListeners) {
			r.receiveGuideStarReceiverStatus(s);
		}
	}

	public static List<GuideStarContainer> readGSCatalogFromFile(String fname) {

		GuideStarClient c = new GuideStarClient(fname);

		if (c.inStream == null)
			return null;
		List<GuideStarContainer> gsV = new ArrayList<GuideStarContainer>();
		try {
			while (c.inStream.available() > 0) {
				GuideStarContainer gs = c.readGuideStarStream();
				if (gs == null || gs.isEndCatalogPackage())
					break;

				if (!gs.isStartCatalogPackage())
					gsV.add(gs);
			}
		} catch (Exception e) {

		}
		return gsV;

	}

	public void run() {

		Socket sock = null;
		while (true) {
			try {
				sock = this.ServerSock.accept();

			} catch (IOException e) {
				myLogger.error("Error while accepting incoming conenction: ", e);
				notifyStatusAll(GuideStarReceiverStatusListener.STATUS_ERROR_ACCEPT);
				continue;

			}

			myLogger.info("Accepted connection from " + sock.getInetAddress());
			GuideStarClient newClient = new GuideStarClient(this, sock);
			openConnections.add(newClient);
			if (newClient.init()) {
				Thread t = new Thread(newClient);
				t.setName("GuideStarReceiver connection thread."
						+ sock.getInetAddress());
				t.start();
				// newClient will stop itself when sock runs dry
			}
			notifyStatusAll(GuideStarReceiverStatusListener.STATUS_OPENCONNECTION);

		}
	}

	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		@SuppressWarnings("unused")
		GuideStarReceiver myRecevier = new GuideStarReceiver();

	}

	public void injectEOC() {
		this.distributeGuideStar(GuideStarContainer.getEndCatalogPackage());

	}

	public void injectGS(GuideStarContainer gs) {
		if (this.myGSQueue != null) {
			myGSQueue.offer(gs);
		}

	}

}

class GuideStarClient implements Runnable {

	Socket mySock;
	GuideStarReceiver myMaster;
	InputStream inStream;
	public long lostPackage = 0;
	public long receivedPackage = 0;
	private boolean shutdown = false;
	private static Logger myLogger = Logger.getLogger(GuideStarClient.class);

	public GuideStarClient(GuideStarReceiver myMaster, Socket sock) {

		this.myMaster = myMaster;
		mySock = sock;

	}

	protected GuideStarClient(String fname) {
		try {
			inStream = new FileInputStream(fname);
		} catch (FileNotFoundException e) {

			myLogger.error("Could not open inout file: " + fname);
			inStream = null;
		}
	}

	public void stop() {
		myLogger.debug("Shutting down GuideStarReceiver thread " + this);
		this.queueGuideStarforDistribution(GuideStarContainer
				.getEndConnectionPackage());
		if (inStream != null) {
			try {
				inStream.close();
			} catch (IOException e) {

				myLogger.error(
						"Guidestarclient.stop: Error while closing inputstream from tcp connection",
						e);
			}
		}

		try {
			myMaster.openConnections.remove(this);
			mySock.close();
			mySock = null;
		} catch (IOException e) {

			myLogger.error("Guidestarclient.stop: Error while closing socket: "
					+ mySock, e);
		}

		if (receivedPackage > 0 && lostPackage > 0) {
			myLogger.warn(String.format(
					"Guidestarclient.stop: Lost packages: %d / %d ",
					lostPackage, receivedPackage));
		}

	}

	public boolean init() {

		try {
			mySock.setKeepAlive(true);
		} catch (SocketException e) {
			myLogger.error("Error while configuring incoming socket.", e);
			return false;
		}

		try {
			// inStream = new BufferedInputStream((mySock.getInputStream()));
			inStream = ((mySock.getInputStream()));
		} catch (IOException e1) {
			myLogger.error("error while geting stream from Socket", e1);
			return false;
		}

		return true;
	}

	public static short checksumImage(GuideStarContainer gs) {
		long sum = 0;

		if (gs.getRawImageBuffer() != null) {
			for (int ii = 0; ii < gs.getRawImageBuffer().length; ii++)
				sum += gs.getRawImageBuffer()[ii];
		}
		sum = sum % Short.MAX_VALUE;
		return (short) sum;
	}

	public void run() {
		myLogger.info("Client thread started. Waiting for data.");
		GuideStarContainer gs = null;
		while (!shutdown) {
			try {
				gs = this.readGuideStarStream();
				if (gs == null) {
					shutdown = true;
					continue;
				}

				queueGuideStarforDistribution(gs);

				if (gs.isEndConnectionPackage())
					shutdown = true;

			} catch (Exception e) {
				if (EOFException.class.isInstance(e)) {
					myLogger.info(
							"End of file in Guidestar Container data stream", e);
				} else {
					myLogger.error(
							"Error while reading from guide star socket", e);
				}
				shutdown = true;
				continue;

			}
		}
		stop();
	}

	protected void queueGuideStarforDistribution(GuideStarContainer gs) {
		// Guide Star distribution is funneled through the single master Guide
		// Star Receiver to avoid competition in the receiver with
		// simultaneous arrival of packages.
		receivedPackage++;
		if (myMaster != null) {
			if (!myMaster.myGSQueue.offer(gs)) {
				myLogger.warn("Guidestar was rejected when offering to receiver distribution queue. Guide star was lost in GuideStarReceiver thread.");
			}
			if (myMaster.myGSQueue.size() > 5) {
				myLogger.warn("More than 50 elemets in Guide Star receiver queue. Not keeping up with processing ...?");
			}
		}
	}

	ByteBuffer bb = null;
	byte[] ba = null;

	private static String readLine(InputStream inputS) {

		StringBuilder sb = new StringBuilder();

		byte[] myByteBuffer = new byte[1];
		int n = 0;
		try {
			while ((n = inputS.read(myByteBuffer)) > 0) {

				if (myByteBuffer[0] == (char) '\n') {
					String retVal = sb.toString();
					if (myLogger.isDebugEnabled())
						myLogger.debug("Got Line: " + retVal);
					return retVal;
				} else

					sb.append((char) myByteBuffer[0]);

			}

			if (n < 0) {
				if (myLogger.isInfoEnabled())
					myLogger.warn("GuideStarReceiver.readline: Got a broken connection, assuming it has been closed. Received so far: "
							+ sb.toString());
				return null;

			}
		} catch (IOException e) {
			myLogger.warn("readline: error: ", e);

		}
		myLogger.error("Readline: Shouldn't get to here");
		return sb.toString();
	}

	final static private byte[] triggerString = "GSContainer\n".getBytes();

	private boolean waitForGSContainer(InputStream inputS) {

		byte[] myByteBuffer = new byte[1];
		int triggerIndex = 0;

		try {
			while (inputS.read(myByteBuffer) > 0) {

				byte currentChar = myByteBuffer[0];

				if (triggerIndex == 0 && currentChar == triggerString[0]) {

					triggerIndex = 1;

				} else if (triggerIndex != 0) {

					if (currentChar == triggerString[triggerIndex]) {

						if (triggerIndex == triggerString.length - 1)
							return true;
						else
							triggerIndex++;

					} else {
						myLogger.warn("Guide Star trigger lost at position "
								+ triggerIndex + ". Probably lost a packge.");
						triggerIndex = 0;
						lostPackage++;

					}
				}

			}
		} catch (IOException e) {
			myLogger.debug("readline: error: ", e);

		}

		myLogger.info("Guidestarcontainer.waitforGS: Got a broken connection, assuming it has been closed.");
		return false;
	}

	protected GuideStarContainer readGuideStarStream() {

		while (true) {
			String line = null;

			if (waitForGSContainer(inStream) == false) {
				return null;
			}

			{
				if (myLogger.isDebugEnabled())
					myLogger.debug("found GSContainer");

				GuideStarContainer gs = new GuideStarContainer();

				while (true) {

					if ((line = readLine(inStream)) == null) {
						myLogger.debug("Received null line from guide star inputstream");
						return null;
					}

					if (line != null && !line.startsWith("Image")) {

						gs.addHashFromString(line);
					} else
						break;
				}

				if (line != null) {
					if (myLogger.isDebugEnabled())
						myLogger.debug("got image line");

					assert (line.startsWith("Image"));
					StringTokenizer tok = new StringTokenizer(line);
					tok.nextToken(); // Image;

					String label = tok.nextToken();
					if (!label.equals("null")) {
						int dimX = Integer.parseInt(label);
						int dimY = Integer.parseInt(tok.nextToken());

						gs.setImageDimX(dimX);
						gs.setImageDimY(dimY);

						if (bb == null || ba == null
								|| bb.capacity() < dimX * dimY * 2) {
							ba = new byte[dimX * dimY * 2];
							bb = ByteBuffer.wrap(ba);
						}
						bb.clear();

						try {

							int readBytes = 0;
							int readiterations = 0;
							do {
								int deltaBytes = inStream.read(ba, readBytes,
										dimX * dimY * 2 - readBytes);
								readBytes += deltaBytes;
								if (deltaBytes <= 0) {
									myLogger.info("Broken data pipe");
									return null;
								}
								readiterations++;
							} while (readBytes < dimX * dimY * 2);

							if (readiterations > 3) {
								myLogger.debug("Needed >2 reads to get guide star package");
							}
						} catch (IOException e) {

							myLogger.error(
									"Error while trying to read image buffer:",
									e);
						}

						gs.readImageFromByteBuffer(bb, dimX * dimY);
					} else {
						// null image; do noting
					}
					return gs;
				}
			}
		}
	}

}
