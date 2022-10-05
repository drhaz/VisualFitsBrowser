package org.cowjumping.FitsUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.Callable;


public class starFinder implements Callable<starFinder> {

	private final static Logger myLogger = LogManager.getLogger(starFinder.class);

	/** The bias (overscan) level of the image */
	double biasLevel = 0;

	/** The sky background level of the image */
	double skyLevel = 0;

	/** the assumed background noise in the iamge */
	double skyNoise = 0;

	/** the fraction of saturated pixels in the image */
	double satFrac = 0;

	/** The saturation level in the image */
	double saturationLevel = 55000;

	/** Detection sigma threshold */
	double detSigma = 20;

	/** Assumed exposure time of the image */
	double expTime = 1;

	/** Flag weather to generate thumbnail images or not */
	boolean getCutOuts = true;

	/** Holder for detected guide stars */
	public Vector<ImageContainer> detectedStars = new Vector<ImageContainer>();

	/** size of the local maximum box for a star detection */
	int starRadius = 11;

	/** An internal reference to the image in which to find stars. */
	private ImageContainer image;

	public starFinder(ImageContainer cellImage) {

		this.image = cellImage;
		expTime = this.image.getExptime();

		if (expTime <= 0) {

			myLogger.debug("starFinder received image with an invalid exposure time (<=0):\n "
					+ cellImage.toString());
			expTime = 1;

		}

	}

	/**
	 * This is the workhorse routine of a starfinder
	 * 
	 */
	public starFinder call() throws Exception {

		findImageStatistics(this.image, this);

		if (satFrac > 0.22) {
			if (myLogger.isDebugEnabled())
				myLogger.debug("Cell has a too high saturation fraction: "
						+ satFrac + ". This is fishy, skipping that one.");
			return this;
		}

		if (skyNoise == 0) {
			if (myLogger.isDebugEnabled())
				myLogger.debug("Skipping star search because of invalid sky level & noise measurement.");
			return this;
		}
		if (skyNoise < 10) {
			skyNoise = 10;
			if (myLogger.isDebugEnabled())
				myLogger.debug("Low sky noise measurement adjusted to minium of readnoise (10).");
		}

		// go, find the stars.
		findStars();
		return this;
	}

	public void findStars() {

		if (skyNoise == 0) {
			myLogger.warn("Skynoise is set to 0. Not processing this cell!");
			return;
		}

		/*
		 * We do not do a local background subtraction here. we define a minimum
		 * level a pixel has to have in order to be accepted as a valid seed for
		 * a bright star.
		 * 
		 * This is potentially difficult when searching at the boundary of a
		 * galaxy or so.
		 */
		short starThresh = (short) (skyLevel + skyNoise * detSigma + 20);

		float[] imageBuffer = image.getRawImageBuffer();
		short[] imageMask = new short[imageBuffer.length];

		int dimX = image.getImageDimX();
		int dimY = image.getImageDimY();


		int id = 0;
		// TODO: Here is potential for memory optimization by not creating a new
		// guide star object for each candidate, but to re-use an existing one
		// and only clone it when a star is found acceptable.

		for (int yy = 0 + starRadius; yy < dimY - starRadius; yy++) {

			for (int xx = 0 + starRadius; xx < dimX - starRadius; xx++) {

				float value = imageBuffer[yy * dimX + xx];

				/* Is pixel a valid star seed? */
				if (value > starThresh //
						&& value < this.saturationLevel //
						&& imageMask[yy * dimX + xx] == 0 //
						&& localMaximum(imageBuffer, dimX, dimY, xx, yy,
								starRadius)) {

					ImageContainer gs = new ImageContainer();

					// We need a small sub-image to do photometry
					ImageContainer.extractThumImage(image, gs, 48, xx, yy);

//					odiMomentCentroider.doCentroiding(gs);
//					odiCentroidSupport.gaussianFitFWHM(gs);

					// TODO: Make acceptance criteria a configurable parameter
					if (//
					gs.getFWHM_X() > 2 //
							&& gs.getFWHM_X() < 30 //
							&& gs.getFlux() > 100 //

					) {


						gs.setID(id++);

						gs.setCenterX(gs.getCenterX() + gs.getWindow_Offset_X());
						gs.setCenterY(gs.getCenterY() + gs.getWindow_Offset_Y());
						gs.setBinningX(image.getBinningX());
						gs.setBinningY(image.getBinningY());
						this.detectedStars.add(gs);

						gs.setInstMag((float) (-2.5 * Math.log10(gs.getFlux()
								/ expTime)));

						applyMask(imageMask, dimX, dimY, xx, yy, starRadius);
						if (myLogger.isDebugEnabled()) {
							myLogger.debug("Adding new  star to catalog: " + gs);
						}
						xx += starRadius - 1;

					} else {
						if (myLogger.isDebugEnabled())
							myLogger.debug("Guide star rejected due to out of viable range.");
					}
				}

			}

		}
		imageMask = null;

	}

