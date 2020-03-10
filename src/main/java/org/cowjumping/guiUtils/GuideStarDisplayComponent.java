package org.cowjumping.guiUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cowjumping.FitsUtils.ImageContainer;
import org.cowjumping.FitsUtils.odiCentroidSupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.Vector;

/**
 * A component that is designed to draw guide star video data, with an overlay
 * of a mean central position and a current position.
 * 
 * Data are internally stored in an unsigned short BufferedImage. New video
 * images are accepted as either short[] or float[] arrays. Other data types can
 * easily be added.
 * 
 * The class can also mark other data points in the video.
 * 
 * @author Daniel Harbeck
 * 
 */

@SuppressWarnings("serial")
public class GuideStarDisplayComponent extends
		org.cowjumping.guiUtils.CooSysComponent implements MouseMotionListener,
		zScaleListener {

	private final static Logger myLogger = LogManager.getLogger();

	public ImageContainer gs = null;

	/** Internal buffer for the raw image */
	public BufferedImage RawImage = null;
	/** Internal image for a z-scaled (i.e., luminosity cut applied) image */
	protected BufferedImage zScaledImage = null;

	/** Container for <x,y> location of object markers */
	Vector<Point2D.Float> markers = null;

	/** Size of the image in X dimension */
	protected int imageX = 0;
	/** Size of the image in Y dimension */
	protected int imageY = 0;

	// public GuideStarContainer gs = null;
	/** Image rescaling parameter: scaling of the image */
	float zscale = 1; // data scaling parameters
	float zoffset = 0;

	float z1 = 0;
	float z2 = 1;

	/** Data value used for auto set bias */
	float zExtraOffset = 0;

    public void setMyZscaleSelector(ZScaleSelectorComponent myZscaleSelector) {
        this.myZscaleSelector = myZscaleSelector;
    }

    ZScaleSelectorComponent myZscaleSelector = null;

	/** marker for the current location of the guide star */
	protected Point2D currentCenter = new Point2D.Float();
	Point2D centerOnScreen = new Point2D.Float();
	/** marker for the mean (or intial) position of the guide star */
	protected Point2D meanCenter = new Point2D.Float();

	private float fwhm = 0;
	float flux = 0;
	static final int SHORTMAX = Short.MAX_VALUE - 1;

	/** Image Operator to do z-scaling of the image */
	private zScaleTransformOp myZScaleOp = null;
	/**
	 * Image Operator to expand/shrink the guide star image onto the component's
	 * area
	 */
	private AffineTransformOp myMagnification = null; // magnifying the image

	/** Color of the current position marker */
	public final static Color MarkerColor_Good = Color.green;
	public final static Color MarkerColor_Bad = Color.orange;
	private BufferedImage lookupImage;

	public double DonutRadius = 0;

	public GuideStarDisplayComponent() {
		this(150, 150, 0, SHORTMAX);
	}

	public GuideStarDisplayComponent(int displaySizeX, int displaySizeY,
			int z1, int z2) {

		this.drawsCooSys = false;
		setZScale(z1, z2);

		this.setBorders(5, 5, 5, 5);
		this.updateSize(displaySizeX, displaySizeY);
		this.updateRange(0, 10, 0, 10);
		this.initForSize();

		addMouseMotionListener(this);

	}

	/**
	 * Check if a requested image size is compatible with the currently
	 * allocated buffers. If it is not, reallocate the internal buffers.
	 * 
	 * @param imageX
	 *            requested Image width.
	 * @param imageY
	 *            requested Image height.
	 */
	protected void updateImageSize(int imageX, int imageY) {

		boolean update = (RawImage == null || imageX != this.imageX || imageY != this.imageY);
		this.imageX = imageX;
		this.imageY = imageY;

		minX = 0;
		minY = 0;
		maxX = imageX;
		maxY = imageY;

		if (update)
			initForSize();
	}

	/**
	 * Initialize the component to display images of a new size.
	 * 
	 * This is always called if either the screen size or the iamge size is
	 * changed
	 * 
	 */
	public void initForSize() {

		// this will take care of internal buffers and the AffineTransformation
		// classes used.
		super.initForSize();

		// Here we have to take special precautions since we have an image
		// buffer and an image manipulation chain set up. All that information
		// needs to be updated if we change any size.

		if (imageX > 0 && imageY > 0) {
			// Allocating buffers makes only sense if the image size is >0. It
			// can be ==0 if there wasn't a proper initialisation yet
			if (RawImage != null) {
				RawImage = null;
			}

			RawImage = new BufferedImage(imageX, imageY,
					BufferedImage.TYPE_USHORT_GRAY);

			// Recalculate the image magnification operators

			if (forwardTransform != null && myZScaleOp != null) {
				// the magnification is based on the affine transform we use
				myMagnification = null;
				myMagnification = new AffineTransformOp(forwardTransform,
						AffineTransformOp.TYPE_BILINEAR);

				zScaledImage = null;
				zScaledImage = myZScaleOp.createCompatibleDestImage(RawImage,
						RawImage.getColorModel());

			} else
				myMagnification = null;
		}
	}

	public void updateObjectCenter(float centerX, float centerY, float fwhm) {
		currentCenter.setLocation(centerX, centerY);
		this.setFwhm(fwhm);

	}

	public void setMeanCenter(float x, float y) {
		this.meanCenter.setLocation(x, y);
	}

	public void clear() {
		clearMarkers();

		this.updateObjectCenter(0, 0, 0);
		this.RawImage = null;
		this.invokeRepaint();

	}

	/**
	 * Add a marker to the image The guide star display has the capability to
	 * draw markers on, e.g., objects.
	 * 

	 */
	public void addMarker(float f, float g) {
		if (markers == null)
			markers = new Vector<Point2D.Float>();
		markers.add(new Point2D.Float(f, g));
	}

	/**
	 * Clear all markers in the Image
	 * 
	 */
	public void clearMarkers() {
		if (this.markers != null)
			this.markers.clear();
		this.markers = null;
	}

	/**
	 * Override the inherited background image The super class would draw a
	 * coordinate system. Here we draw the image of the guide star.
	 * 
	 */
	protected void drawBackGround(Graphics2D g2) {

		clearBackground(g2);
		if (RawImage == null || forwardTransform == null) {
			// there is no image loaded yet,or we are not properly initialized

			g2.setColor(Color.black);
			g2.fillRect(0, 0, drawX, drawY);
			g2.setColor(Color.red);
			g2.drawLine(0, 0, drawX, drawY);
			g2.drawLine(drawX, 0, 0, drawY);
			return;
		}

		myZScaleOp.filter(RawImage, zScaledImage);

		// do magnification & draw

		g2.drawImage(zScaledImage, myMagnification, 0, 0);
		// g2.drawImage (zScaledImage, null, 0, 0);

	}

	/**
	 * Draw additional meta-information on top of the image
	 * 
	 */

	public void drawData(Graphics2D g) {

		if (forwardTransform == null)
			return;

		g.setColor(this.MarkerColor_Good);

		g.setColor(this.MarkerColor_Good);
		// draw the current center
		if (currentCenter.getX() > 0 && currentCenter.getY() > 0) {
			centerOnScreen = forwardTransform.transform(currentCenter, null);



			float markerOuterDiameter = 20;

			if (DonutRadius > 0) {
				Point2D calcPoint = forwardTransform.transform(
						new Point2D.Double(DonutRadius, 0), null);
				markerOuterDiameter = (int) calcPoint.getX() * 2;
			}

			g.drawOval(
					(int) (centerOnScreen.getX() - markerOuterDiameter / 2. ),
					(int) (centerOnScreen.getY() - markerOuterDiameter / 2. ),
					(int) (markerOuterDiameter+1), (int)(markerOuterDiameter+1));



		}

		// draw object markers

		if (markers != null) {
			g.setColor(Color.magenta);
			for (java.util.Iterator<Point2D.Float> it = markers.iterator(); it
					.hasNext();) {
				Point2D.Float p = it.next();
				Point2D p1 = new Point2D.Double(p.x, p.y);
				forwardTransform.transform(p1, p1);
				((Graphics2D) g).drawOval((int) (p1.getX() - 4 + 0.5),
						(int) (p1.getY() - 4 + 0.5), 8, 8);
			}
		}


		if (this.meanCenter.getX() > 0 && this.meanCenter.getY() > 0) {
			drawMeanCenter(g, meanCenter.getX(), meanCenter.getY(), 2);

		}

	}

	public void drawMeanCenter(Graphics2D g2, double x, double y, double extent) {
		g2.setColor(Color.red);

		// draw the mean center
		Point2D.Double p1 = new Point2D.Double();
		Point2D.Double p2 = new Point2D.Double();
		this.drawLine(g2, x - extent, y, x - extent / 2, y, p1, p2);
		this.drawLine(g2, x + extent / 2, y, x + extent, y, p1, p2);

		this.drawLine(g2, x, y - extent, x, y - extent / 2, p1, p2);
		this.drawLine(g2, x, y + extent / 2, x, y + extent, p1, p2);
	}

	// ///////////////////////////////
	// /// Image update interfaces

	/**
	 * Update the image to be displayed.
	 * 
	 * @param data
	 *            an array of floats or shorts
	 * 
	 * @param imageX
	 * @param imageY
	 * @param centerX
	 * @param centerY
	 * @param fwhm
	 */
	public void updateImage(Object data, int imageX, int imageY, float centerX,
			float centerY, float fwhm) {

		if (!this.lockData()) {
			myLogger.warn("GuideStarDisplay is busy; ignoring image update.");
			SoundSignal.fail2();
			return;
		}

		if (data == null)
			return;

		if (this.RawImage == null || this.imageX != imageX
				|| this.imageY != imageY)
			updateImageSize(imageX, imageY);

		updateObjectCenter(centerX, centerY, fwhm);

		if (data.getClass().getComponentType() == short.class)
			updateImage((short[]) data, imageX, imageY);

		else if (data.getClass().getComponentType() == float.class)

			updateImage((float[]) data, imageX, imageY);

		else
			myLogger.error("updateImage: Guide star display: Unsupported data type");

		this.releaseData();
		this.invokeRepaint();
	}

	/**
	 * Update the image to be displayed from a buffer of unsigned shorts
	 * 
	 * @param data
	 *            Array of shorts[] that will be interpreted as ushorts.
	 * @param imageX
	 *            Image dimension in X
	 * @param imageY
	 *            Image dimension in Y
	 */
	private void updateImage(short[] data, int imageX, int imageY) {

		WritableRaster ras = RawImage.getRaster();
		ras.setDataElements(0, 0, imageX, imageY, data);

	}

	/**
	 * Update the image dot be displayed from a buffer of floats
	 * 
	 * @param data
	 * @param imageX
	 * @param imageY
	 */

	private void updateImage(float[] data, int imageX, int imageY) {
		DataBuffer db = RawImage.getRaster().getDataBuffer();

		// convert pixel by pixel to shorts. Not that fastest way of doing this!
		for (int ii = 0; ii < imageX * imageY; ii++)

			db.setElemFloat(ii, data[ii]);
	}

	/**
	 * Modify the z-scaling of the images.
	 * 
	 * This procedure will need to reset the internal ReScaleOp image operator.
	 * 
	 * @param z1
	 * @param z2
	 */
	public void setZScale(float z1, float z2) {

		this.z1 = z1;
		this.z2 = z2;
		updateScaleing();

		if (this.myZscaleSelector != null)
		    myZscaleSelector.setZScale((int) z1, (int) z2,false);

	}

	protected void updateScaleing() {

		this.myZScaleOp = new zScaleTransformOp(z1, z2, 1);
		this.invokeRepaint();

	}

	public void mouseDragged(MouseEvent e) {
		// Intentionally left blank

	}

	/**
	 * Provide a method to display the actual image value at the mouse location
	 * 
	 */
	public void mouseMoved(MouseEvent e) {

		Point p = e.getPoint();
		Point2D P = new Point2D.Float(p.x, p.y);
		if (backwardTransform == null && RawImage != null)
			return;

		backwardTransform.transform(P, P);

		int x = (int) P.getX();
		int y = (int) P.getY();

		if (x < 0 || x >= imageX || y < 0 || y >= imageY)
			return;
		int val = 0;
		if (RawImage != null)
			val = (int) (RawImage.getRaster().getSample(x, y, 0) & 0xffff);



	}

	public float getZExtraOffset() {
		return zExtraOffset;
	}

	public void setZExtraOffset(float extraOffset) {
		zExtraOffset = extraOffset;
	}

	public void autoSetExtraZOffset(boolean set) {

		float[] outData = null;
		// centroidResults results = new centroidResults ();

		if (set == false || this.RawImage == null) {
			this.zExtraOffset = 0;

		} else {

			DataBuffer db = this.RawImage.getRaster().getDataBuffer();
			outData = new float[RawImage.getWidth() * RawImage.getHeight()];

			for (int ii = 0; ii < outData.length; ii++) {
				outData[ii] = (short) Math.round(db.getElemFloat(ii));
			}

			if (outData == null) {
				myLogger.warn("autoSetExtraZOffset: got a null pointer from RawImage data!");
				return;
			}

			ImageContainer tempgs = new ImageContainer();
			tempgs.setImage(outData, RawImage.getWidth(), RawImage.getHeight());
			odiCentroidSupport.findSkyandPeak(tempgs, 3, 1);

			this.zExtraOffset = tempgs.getBackground() - 50;

		}
		if (myLogger.isDebugEnabled())
			myLogger.debug("Extra Offset set to: " + this.zExtraOffset);

		this.updateScaleing();
	}

	public void setAutoBias(boolean b) {
		// TODO Auto-generated method stub

	}

	public float getFlux() {
		return flux;
	}

	public void setFlux(float flux) {
		this.flux = flux;
	}

	public void setFwhm(float fwhm) {
		this.fwhm = fwhm;
	}

	public float getFwhm() {
		return fwhm;
	}

}

class GSToolTip extends JToolTip {

	RadialPlotComponent rPlot = null;

	public GSToolTip() {

		rPlot = new RadialPlotComponent(100, 100);
		this.add(rPlot);
	}

	public void update(ImageContainer gs, Object image, int dimX, int dimY) {
		if (gs.rawImageBuffer == null && image != null)
			gs.rawImageBuffer = (float[]) image;
		rPlot.updateData(gs);

	}

}
