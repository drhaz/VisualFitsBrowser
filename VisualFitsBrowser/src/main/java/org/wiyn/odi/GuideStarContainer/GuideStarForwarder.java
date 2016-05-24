package org.wiyn.odi.GuideStarContainer;

import org.apache.log4j.Logger;

// TODO: Move functionality into a background thread, and limit reconnection attempts.

public class GuideStarForwarder implements GuideStarContainerListener {
	final static Logger myLogger = Logger.getLogger(GuideStarForwarder.class);
	String hostname;
	int port;
	boolean sendCatalogs = false;
	// private GuideStarSender theGuideStarSender;

	private boolean CatalogMode = false;

	private GuideStarSender mySender = null;

	private boolean forwardEnable = true;

	private long lastReconnectionAttempt = 0;
	private final static long reconnectTimeout = 1000 * 10; // allow reconnect

	// every
	// 30 seconds or

	// so

	private GuideStarSender getSender(boolean reInit) {

		if (reInit || mySender == null || !mySender.isAlive()) {
			if (reInit) {
				if (mySender != null)
					mySender.closeConnection();
				mySender = null;
			}
			// need to work on the connection
			long now = System.currentTimeMillis();
			if (now - lastReconnectionAttempt > reconnectTimeout || reInit) {

				if (mySender == null) {
					myLogger.info("Creating new Guide Star Sender");
					mySender = new GuideStarSender(hostname, port);
					lastReconnectionAttempt = now;
				}
				if (!mySender.isAlive()) {

					myLogger.info("Resetting guide star sender Conenction.");
					mySender.reset();
					lastReconnectionAttempt = now;

				}

			} else
				myLogger.debug("Need to reset connection, but do not want to flood receiver.");
		}
		return mySender;

	}

	public GuideStarForwarder(String hostname, int port, boolean sendCatalogs) {
		this.hostname = hostname;
		this.port = port;
		this.sendCatalogs = sendCatalogs;
	}

	
	public void pushNewGuideStarContainer(GuideStarContainer gs) {

		if (forwardEnable && !gs.isEndConnectionPackage()) {

			// Create a new connection for a new video stream
			GuideStarSender mySender = getSender(gs.isStartVideoPackage());
			if (mySender != null) {

				if (gs.isStartCatalogPackage()) {
					CatalogMode = true;
					if (sendCatalogs)
						mySender.queueGuideStarObject(gs);
					return;
				}

				if (gs.isEndVideoPackage()) {

					if ((!CatalogMode) || (CatalogMode && sendCatalogs))
						mySender.queueGuideStarObject(gs);
					CatalogMode = false;
					return;
				}

				if (sendCatalogs || !CatalogMode) {

					mySender.queueGuideStarObject(gs);
				}
			} else {
				if (myLogger.isDebugEnabled()) {
					myLogger.debug("received Guide Star Pacakge, but cannot forward since sender is NULL.");
				}
			}

		} else {

			if (myLogger.isDebugEnabled())
				myLogger.debug("Received guide star package,but forwarding is disabled");

		}

	}

	public boolean isForwardEnable() {
		return forwardEnable;
	}

	public void setForwardEnable(boolean forwardEnable) {
		this.forwardEnable = forwardEnable;
	}
}
