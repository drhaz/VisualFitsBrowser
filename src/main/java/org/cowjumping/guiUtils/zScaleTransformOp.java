package org.cowjumping.guiUtils;

import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

// zScales an input image to a ARGB destination Image

public class zScaleTransformOp implements BufferedImageOp {

//	float z1 = 0;
//	float z2 = 65000;

	float offset = 0;
	float factor = 1;

	public final static int alpha = 0xFF000000;
	static int[] colorLookup = null;
	static int lookupSize = 1024;
	public static boolean doColorLookUp = true;

	static {
		try {
			initLookUpTable(null);
		} catch (Exception e) {
		}
	}

	public zScaleTransformOp(float z1, float z2, double gain) {
		super();
		updateZScale(z1, z2, gain);
	}

	private void updateZScale(float z1, float z2, double gain) {
	//	this.z1 = z2;
	//	this.z2 = z2;

		this.offset = -z1;
		this.factor = lookupSize / (z2 - z1);

	}

	public BufferedImage createCompatibleDestImage(BufferedImage src,
			ColorModel destCM) {
		BufferedImage image;

		image = new BufferedImage(src.getWidth(), src.getHeight(),
				BufferedImage.TYPE_INT_RGB);

		return image;
	}

	public void setDimensions(int width, int height) {
	}

	public int filterUSHORT(int x, int y, short pixel16bit) {

		int outValue = ((int) pixel16bit) & 0xffff;
		outValue = (int) ((outValue + offset) * factor);
		if (outValue < 0)
			outValue = 0;
		if (outValue > lookupSize - 1)
			outValue = lookupSize - 1;

		// If we do a color lookup, we look up a new color. Otherwise, just
		// transfor the output value to a greyscale.

		if (!doColorLookUp) {
			outValue = (int) (outValue * 255. / lookupSize);
			if (outValue < 0)
				outValue = 0;
			if (outValue > 255)
				outValue = 255;
		}

		int outpixel = doColorLookUp ? colorLookup[outValue] : alpha
				| (outValue & 0xFF) << 16 | (outValue & 0xFF) << 8 | (outValue & 0xFF);
		return outpixel;
	};

	public BufferedImage filter(BufferedImage src, BufferedImage dst) {
		int width = src.getWidth();
		int height = src.getHeight();
		//int type = src.getType();
		WritableRaster srcRaster = src.getRaster();

		if (dst == null)
			dst = createCompatibleDestImage(src, null);

		WritableRaster dstRaster = dst.getRaster();

		setDimensions(width, height);

		short[] inPixels = new short[width];
		int[] outPixels = new int[width];
		for (int y = 0; y < height; y++) {
			// We try to avoid calling getRGB on images as it causes them to
			// become unmanaged, causing horrible performance problems.

			srcRaster.getDataElements(0, y, width, 1, inPixels);
			for (int x = 0; x < width; x++)
				outPixels[x] = filterUSHORT(x, y, inPixels[x]);
			dstRaster.setDataElements(0, y, width, 1, outPixels);

		}

		return dst;
	}

	/**
	 * A convenience method for getting ARGB pixels from an image. This tries to
	 * avoid the performance penalty of BufferedImage.getRGB unmanaging the
	 * image.
	 * 
	 * @param image
	 *            a BufferedImage object
	 * @param x
	 *            the left edge of the pixel block
	 * @param y
	 *            the right edge of the pixel block
	 * @param width
	 *            the width of the pixel arry
	 * @param height
	 *            the height of the pixel arry
	 * @param pixels
	 *            the array to hold the returned pixels. May be null.
	 * @return the pixels
	 * @see #setRGB
	 */
	
	public int[] getRGB(BufferedImage image, int x, int y, int width,
			int height, int[] pixels) {
		int type = image.getType();
		if (type == BufferedImage.TYPE_INT_ARGB
				|| type == BufferedImage.TYPE_INT_RGB)
			return (int[]) image.getRaster().getDataElements(x, y, width,
					height, pixels);
		return image.getRGB(x, y, width, height, pixels, 0, width);
	}

