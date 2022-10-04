package org.cowjumping.guiUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A JComponent that displays a strip chart. This class is designed to be a
 * strip chart in a data display. It will store data in an internal FIFO buffer
 * and store (and display) up to a given number of data points simultaneously.
 * 
 * There are two display modes: plain and processed. In the plain mode, all
 * graphs wil be displayed, whereas in the processed mode the mean and upper
 * /lower limits will be displayed.
 * 
 */
@SuppressWarnings("serial")
public class StripChartComponent extends org.cowjumping.guiUtils.CooSysComponent
		implements MouseListener {

	/**
	 * Exposure State.
	 */
	protected boolean inExposure = false;

	private final static Logger myLogger =LogManager.getLogger(StripChartComponent.class);

	LinkedList<StripChartDataPoint> dataPoints = new LinkedList<StripChartDataPoint>();

	/**
	 * A buffer to enable smoothing of the incoming data points
	 * 
	 */
	LinearKalmanFilter myKalmanFilters[] = null;
	/**
	 * Internal flag indicating if data is to be smoothed.
	 * 
	 */
	boolean doSmoothing = false;

	final static Color MainLine = new Color(255, 255, 0);
	final static Color SideLine = new Color(0, 123, 167);

	boolean autozoomY_upperLimit = true;
	boolean autozoomY_lowerLimit = false;

	/* when the strip chart was begun in Date.getTime() msecs */
	long startTime = -1;

	/* last strip char addData update time in Date.getTime() msecs */
	long latestDataAdditionTime = System.currentTimeMillis();

	double min = Double.POSITIVE_INFINITY;
	double max = Double.NEGATIVE_INFINITY;

	/** by how far we increment the range when upon rescale */
	public double magStep = 0;

	double sum = 0;
	int num_data_points = 0;
	protected long timeBaseline = 20 * 1000;

	protected int lastX = 0;
	protected int lastY = 0;

	protected boolean drawMinMax = true;

	protected boolean updated = false;

	/**
	 * Default constructor Initialises the class with a defaul of 100 data
	 * points in the +/- 2 range
	 */
	public StripChartComponent(String ylabel, int dimX, int dimY) {

		this.yLabel = new String(ylabel);

		this.setXLabelMultiplier(1 / 1000.);
		this.updateSize(dimX, dimY);
		borderDown = 10;
		this.updateRange(timeBaseline, 0, 0, 1);

		addMouseListener(this);

	}

	/**
	 * Define how many milliseconds range should be displayed (and thus also
	 * buffered) by the stripchart
	 */

	public void setTimeBaseline(long timeBaseline) {

		this.timeBaseline = timeBaseline;
		this.updateRange(timeBaseline, 0, minY, maxY);
		initForSize();
		repaint();

	}

	public void exposureStart() {
		startTime = (new Date()).getTime();
		inExposure = true;
	}

	public void exposureEnd() {
		inExposure = false;
	}

	public boolean isExposure() {
		return inExposure;
	}

	public void setBuffering(boolean v) {

		this.doSmoothing = v;
	}

	private void initBuffers(int size) {

		myKalmanFilters = new LinearKalmanFilter[size];
		for (int ii = 0; ii < myKalmanFilters.length; ii++)
			myKalmanFilters[ii] = new LinearKalmanFilter();

	}

	public void addData(Long timeVal, StripChartDataPoint datum) {

		if (!this.lockData()) {

			if (myLogger.isDebugEnabled()) {

				myLogger.warn("Adding data to stripchart rejected : data locked; data has been lost.");

			}

			return;
		}

		if (myLogger.isDebugEnabled())
			myLogger.debug("Adding new data set to Stripchart " + this.yLabel);

		this.latestDataAdditionTime = timeVal;

		if (this.doSmoothing) {
			int datalength = datum.data.length;

			if (myKalmanFilters == null
					|| datum.data.length != myKalmanFilters.length) {

				initBuffers(datum.data.length);
			}

			for (int ii = 0; ii < myKalmanFilters.length; ii++) {

				double filteredVal = myKalmanFilters[ii].addValue(
						datum.data[ii],
						Math.sqrt(Math.abs(datum.data[ii]) + 0.1), false);
				datum.data[ii] = filteredVal;

			}
		}

		// Add the data to the internal array; this could be now a filtered or
		// unfiltered version.

		dataPoints.addLast(datum);

		/* Autoscaling if we exceed the current boundaries */

		if (autozoomY_upperLimit) {
			double maxVal = datum.getMaxValue();
			if (maxVal > maxY) {
				maxY = maxVal;
				this.initForSize();

			}
		}
		if (autozoomY_lowerLimit) {
			double minVal = datum.getMinValue();
			if (minVal < minY) {
				minY = minVal;
				this.initForSize();

			}
		}
		this.releaseData();

		this.invokeRepaint();

		if (myLogger.isDebugEnabled())
			myLogger.debug("Updates stripchaet data set.");
	}

	public void addData(Long timeVal, double[] newData, boolean process) {

		StripChartDataPoint datum = new StripChartDataPoint(timeVal, newData,
				process);

		this.addData(timeVal, datum);

	}

	public void addData(Long timeVal, Double minVal, Double avgVal,
			Double maxVal) {

		StripChartDataPoint datum = new StripChartDataPoint(timeVal, minVal,
				maxVal, avgVal);

		this.addData(timeVal, datum);

	}

	private void discardOldData(Long timeVal) {
		while (dataPoints.size() > 0
				&& dataPoints.getFirst().getTimeStamp() < timeVal
						- timeBaseline) {

			dataPoints.removeFirst();

		}
	}

	public boolean isOpaque() {
		return false;
	}



	protected void drawData(Graphics2D g) {
		double t1 = 0, t2 = 0;

		// System.err.println("Drawing lines now");
		g.setClip(this.borderLeft, borderUp, drawX - borderLeft - borderRight,
				drawY - borderUp - borderDown);

		if (!this.lockData()) {

			return;
		}
		// System.err.println("Enter painting");
		long now = this.latestDataAdditionTime;

		this.discardOldData(now);
		if (dataPoints != null) {

			Iterator<StripChartDataPoint> dataIter = dataPoints.iterator();

			if (dataIter.hasNext() == false) {
				// System.err.println ("[Stripchart] No data to display,
				// returning");
				this.releaseData();
				return;
			}
			// Search starting data
			StripChartDataPoint initPoint = null;
			while (dataIter.hasNext()) {
				initPoint = dataIter.next();
				t1 = now - initPoint.getTimeStamp();

				if (!(t1 > Math.abs(rangeX)))
					break;
			}
			if (initPoint == null)
				return;

			StripChartDataPoint oldPosition = new StripChartDataPoint(initPoint);

			Point2D p1 = new Point2D.Double(0, 0);
			Point2D p2 = new Point2D.Double(0, 0);
			int nLines = initPoint.getDimen();

			while (dataIter.hasNext()) {

				StripChartDataPoint datum = dataIter.next();

				t2 = now - datum.getTimeStamp();

				for (int ii = 0; ii < nLines; ii++) {

					g.setColor(GUIConsts.ChartColors[ii % 4]);
					this.drawLine(g, t1, oldPosition.getDatum(ii), t2,
							datum.getDatum(ii), p1, p2);

				}
				t1 = t2;
				oldPosition.set(datum);
			}

		}
		this.releaseData();

	}

	public Dimension getMinimumSize() {
		return new Dimension(drawX, drawY);
	}

	public Dimension getPreferredSize() {
		return getMinimumSize();
	}

	public Dimension getMaximumSize() {
		return getMinimumSize();
	}

	private void autoscaleY() {

		if (this.autozoomY_lowerLimit == false
				&& this.autozoomY_upperLimit == false)
			return;

		if (!this.lockData()) {

			return;
		}

		Iterator<StripChartDataPoint> dataIter = dataPoints.iterator();
		double min, max;

		if (dataIter.hasNext()) {
			StripChartDataPoint d = dataIter.next();
			min = d.getMinValue();
			max = d.getMaxValue();

			while (dataIter.hasNext()) {
				d = dataIter.next();
				double dmin = d.getMinValue();
				double dmax = d.getMaxValue();
				if (dmax > max && dmax != Double.NaN)
					max = dmax;
				if (dmin < min && dmin != Double.NaN)
					min = dmin;

			}

			if (min != max) {
				if (this.autozoomY_upperLimit)
					this.maxY = max;
				if (this.autozoomY_lowerLimit)
					this.minY = min;
				this.initForSize();
			}

		}

		this.releaseData();

	}

	//
	//
	// MouseListener Interface
	//
	//

	public void mouseClicked(MouseEvent e) {

		int whereY = e.getY();
		int whereX = e.getX();
		int count = e.getClickCount();

		if (count == 1 && whereX > this.borderLeft) {
			double quadrant = (whereY - borderUp) / (drawY - 2 * borderUp);
			int quad = (int) (quadrant * 4);
			// System.err.println ("Mouse Clicked in quadrant:" + quad);

			switch (quad) {

			case 0:
				// System.err.println ("Increase upper limit");
				maxY += this.magStep;
				break;

			case 1:
				// System.err.println ("Decrease upper limit");
				if (maxY - magStep > minY)
					maxY -= magStep;
				break;

			case 2:
				if (minY + magStep < maxY)
					minY += magStep;
				break;

			case 3:
				minY -= magStep;
				break;

			}
			initForSize();
		}

		if (count == 2 && whereX < this.borderLeft) {

			this.autoscaleY();

		}
		this.invokeRepaint();

	}

	public void mouseEntered(MouseEvent e) {

	}

	public void mouseExited(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {

	}

	public void mouseReleased(MouseEvent e) {

	}

	/**
	 * Define if the lower or upper limit on the Y axis should be automatically
	 * adjusted.
	 * 
	 * @param lower
	 *            flag if lower limit should be adjusted.
	 * @param upper
	 *            flag if upper limit should be adjusted.
	 */
	public void setAutozoomY(boolean lower, boolean upper) {
		this.autozoomY_upperLimit = upper;
		this.autozoomY_lowerLimit = lower;
	}

	public static void main(String[] args) {

		JFrame myFrame = new JFrame("Stripchart test");
		StripChartComponent myChart = new StripChartComponent("y-LABEL", 500,
				128);
		myChart.setBuffering(true);
		myFrame.getContentPane().add(myChart);

		myFrame.pack();
		myFrame.setVisible(true);

		for (double ii = 0; ii < 100000; ii++) {
			double v = 1 + 1 * Math.sin(ii * 3.14 / 18);

			myChart.addData(System.currentTimeMillis(), v - 0.5, v, v + 0.5);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}

		}

	}

}

class LinearKalmanFilter {

	double currentX = 0;
	double currentVar = 0;

	final static Logger myLogger = LogManager.getLogger(LinearKalmanFilter.class);

	public LinearKalmanFilter() {

	}

	public double addValue(double value, double variance, boolean reset) {

		double K = 1;
		if (reset) {

			currentX = value;
			currentVar = variance;
			K = 0;

		} else {

			K = currentVar != 0 ? currentVar / (currentVar + variance) : 1;
			if (K < 0.00001)
				K = 0.00001;
			if (K > 1)
				K = 1;

		}

		currentX = currentX + K * (value - currentX);
		currentVar = (5 * currentVar + variance) / 6 / 2;

		if (myLogger.isDebugEnabled())
			myLogger.debug(String
					.format("Kalman add value: % 8.3f % 8.3f  new State is % 8.3f % 8.3f % 5.2f",
							value, variance, currentX, currentVar, K));
		return currentX;

	}
}
