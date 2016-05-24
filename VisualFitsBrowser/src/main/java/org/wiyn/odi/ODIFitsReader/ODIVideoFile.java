package org.wiyn.odi.ODIFitsReader;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jsky.util.gui.ProgressPanel;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import nom.tam.image.ImageTiler;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.wiyn.odi.GuideStarContainer.GuideStarContainer;
import org.wiyn.odi.GuideStarContainer.GuideStarSender;

public class ODIVideoFile {
	private final static Logger myLogger = Logger.getLogger(ODIVideoFile.class);

	// private static final String VIDEO_DIR = "expVideo";

	// List of directories to search for video files. expVideo is legacy code
	// from early installation and commissioning of pODI (Sept 2012)
	// ,,VIDEO'' is ICD compliant
	private static final String[] VIDEO_DIRS = { "VIDEO", "expVideo" };
	private static final String VIDEO_POSTFIX = "";

	ArrayList<ImageTiler> videoTilers = null;
	ArrayList<Integer> nGuideStars = null;

	long nGuideCycles = -1;
	int GuideWindowX = -1;
	static int BSCALE = 1;
	static int BZERO = 0;

	long cycleCounter = 0;
	private static HashMap<Point2D, String> otaToSGBoard = new HashMap<Point2D, String>();

	static {
		otaToSGBoard.put(new Point2D.Float(0, 0), "H4");
		otaToSGBoard.put(new Point2D.Float(1, 6), "A4");

		otaToSGBoard.put(new Point2D.Float(2, 2), "G4");
		otaToSGBoard.put(new Point2D.Float(2, 3), "G3");
		otaToSGBoard.put(new Point2D.Float(2, 4), "B2");

		otaToSGBoard.put(new Point2D.Float(3, 2), "G1");
		otaToSGBoard.put(new Point2D.Float(3, 3), "G2");
		otaToSGBoard.put(new Point2D.Float(3, 4), "B3");

		otaToSGBoard.put(new Point2D.Float(4, 2), "F4");
		otaToSGBoard.put(new Point2D.Float(4, 3), "F3");
		otaToSGBoard.put(new Point2D.Float(4, 4), "C2");

		otaToSGBoard.put(new Point2D.Float(5, 5), "C3");
		otaToSGBoard.put(new Point2D.Float(6, 1), "E3");
	}

	public ODIVideoFile(String Rootname) {
		ArrayList<Fits> videoFiles = getVideoFiles(Rootname);

		if (videoFiles.size() > 0) {

			nGuideStars = new ArrayList<Integer>(videoFiles.size());
			videoTilers = new ArrayList<ImageTiler>(videoFiles.size());

			// Get the initialisation data
			int ii = 0;
			for (Fits fits : videoFiles) {

				ImageHDU h = null;
				Header myHeader;
				try {
					h = (ImageHDU) fits.getHDU(0);
				} catch (Exception e) {
					myLogger.error("Error while reading fits file:  ", e);
					this.nGuideStars.add(0);
					continue;
				}
				myHeader = h.getHeader();
				GuideWindowX = myHeader.getIntValue("NAXIS1");
				nGuideCycles = myHeader.getIntValue("NAXIS2") / GuideWindowX;
				BZERO = myHeader.getIntValue("BZERO");
				BSCALE = myHeader.getIntValue("BSCALE");

				this.nGuideStars.add(1);
				this.videoTilers.add(h.getTiler());
				ii++;
			}

			myLogger.info("Read " + videoTilers.size()
					+ " video fits files. Window: " + GuideWindowX
					+ " iterations: " + nGuideCycles);

		}

	}

	public GuideStarContainer[] readRow(int n) {
		myLogger.debug("reading Guide Star Video Row: " + n);
		List<GuideStarContainer> retVal = new ArrayList<GuideStarContainer>();

		int id = 0;
		for (ImageTiler tiler : videoTilers) {
			GuideStarContainer[] gsc = loadGuideStarRow(tiler, GuideWindowX, n,
					1, (int) nGuideCycles);

			for (GuideStarContainer gs : gsc) {
				gs.setID(id);
				retVal.add(gs);
				id++;
			}
		}

		return (GuideStarContainer[]) (retVal
				.toArray(new GuideStarContainer[retVal.size()]));
	}

	public static GuideStarContainer[] loadGuideStarRow(ImageTiler myTiler,
			int windowX, int iteration, int nGuideStars, int nIterations) {

		GuideStarContainer[] retVal = new GuideStarContainer[nGuideStars];

		short[] tempImage = null;
		Object data = null;

		// myLogger.debug ("loading guide star column:" + iteration);

		if (iteration >= 0 && iteration < nIterations) {
			try {

				data = myTiler.getTile(new int[] { iteration * windowX, 0 },
						new int[] { windowX, nGuideStars * windowX });

			} catch (IOException e) {

				myLogger.error("Error while reading guide star column "
						+ iteration, e);
			}
		}

		if (data != null) {

			if (data.getClass().getComponentType() == short.class) {

				tempImage = (short[]) data;
				for (int ii = 0; ii < tempImage.length; ii++) {
					long pixel = tempImage[ii] * BSCALE + BZERO;
					tempImage[ii] = (short) (pixel & 0xFFFF);

				}
				GuideStarContainer gs = new GuideStarContainer();

				gs.setImage(tempImage, windowX, windowX);
				retVal[0] = gs;

			} else {

				myLogger.error("Unsupported data type: "
						+ data.getClass().getComponentType());

			}

		} else
			myLogger.warn("getTile returned emtpy data array");

		return retVal;

	}

