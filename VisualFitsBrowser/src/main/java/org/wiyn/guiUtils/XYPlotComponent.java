package org.wiyn.guiUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class XYPlotComponent extends CooSysComponent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    double good_x[];
    double good_v[];
    double bad_x[];
    double bad_v[];

    private double fitX;
    private double fitY;
    private double[] coefficients;

    public Color FitMarkerColor = new Color (0, 1f, 0, 0.8f);

    public XYPlotComponent(int drawX, int drawY, float minX, float maxX,
	    float minY, float maxY) {
	super ();
	this.CoosysBackground = Color.white;
	this.CooSysForeground = Color.black;
	this.CooSysGrid = Color.gray;
	CooSysNumberFont = new Font ("Helvetica", Font.ROMAN_BASELINE, 12);
	borderDown = 50;
	borderRight = 20;
	updateSize (drawX, drawY);
	this.minX = minX;
	this.maxX = maxX;
	this.minY = minY;
	this.maxY = maxY;

	this.initForSize ();

    }

    public void updateData (double[] good_x, double good_v[], double bad_x[],
	    double bad_v[], double[] coefficients, double fitX, double fitY) {

	if (!this.lockData ()) {

	    return;
	}

	this.good_x = good_x;
	this.good_v = good_v;
	this.bad_x = bad_x;
	this.bad_v = bad_v;
	if (coefficients != null) {
	    this.coefficients = new double[coefficients.length];
	    System.arraycopy (coefficients, 0, this.coefficients, 0,
		    coefficients.length);
	}

	this.fitX = fitX;
	this.fitY = fitY;
	this.releaseData ();

	this.invokeRepaint ();
    }

    @Override
    protected void drawData (Graphics2D g2) {
	if (!this.lockData ()) {

	    return;
	}

	g2.setStroke (new BasicStroke (1));
	// Good Data

	g2.setColor (Color.DARK_GRAY);
	if (good_x != null && good_v != null)
	    for (int ii = 0; ii < good_x.length; ii++) {
		this.drawBall (g2, (float) good_x[ii], (float) good_v[ii]);

	    }
	// bad Data
	g2.setStroke (new BasicStroke (2));
	g2.setColor (Color.red);
	if (bad_x != null && bad_v != null)
	    for (int ii = 0; ii < bad_x.length; ii++) {
		this.drawCross (g2, (float) bad_x[ii], (float) bad_v[ii]);

	    }

	// Draw the fit
	g2.setColor (Color.blue);
	g2.setStroke (new BasicStroke (2));
	Point2D p1 = new Point2D.Double ();
	Point2D p2 = new Point2D.Double ();

	double x0, x1, y0, y1;
	double step = (maxX - minX) / 50;
	x0 = minX;
	y0 = getValue (x0);
	for (x0 = this.minX; x0 < this.maxX - 1; x0 += step) {
	    x1 = x0 + step;

	    y1 = getValue (x1);

	    this.drawLine (g2, x0, y0, x1, y1, p1, p2);
	    y0 = y1;
	}

	g2.setColor (this.FitMarkerColor);
	this.drawFitMarker (g2, (float) fitX, (float) fitY);
	this.releaseData ();

    }

    protected double getValue (double x) {
	if (this.coefficients == null)
	    return 0.;
	double retval = 0;
	for (int ii = 0; ii < coefficients.length; ii++) {
	    retval += coefficients[ii] * Math.pow (x, ii);
	}
	return retval;
    }

    private void drawCross (Graphics2D g2, float f, float g) {
	Point2D p1 = new Point2D.Double ();
	int width = 5;

	this.userToScreenCoordinates (f, g, p1);
	int x = (int) p1.getX ();
	int y = (int) p1.getY ();
	g2.drawLine (x - width, y - width, x + width, y + width);
	g2.drawLine (x + width, y - width, x - width, y + width);
    }

    protected void drawFitMarker (Graphics2D g2, float X, float Y) {

	Point2D p1 = new Point2D.Double ();
	// Point2D p2 = new Point2D.Double ();

	int width = 10;

	this.userToScreenCoordinates (X, Y, p1);
	int x = (int) p1.getX ();
	int y = (int) p1.getY ();
	g2.setStroke (new BasicStroke (3));
	;
	g2.drawOval (x - width, y - width, width * 2 + 1, width * 2 + 1);

	g2.drawLine (x - width, y, x + width, y);
	g2.drawLine (x, y - width, x, y + width);

    }

    protected void drawBall (Graphics2D g2, float X, float Y) {

	int width = 3;
	Point2D p1 = new Point2D.Double ();

	this.userToScreenCoordinates (X, Y, p1);

	p1.setLocation (p1.getX () - width, p1.getY () - width);

	g2.fillOval ((int) p1.getX (), (int) (p1.getY ()), 2 * width + 1,
		2 * width + 1);
    }

}