	/**
	 * Find the sky level & noise level of an image. It also determines the
	 * saturation percentage.
	 * 
	 * @param image
	 * @param sF
	 */
	public static void findImageStatistics(ImageContainer image,
			starFinder sF) {

		int nSat = 0;
		int n = 0;
		float[] buffer = image.getRawImageBuffer();
		float[] medBuf = new float[buffer.length * 217 / 1000 + 1];

		// Get a sample of the image,but not everything!
		for (int kk = 0; kk < image.getImageDimX() * image.getImageDimY(); kk += image
				.getImageDimX() * 217 / 1000) {

			if (buffer[kk] > sF.saturationLevel) {
				nSat++;
			} else if (buffer[kk] > 0) {
				medBuf[n] = buffer[kk];
				n++;
			}
		}

		if (n < 20) {
			sF.skyLevel = 0;
			sF.skyNoise = 0;
			return;
		}

		sF.satFrac = (double) nSat / (n + nSat);

		sF.skyLevel = selectKth(medBuf, n / 2, n, true);
		sF.skyNoise = 1.33 * (sF.skyLevel - medBuf[n / 4]);

		if (myLogger.isDebugEnabled())
			myLogger.debug("First pass sky estimate: " + sF.skyLevel + "+/-"
					+ sF.skyNoise + " " + n);

		// Now do a 3-sigma clip;
		int lower = (int) (sF.skyLevel - sF.skyNoise * 3);
		int upper = (int) (sF.skyLevel + sF.skyNoise * 3);
		int oldN = n;
		n = 0;
		for (int ii = 0; ii < oldN; ii++) {
			float val = medBuf[ii];
			if (val > lower && val < upper) {
				medBuf[n] = medBuf[ii];
				n++;
			}
		}
		sF.skyLevel = selectKth(medBuf, n / 2, n, true);
		sF.skyNoise = 1.33 * (sF.skyLevel - medBuf[n / 4]);

		image.setBackground((float) sF.skyLevel);
		image.setBackNoise((float) sF.skyNoise);
		double ShotnoiseRatio = Math.sqrt(sF.skyLevel) / sF.skyNoise;
		if (myLogger.isDebugEnabled())
			myLogger.debug("Sky for image base on " + n + " pixels:" + image);
		if (ShotnoiseRatio > 10 || ShotnoiseRatio < 0.1) {
			sF.skyLevel = 0;
			sF.skyNoise = 0;

		}
		if (myLogger.isDebugEnabled())
			myLogger.debug("Second pass sky estimate: " + sF.skyLevel + "+/-"
					+ sF.skyNoise + " " + n + "  " + ShotnoiseRatio);

	}