	/**
	 * A convenience method for setting ARGB pixels in an image. This tries to
	 * avoid the performance penalty of BufferedImage.setRGB unmanaging the
	 * image.
	 * 
	 * @param image
	 *            a BufferedImage object
	 * @param x
	 *            the left edge of the pixel block
	 * @param y
	 *            the right edge of the pixel block
	 * @param width
	 *            the width of the pixel arry
	 * @param height
	 *            the height of the pixel arry
	 * @param pixels
	 *            the array of pixels to set
	 * @see #getRGB
	 */
	
	public void setRGB(BufferedImage image, int x, int y, int width,
			int height, int[] pixels) {
		int type = image.getType();
		if (type == BufferedImage.TYPE_INT_ARGB
				|| type == BufferedImage.TYPE_INT_RGB)
			image.getRaster().setDataElements(x, y, width, height, pixels);
		else
			image.setRGB(x, y, width, height, pixels, 0, width);
	}

	public Rectangle2D getBounds2D(BufferedImage src) {
		return src.getRaster().getBounds();
	}

	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		if (dstPt == null) {
			dstPt = new Point2D.Float();
		}
		dstPt.setLocation(srcPt.getX(), srcPt.getY());
		return dstPt;
	}

	public RenderingHints getRenderingHints() {
		// TODO Auto-generated method stub
		return null;
	}

	public static void initLookUpTable(InputStream lookup) throws Exception {

		BufferedReader in = null;
		if (lookup == null)
			lookupSize = 1024;
		else
			lookupSize = 256;
	
		colorLookup = new int[lookupSize];

		if (lookup != null) {
			in = new BufferedReader(new InputStreamReader(lookup));
			String line = in.readLine();
			if (Integer.parseInt(new StringTokenizer(line, " ,;").nextToken()) != lookupSize) {

				return;
			}
		}

		for (int i = 0; i < lookupSize; i++) {

			if (in != null) {
				StringTokenizer tok = new StringTokenizer(in.readLine(), " ,;");
				int r = (byte) ((Double.parseDouble(tok.nextToken()) * 255.));
				int g = (byte) ((Double.parseDouble(tok.nextToken()) * 255.));
				int b = (byte) ((Double.parseDouble(tok.nextToken()) * 255.));

				colorLookup[i] = alpha | r << 16 | g << 8 | b;
			} else {

				colorLookup[i] = spiralLUT(i, lookupSize, 0.5, 1.5, 1.2);
			}
		}

	}

	public static JMenu getColorMenu() {
		JMenu menu = new JMenu("Color Map");

		JMenuItem item;

		item = new JMenuItem("Grey");
		menu.add(item);
		item.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				zScaleTransformOp.doColorLookUp = false;
				try {
					zScaleTransformOp.initLookUpTable(null);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

		item = new JMenuItem("Heat");
		menu.add(item);
		item.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				zScaleTransformOp.doColorLookUp = true;
				try {
					zScaleTransformOp.initLookUpTable(null);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		return menu;

	}

	public static int spiralLUT(int value, int valueMax, double startColor,
			double rotations, double hue) {

		int rgb = 0;
		float normvalue = (float) (value * 1.0 / valueMax);
		float phi = (float) (2 * Math.PI * (startColor / 3. + rotations
				* normvalue));
		double a = hue * normvalue * (1 - normvalue) / 2;

		double cosPhi = Math.cos(phi);
		double sinPhi = Math.sin(phi);
		int r = (int) (255 * (normvalue + a
				* (-0.14861 * cosPhi + 1.78277 * sinPhi)));
		int g = (int) (255 * (normvalue + a
				* (-0.29227 * cosPhi - 0.90649 * sinPhi)));
		int b = (int) (255 * (normvalue + a * (1.97294 * cosPhi)));

		rgb = alpha | r << 16 | g << 8 | b;
		return rgb;

	}

	public static void main(String[] args) {

		BufferedImage inImage = new BufferedImage(500, 500,
				BufferedImage.TYPE_USHORT_GRAY);

		BufferedImageOp zScale = new zScaleTransformOp(1, 100, 1);
		BufferedImage outImage = zScale
				.createCompatibleDestImage(inImage, null);

		zScale.filter(inImage, outImage);

		System.exit(0);

	}

}
