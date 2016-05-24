package org.wiyn.guiUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import javax.swing.JFrame;

/**
 * An interface to illustrate the offset of a coordinate from it's initial
 * position.
 * 
 * @author harbeck
 * 
 */

public class MultiOffsetDisplay extends CooSysComponent {
	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	protected double xs[];
	protected double ys[];
	protected double deltaRotaion = 0;
	public double deltaRotaionAmp = 50;
	protected double angleLength = 1.;
	protected int size;

	Color Background = Color.black;
	Color CooSys = Color.white;
	Color Point = Color.yellow;

	public MultiOffsetDisplay(int dimX, int count) {
		this.updateSize(dimX, dimX);

		xs = new double[count];
		ys = new double[count];
		size = count;
		this.setBorders(2, 2, 2, 2);
		this.setBorders(2, 2, 2, 2);
		this.updateRange(-2, 2, -2, 2);
		this.initForSize();
	}

	public void updateRotation(double r) {
		this.deltaRotaion = r;
	}

	public void updateOffset(double x, double y, int which) {

		if (!this.lockData()) {

			return;
		}

		if (which < size && which >= 0) {
			xs[which] = x;
			ys[which] = y;
		}
		// if only one objects is displayed we paint ourselves, otherwise we
		// wait for an external invocation of repaint.

		this.releaseData();

		if (size == 1)
			this.invokeRepaint();

	}

	@Override
	protected void drawData(Graphics2D g2) {
		if (!this.lockData()) {

			return;
		}

		g2.setColor(Point);
		for (int i = 0; i < size; i++) {
			if (this.size > 1) {
				g2.setColor(GUIConsts.ChartColors[i % 4]);
			}
			this.drawBall(g2, (float) xs[i], (float) ys[i], 1.);
		}

		if (deltaRotaion != 0) {
			// draw arrow for rotation offset.
			g2.setColor(Color.cyan);
			double angle = (this.deltaRotaion * this.deltaRotaionAmp + 90)
					* Math.PI / 180.;
			double x = Math.cos(angle) * this.angleLength;
			double y = Math.sin(angle) * this.angleLength;
			this.drawLine(g2, xs[0], ys[0], xs[0] + x, ys[0] + y, null, null);
			this.drawBall(g2, (float) (xs[0] + x), (float) (ys[0] + y), 0.2);

		}
		this.releaseData();
	}

	protected void drawBall(Graphics2D g2, float X, float Y, double mag) {

		Point2D p1 = new Point2D.Double();
		Point2D p2 = new Point2D.Double();
		this.userToScreenCoordinates(X - 0.3 * mag, Y + 0.3 * mag, p1);
		this.userToScreenCoordinates(X + 0.3 * mag, Y - 0.3 * mag, p2);

		g2.fillOval((int) p1.getX(), (int) (p1.getY()),
				(int) Math.abs(p1.getX() - p2.getX()),
				(int) Math.abs(p1.getY() - p2.getY()));
	}

	public static void main(String[] args) {
		JFrame f = new JFrame();
		MultiOffsetDisplay d = new MultiOffsetDisplay(150, 150);
		f.getContentPane().add(d);
		f.pack();
		f.setVisible(true);
	}

}