	/**
	 * Numerical Recipies, page 342; Chapter 8 sorting
	 * 
	 * @param data
	 * @param k
	 * @param destructive
	 *            if true, the array can be rearanged by the the algorithm. if
	 *            false, a local copy will be created instead.
	 * @return
	 */
	public static float selectKth(float[] data, int k, int N,
			boolean destructive) {

		// this algorithm distorts data, so we create a local copy
		float[] arr = data;

		if (destructive == false) {
			arr = new float[data.length];
			System.arraycopy(data, 0, arr, 0, data.length);
		}

		int i, partitionRight, j, partionLeft, mid;
		float a;
		partionLeft = 0;
		partitionRight = N - 1;
		for (;;) {

			if (partitionRight <= partionLeft + 1) {

				// last sort iteration here, only two elements are left, sort
				// them
				if (partitionRight == partionLeft + 1
						&& arr[partitionRight] < arr[partionLeft]) {
					swap(arr, partionLeft, partitionRight);
				}

				return arr[k];

			} else {

				mid = (partionLeft + partitionRight) >> 1;
				swap(arr, mid, partionLeft + 1);

				if (arr[partionLeft] > arr[partitionRight])
					swap(arr, partionLeft, partitionRight);

				if (arr[partionLeft + 1] > arr[partitionRight])
					swap(arr, partionLeft + 1, partitionRight);

				if (arr[partionLeft] > arr[partionLeft + 1])
					swap(arr, partionLeft, partionLeft + 1);

				i = partionLeft + 1;
				j = partitionRight;
				a = arr[partionLeft + 1];

				for (;;) {
					do {
						i++;
					} while (arr[i] < a);

					do {
						j--;
					} while (arr[j] > a);

					if (j < i)
						break;
					swap(arr, i, j);
				}
				arr[partionLeft + 1] = arr[j];
				arr[j] = a;
				if (j >= k)
					partitionRight = j - 1;
				if (j <= k)
					partionLeft = i;

			}

		}

	}

	/**
	 * Checks if an object is a local maximum, or just a bright pixel above
	 * threshold on a gradient.
	 * 
	 * 
	 * @param buffer
	 * @param dimX
	 * @param dimY
	 * @param centerX
	 * @param centerY
	 * @param searchRadius
	 * @return
	 */

	public boolean localMaximum(float[] buffer, int dimX, int dimY,
			int centerX, int centerY, int searchRadius) {

		float value = buffer[dimX * centerY + centerX];

		if (centerX - searchRadius < 0 || centerX + searchRadius > dimX - 1
				|| centerY - searchRadius < 0
				|| centerY + searchRadius > dimY - 1)
			return false;

		// first quick test: nearest neighbors
		if (value < buffer[dimX * centerY + centerX + 1]
				|| value < buffer[dimX * centerY + centerX - 1]
				|| value < buffer[dimX * (centerY + 1) + centerX]
				|| value < buffer[dimX * (centerY - 1) + centerX])
			return false;

		for (int yy = -searchRadius; yy < searchRadius; yy++)
			for (int xx = -searchRadius; xx < searchRadius; xx++) {
				float pixelValue = buffer[dimX * (centerY + yy) + centerX + xx];
				if (value < pixelValue || pixelValue >= this.saturationLevel)
					return false;

			}

		return true;

	}

	public static void applyMask(short buffer[], int dimX, int dimY,
			int centerX, int centerY, int radius) {
		if (centerX - radius < 0 || centerX + radius > dimX - 1
				|| centerY - radius < 0 || centerY + radius > dimY - 1)
			return;

		for (int yy = -radius; yy < radius; yy++)
			for (int xx = -radius; xx < radius; xx++) {

				buffer[dimX * (centerY + yy) + centerX + xx] = 1;

			}

	}

	public static short getMedianSort(short[] data, boolean destructive) {
		short ov = 0;
		short[] arr = data;
		if (destructive == false) {

			arr = new short[data.length];
			System.arraycopy(data, 0, arr, 0, data.length);
		}

		Arrays.sort(arr);

		ov = arr[arr.length / 2];
		return ov;
	}

	public static short getFirstQuartileSort(short[] data, boolean destructive) {
		short ov = 0;
		short[] arr = data;
		if (destructive == false) {

			arr = new short[data.length];
			System.arraycopy(data, 0, arr, 0, data.length);
		}

		Arrays.sort(arr);

		ov = arr[arr.length / 4];
		return ov;
	}

	private static void swap(float[] data, int a, int b) {
		float t = data[a];
		data[a] = data[b];
		data[b] = t;

	}

	public static void main(String[] args) throws Exception {


//		catalogFromOTA c = new catalogFromOTA(
//				"/Volumes/odifile/archive/podi/test/2012.08.13/o20120813T231626.0",
//				0, 0, true);
//		long start = System.currentTimeMillis();
//
//		c.call();
//		c.numberCatalogStars();
//		GuideStarSender.dumpGuideStarCatalog("gslist.dat", c.cellStarCatalog);

//		long end = System.currentTimeMillis();
//		System.out.println("Execution time: " + ((end - start) / 1000.)
//				+ "seconds.");

	}
}
