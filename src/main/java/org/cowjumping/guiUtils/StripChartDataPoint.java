package org.cowjumping.guiUtils;

import java.util.LinkedList;

/**
 * 
 * Provide a data entry in a strip chart.
 * 
 * A strip chart data point consists of a time stamp and an array of double[].
 * The data can be interpreted in two ways: processed or unprocessed. In the
 * processed case the data will be interpreted as an average, minimum and
 * maximum value. In the unprocessed case the data represent stripcharts of
 * several sources, where the index in the data refers distinguishes between
 * data sources.
 * 
 * @author harbeck
 * 
 */
public class StripChartDataPoint {
    protected long timeStamp;
    /**
     * The data array will either hold a collection of raw data, or an
     * average/min/max of an ensemble
     */
    protected double[] data = null;

    /** Flag if the data is processed (min/max/avg) or raw (severeal lines) */
    boolean processed = true;

    boolean toBeProcessed = false;

    public StripChartDataPoint(long timeStamp, double minValue,
	    double maxValue, double averageValue) {
	super ();

	data = new double[3];
	this.setTimeStamp (timeStamp);
	this.setMinValue (minValue);
	this.setMaxValue (maxValue);
	this.setAverageValue (averageValue);
    }

    /**
     * Copy a data into a new data point
     * 
     * @param o
     */
    public StripChartDataPoint(StripChartDataPoint o) {
	super ();
	this.set (o);

    }

    public StripChartDataPoint(long timeStamp, double[] data, boolean process) {
	super ();
	this.setTimeStamp (timeStamp);
	// TODO: verify if we do not have to copy the data here!

	if (process == false) {

	    // this.data = new double[data.length];
	    // System.arraycopy (data, 0, this.data, 0, data.length);
	    this.data = data;
	    processed = false;

	} else {

	    processData (data);
	    processed = true;
	    // process data!
	}
    }

    public void set (StripChartDataPoint o) {
	this.timeStamp = o.timeStamp;
	if (data == null || data.length != o.data.length)
	    this.data = new double[o.data.length];
	System.arraycopy (o.data, 0, this.data, 0, o.data.length);
    }

    /**
     * find min.max.average values of a data array s
     * 
     * @param data
     */
    private void processData (double[] data) {

	if (data == null)
	    return;
	double min = data[0];
	double max = data[0];
	double avg = data[0];
	for (int ii = 1; ii < data.length; ii++) {
	    if (data[ii] < min)
		min = data[ii];
	    if (data[ii] > max)
		max = data[ii];
	    avg += data[ii];

	}
	avg /= data.length;
	this.data = new double[3];
	this.setMaxValue (max);
	this.setMinValue (min);
	this.setAverageValue (avg);
	this.processed = true;
	this.toBeProcessed = false;
    }

    public double getDatum (int index) {
	if (data != null && index < data.length)
	    return data[index];
	else
	    return 0;
    }

    public int getDimen () {
	return data.length;
    }

    public long getTimeStamp () {
	return timeStamp;
    }

    public void setTimeStamp (long timeStamp) {
	this.timeStamp = timeStamp;
    }

    /**
     * return the Minimum value of the data point.
     * 
     * If this data point is processed, the stored minimum value is returned.
     * Otherwise the Minimum is searched in the data array.
     * 
     * @return
     */
    public double getMinValue () {
	if (processed)
	    return data[0];
	else
	    return getMin ();
    }

    public void setMinValue (double minValue) {

	data[0] = minValue;
    }

    /**
     * Return the Maximum value of the data point.
     * 
     * If the data point is processed, the stored maximum value is returned.
     * Otherwise, the maximum value is searched for.
     * 
     * @return
     */
    public double getMaxValue () {
	if (processed)
	    return data[1];
	else
	    return getMax ();
    }

    public void setMaxValue (double maxValue) {
	data[1] = maxValue;
    }

    public double getAverageValue () {
	return data[2];
    }

    public void setAverageValue (double averageValue) {
	data[2] = averageValue;
    }

    private double getMax () {

	double max = data.length > 0 ? data[0] : 0;
	for (int ii = 0; ii < data.length; ii++)
	    if (data[ii] > max)
		max = data[ii];
	return max;

    }

    private double getMin () {
	double min = data.length > 0 ? data[0] : 0;
	for (int ii = 0; ii < data.length; ii++)
	    if (data[ii] < min)
		min = data[ii];
	return min;

    }

}

/**
 * Poor man's fifo buffer implementaion with the ability to get the average of
 * the values.
 * 
 * @author harbeck
 * 
 */
class doubleBuffer {

    LinkedList<Double> buffer;

    int size = 0;

    public doubleBuffer(int n) {

	this.buffer = new LinkedList<Double> ();
	size = n;

    }

    /** add a double value to buffer and discard old value */

    public void addDouble (double v) {

	this.buffer.add (v);
	while (buffer.size () > size) {
	    buffer.poll ();

	}
    }

    public double getAverage () {
	double retVal = 0;
	double count = 0;
	if (buffer.size () == 0)
	    return 0;
	for (int ii = 0; ii < buffer.size (); ii++) {
	    retVal += buffer.get (ii);
	    count++;
	}

	return retVal / count;
    }

}
