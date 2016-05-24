package org.wiyn.odi.ODIFitsReader;

import java.io.OutputStream;
import java.nio.ShortBuffer;

import org.apache.log4j.Logger;

import nom.tam.fits.Fits;

public class ODIFitsReaderUtilities {

	final static private Logger myLogger = Logger.getLogger(ODIFitsReaderUtilities.class);
	private static int BADPIXELVALUE = 65536;
	public final static int clamp(int value) {
		if (value < 0)
			return 0;
		if (value > 65535)
			return 65535;
		return value;
	}

	// Measure the overscan level of an image that is representatd as a short
	// buffer

	public static float determineOVLevel_rawBuffer(ShortBuffer sb, int NAXIS1, int NAXIS2, int datasecX, int datasecY,
			int BZERO) {

		float ovValue;

		// Determine OV from the y overscan region
		int minx = datasecX + 20;
		minx = minx > NAXIS1 - 5 ? NAXIS1 - 5 : minx;
		int maxx = NAXIS1 - 1;
		int miny = 10;
		int maxy = datasecY - 10;

		int n = (maxx - minx) * (maxy - miny);

		if (n <= 0) {
			myLogger.error("determineOVLevel_rawBuffer: aborted due to insufficient number of pixels.");
			return 0;
		}

		short[] ovBuffer = new short[n];
		n = 0;

		for (int row = miny; row < maxy; row++) {
			for (int xx = minx; xx < maxx; xx++) {

				int idx = row * NAXIS1 + xx;
				short value = (short) ((sb.get(idx) + BZERO) & 0xFFFF);

				// ignore saturated pixels!
				if (value != 0 && value != BADPIXELVALUE) {
					ovBuffer[n] = value;
					n++;
				}
			}
		}

		ovValue = (selectKth(ovBuffer, (n) / 2, n, true));
		ovBuffer = null;
		return ovValue;
	}

	static public void removeOVLineByLine(ShortBuffer sb, int NAXIS1, int NAXIS2, int datasecX, int datasecY, int BZERO,
			short offsetLevel) {

		removeOVLineByLine_AREA(sb, NAXIS1, NAXIS2, datasecX, datasecY, BZERO, offsetLevel, 0, NAXIS2);
	}

	static protected void removeOVLineByLine_AREA(ShortBuffer sb, int NAXIS1, int NAXIS2, int datasecX, int datasecY,
			int BZERO, short offsetLevel, int startY, int endY) {

		float ovValue;

		int minX = datasecX + 20;
		int maxX = NAXIS1 - 1;

		int n = 1 * (maxX - minX); // line by line ov buffer
		short[] ovBuffer = new short[n];

		// For each image line
		for (int row = startY; row < endY; row++) {
			n = 0;
			for (int xx = minX; xx < maxX; xx++) {

				int idx = row * NAXIS1 + xx;

				int pixelValue = sb.get(idx) + BZERO;

				if (pixelValue < 0 && BZERO == 0)
					pixelValue += 32768 * 2;

				short value = (short) (clamp(pixelValue) & 0xFFFF);
				if (value != 0 && value != BADPIXELVALUE) {
					ovBuffer[n++] = value;
				}
			}

			ovValue = selectKth(ovBuffer, (n) / 2, n, true) - offsetLevel;

			for (int xx = 0; xx < NAXIS1; xx++) {

				int idx = row * NAXIS1 + xx;

				int pixelValue = sb.get(idx) + BZERO;
				if (pixelValue < 0 && BZERO == 0)
					pixelValue += 32768 * 2;

				if (pixelValue != 0 && pixelValue != BADPIXELVALUE) {
					pixelValue = pixelValue - (int) ovValue;

					pixelValue = clamp(pixelValue) - BZERO;
				} else {
					pixelValue = BADPIXELVALUE;
				}

				short newValue = (short) (pixelValue & 0xFFFF);

				sb.put(idx, newValue);
			}

		}

	}

	public static int ODI2IrafMosaic(String inname, String outname) {
		int retStat = 0;

		Fits f = new Fits();

		for (int otaY = 0; otaY < 8; otaY++) {
			for (int otaX = 0; otaX < 8; otaX++) {

				// Vector<GuideStarContainer> otaImage = OTAReader
				// .readDetectorMEFFile(inname, otaX, otaY, -1, -1);

			}
		}
		OutputStream out = null;
		return retStat;

	}

	private static void swap(short[] data, int a, int b) {
		short t = data[a];
		data[a] = data[b];
		data[b] = t;

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
	private static short selectKth(short[] data, int k, int N, boolean destructive) {

		// this algorithm distorts data, so we create a local copy
		short[] arr = data;

		if (destructive == false) {
			arr = new short[data.length];
			System.arraycopy(data, 0, arr, 0, data.length);
		}

		int i, partitionRight, j, partionLeft, mid;
		short a;
		partionLeft = 0;
		partitionRight = N - 1;
		for (;;) {

			if (partitionRight <= partionLeft + 1) {

				// last sort iteration here, only two elements are left, sort
				// them
				if (partitionRight == partionLeft + 1 && arr[partitionRight] < arr[partionLeft]) {
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

	public static void main(String[] args) {
		ODI2IrafMosaic("test.fits", "/tmp/test.fits");
	}
}