	public static ArrayList<Fits> getVideoFiles(String Rootname) {
		ArrayList<Fits> myFiles = new ArrayList<Fits>();

		String[] list = getVideoFileList(Rootname, -1, -1);

		if (list != null && list.length > 0) {

			// Create fits files
			for (String videoname : list) {
				myLogger.debug("Opening pODI video file: " + videoname);

				Fits fits = openVideoFitsFile(videoname);
				if (fits != null) {
					myFiles.add(fits);
				}
			}

		}
		return myFiles;
	}

	private static Fits openVideoFitsFile(String fname) {
		Fits fitsFile = null;
		ImageHDU h = null;
		Header myHeader;

		try {
			fitsFile = new Fits(fname);

			h = (ImageHDU) fitsFile.getHDU(0);
			myHeader = h.getHeader();

		} catch (Exception e) {

			myLogger.error("Error while trying to open guide star file.", e);

			return null;

		}

		return fitsFile;

	}

	protected static String[] getVideoFileList(String imageRootName, int otaX,
			int otaY) {

		if (imageRootName == null) {
			myLogger.warn("getVideoFileList: requesting video file list for null image. Strange.");
			return null;
		}

		String controllerTemplate = (otaX < 0 && otaY < 0) ? "odi[A-H][1-4]"
				: otaToControllerID(otaX, otaY);
		String match = ".*" + controllerTemplate + VIDEO_POSTFIX
				+ "\\.[Ff][Ii][Tt][Ss]";
		FilenameFilter ff = new RegExMatch(match);

		File dir = null;
		// test all candidate directories for the existence of a video file
		for (String testDir : ODIVideoFile.VIDEO_DIRS) {
			dir = new File(imageRootName + "/" + testDir + "/");

			if (dir.exists())
				break;
		}

		ArrayList<String> fnameVec = new ArrayList<String>();
		if (dir != null && dir.exists()) {
			String[] list = dir.list(ff);
			for (String f : list) {
				File myFile = new File(dir.getAbsolutePath() + "/" + f);

				if (!myFile.exists()) {
					myLogger.error("File does not exist: "
							+ myFile.getAbsolutePath());
					continue;
				}

				if (myFile.length() > 0) {

					myLogger.debug("Adding video file"
							+ myFile.getAbsolutePath());
					fnameVec.add(myFile.getAbsolutePath());

				} else {

					myLogger.debug("Rejecting 0-byte video file:"
							+ myFile.getAbsolutePath());

				}
			}

			if (fnameVec.size() > 0) {
				return (String[]) fnameVec.toArray(new String[fnameVec.size()]);
			}
		}
		myLogger.info("No valid video file exists for image " + imageRootName);
		return null;

	}

	public static File getOTAVideoFile(String Rootname, int otaX, int otaY)

	{

		if (Rootname == null)
			return null;

		String[] list = getVideoFileList(Rootname, otaX, otaY);

		if (list != null && list.length > 0) {

			return new File(list[0]);

		}

		return null;
	}

	private static String otaToControllerID(int x, int y) {
		StringBuffer retVal = new StringBuffer("odi");

		String CB = otaToSGBoard.get(new Point2D.Float(x, y));
		if (CB == null) {
			myLogger.warn("No Controller found for detector " + x + "/" + y);
			return null;
		}

		return retVal.append(CB).toString();
	}

	static boolean stop;

	public static void playVideos(String fname) {

		if (fname == null) {
			myLogger.debug("No image for replay selected");
			return;
		} else {
			myLogger.info("Starting playback from file");
		}

		myLogger.info("Opening Fits Video Files");
		ODIVideoFile v = new ODIVideoFile(fname);

		if (v.videoTilers == null || v.videoTilers.size() == 0) {

			myLogger.warn("Returned empty v.");

			return;
		}

		ProgressPanel p = ProgressPanel
				.makeProgressPanel("Play back guide star file");
		p.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				stop = true;
			}

		});

		getSender().sendNewExposure(v.videoTilers.size());

		for (int guideCycle = 0; guideCycle < v.nGuideCycles - 1; guideCycle++) {
			myLogger.debug("Sending video cycle " + guideCycle);
			if (stop) {
				myLogger.info("guide star sending stopped at user's request.");
				break;
			}

			GuideStarContainer[] gsc = v.readRow(guideCycle);
			if (gsc == null) {
				myLogger.error("Reading guide star container packages from from fits files failed. stopping video");
				break;
			}

			for (GuideStarContainer gs : gsc) {
				gs.setCycle(guideCycle);
				getSender().queueGuideStarObject(gs);
			
			}

			p.setProgress((int) (100 * guideCycle / v.nGuideCycles));

			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {

			}

		}
		getSender().sendEndExposure();
		getSender().closeConnection();

		p.stop();
		stop = false;

	}

	private static GuideStarSender mySender = null;

	public static String targetHost = null;

	private static GuideStarSender getSender() {

		if (targetHost == null)
			targetHost = "wiyn-5";

		if (mySender == null)
			mySender = new GuideStarSender(targetHost);

		if (!mySender.isAlive()) {

			myLogger.info("Resetting sender");
			mySender.reset();
		}
		return mySender;

	}

	public static void main(String args[]) {
		BasicConfigurator.configure();

		myLogger.setLevel(Level.DEBUG);
		String fname = "/Volumes/odifile/archive/podi/test/2012.07.09/o20120709T130552.0";

		System.out.println("Experiementing with fits file: \n\t" + fname);
		// ODIVideoFile v = new ODIVideoFile (fname);
		// v.readRow (0);
		// v.readRow ((int) v.nGuideCycles - 1);

		ODIVideoFile.playVideos(fname);
		System.exit(0);

	}

}
