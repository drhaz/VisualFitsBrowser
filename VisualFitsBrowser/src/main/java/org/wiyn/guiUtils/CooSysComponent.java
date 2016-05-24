package org.wiyn.guiUtils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

/**
 * A class to support the usage of an internal coordinate system on a
 * JComponent. This method supports rendering in its own thread.
 * 
 * @author Daniel Harbeck
 * 
 */

public abstract class CooSysComponent extends javax.swing.JComponent implements
    Runnable {

  /**
     * 
     */
  private final static long serialVersionUID = 1L;
  private final static Logger myLogger = Logger
      .getLogger(CooSysComponent.class);

  /** The size of this widget in x-pixels */
  protected int drawX = 150;
  /** The size of this widget in y-pixels */
  protected int drawY = 150;
  /** Internal left border to the drawable area */
  protected int borderLeft = 50;
  /** Internal right border to the drawable area */
  protected int borderRight = 5;
  /** Internal upper border to the drawable area */
  protected int borderUp = 5;
  /** Internal lower border to the drawable area */
  protected int borderDown = 10;

  /** minimum X of the internal coordinate system */
  protected double minX = 0;
  /** maximum X of the internal coordinate system */
  protected double maxX = 0;
  /** minimum Y of the internal coordinate system */
  protected double minY = 0;
  /** maximum Y of the internal coordinate system */
  protected double maxY = 0;
  /** Derived quantity: maxX-minX */
  protected double rangeX = 0;
  /** Derived quantity: maxY - minY */
  protected double rangeY = 0;

  /** The linear transformtion from user coordinate to widget coordinate space */
  protected AffineTransform forwardTransform = null;
  /**
   * linear transformation from widget coordinate space to user coordiante space
   */
  protected AffineTransform backwardTransform = null;

  /**
   * The coordinate system background image, which is calculated upon
   * Initialization
   */
  protected Image CooSysImage = null;
  /** Target image into which the the widget is being rendered before painting */
  protected Image offScreenImage = null;

  /** Label for the X-axis */
  protected String xLabel;
  /** Label for the Y-axis */
  protected String yLabel;
  /** flag whether to draw a x-grid in the coordinate system */
  protected boolean drawXgrid = true;
  /** flag whether to draw a y-grid in the coordinate system */
  protected boolean drawYgrid = true;
  /** TBD */
  private double xLabelMultiplier = 1;
  /** Background color of the coordinate system (and with it, the widget) */
  protected Color CoosysBackground = new Color(0.05f, 0.05f, 0.05f, 1.f);
  /** Foreground color of the coordinate system */
  protected Color CooSysForeground = Color.white;
  /** Color of the coordinate system grid */
  protected Color CooSysGrid = new Color(50, 50, 50);

  /** Font to be used for the Coordiante system axis labels */
  protected static Font CooSysLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN,
      14);

  /** Font to be used for the cooridnate system numbers */
  protected static Font CooSysNumberFont = new Font(Font.SANS_SERIF,
      Font.PLAIN, 10);

  /** internal formatter for decimal numbers */
  protected DecimalFormat myNumberFormat = new DecimalFormat();

  protected boolean drawsCooSys = true;

  /**
   * flag if the widget runs in threaded mode. This is used in the rendereing
   * process
   */
  protected boolean threadedMode = false;

  /**
   * Timestamp of the last rendering request. This is used in threaded mode to
   * prevent flickering
   */
  private long lastRenderRequest = System.nanoTime();;

  /**
   * Set the size of this widget. Warning: no reinitiatlisation is done!
   * 
   * @param drawX
   * @param drawY
   */
  protected void updateSize(int drawX, int drawY) {

    this.drawX = drawX;
    this.drawY = drawY;

  }

  /**
   * Set the internal border variables.
   * 
   * Warning: no reinitialisation is done.
   * 
   * @param left
   * @param right
   * @param up
   * @param down
   */
  public void setBorders(int left, int right, int up, int down) {
    borderLeft = left;
    borderRight = right;
    borderUp = up;
    borderDown = down;

  }

  public void setYLimits(double ymin, double ymax) {

    updateRange(minX, maxX, ymin, ymax);
  }

  /**
   * Reset the user coordinate space.
   * 
   * The widget is reset to the new coordinate system.
   * 
   * @param minX
   * @param maxX
   * @param minY
   * @param maxY
   */

  public void updateRange(double minX, double maxX, double minY, double maxY) {
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;

    if (myLogger.isDebugEnabled())
      myLogger.debug("Updating range: " + minX + " -> " + maxX + " : " + minY
          + " -> " + maxY);

    initForSize();
  }

  /**
   * same as initForSize()
   * 
   */
  public void reset() {
    initForSize();
  }

  /**
   * Clear the data content of this widget.
   * 
   * For this class, nothing is done. For actual functionality, this class is to
   * be overwritten.
   */

  public void clear() {

  }

  /**
   * Initialize this widget for the actual widget's size, internal boundaries,
   * and the user coordinate system
   * 
   */
  public void initForSize() {

    // Do a sanity check on the input values
    if (minX == Double.NaN || maxX == Double.NaN) {
      minX = 0;
      maxX = 1;
    }

    if (minY == Double.NaN || maxY == Double.NaN) {
      minY = 0;
      maxY = 1;
    }

    // update the coordinate transformations.
    this.updateTransformation();

    // Get a new Coordiante system image
    CooSysImage = createVolatileImage(drawX, drawY);
    if (CooSysImage == null)
      CooSysImage = new BufferedImage(drawX, drawY, BufferedImage.TYPE_INT_ARGB);

    // Get a new double-buffering image
    offScreenImage = createVolatileImage(drawX, drawY);
    if (offScreenImage == null)
      offScreenImage = new BufferedImage(drawX, drawY,
          BufferedImage.TYPE_INT_ARGB);

    // Create a new coordinate system.
    if (CooSysImage != null && this.drawsCooSys) {
      Graphics2D g2 = (Graphics2D) CooSysImage.getGraphics();
      this.drawCooSys(g2, drawX, drawY);
    }
  }

  /**
   * Renders the widget by invoking the functions drawBackGround and drawData. *
   */

  public void render() {
    lastRenderRequest = System.nanoTime();
    if (offScreenImage == null)
      initForSize();
    Graphics2D g2 = (Graphics2D) offScreenImage.getGraphics();
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    drawBackGround(g2);
    drawData(g2);
  }

  /**
   * Try to acquire a lock on data that this class might harbour. This is a
   * non-blocking function.
   * 
   * @return flag if a lock was acquired. true if locked, false otherwise.
   */
  synchronized public boolean lockData() {
    boolean retval = true;

    if (dataLock == null)
      dataLock = new Semaphore(1);

    retval = this.dataLock.tryAcquire();

    // if (!retval)
    // System.err.println(this.getClass().getCanonicalName()
    // + ": Could not add data: busy");
    return retval;
  }

  /**
   * Release the lock on data this class might display.
   * 
   */

  synchronized public void releaseData() {
    if (dataLock != null)
      this.dataLock.release();
  }

  /**
   * Request a repaint of this component. This function should be called in
   * order to account for the threaded capability of this class.
   * 
   */

  public void invokeRepaint() {
    if (renderTrigger == null) {
      repaint();
    } else {
      renderTrigger.release();
    }

  }

  /**
   * modified paint function to accomodate a threaded mode.
   * 
   */

  public void paint(Graphics g) {
    // if we are not running in the threaded mode, we have to render in the
    // paint() procedure. However, if we run threaded, we must not call
    // render
    // again, or we will do the job twice :-(

    if (renderTrigger == null)
      render();

    if (offScreenImage != null)
      g.drawImage(offScreenImage, 0, 0, null);

  }

  /**
   * The most important interface to use this class: draw data on the screen. In
   * order to do something useful (i.e., paint more than a coordinate system),
   * this class needs to be overwritten.
   * 
   * 
   * @param g2
   */
  protected abstract void drawData(Graphics2D g2);

  /**
   * Draw the background of the component. This is defaulted to draw the
   * coordinate system. If a different background (e.g., a guide star image) is
   * desired, this calss can be overwritten.
   * 
   * @param g2
   */

  protected void drawBackGround(Graphics2D g2) {
    if (this.drawsCooSys && this.CooSysImage != null)
      g2.drawImage(this.CooSysImage, 0, 0, null);
  }

  /**
   * Transform a location from user coordiantes to component's pixel space.
   * 
   * @param x
   *          input x location.
   * @param y
   *          input y location.
   * @param p
   *          Transformed lcoation of the point. Can be null.
   * @return Transformed location of the point.
   */

  protected Point2D userToScreenCoordinates(double x, double y, Point2D p) {

    if (p == null)
      p = new Point2D.Double(x, y);
    else
      p.setLocation(x, y);

    if (forwardTransform == null) {

      return null;
    }
    this.forwardTransform.transform(p, p);

    return p;

  }

  protected Point2D ScreenToUserCoordinates(double x, double y, Point2D p) {

    if (p == null)
      p = new Point2D.Double(x, y);
    else
      p.setLocation(x, y);

    this.backwardTransform.transform(p, p);

    return p;

  }

  /**
   * Paint a line in the widgets in user space.
   * 
   * @param g
   * @param x1
   * @param y1
   * @param x2
   * @param y2
   * @param p1
   *          auxiliary Points for intermedaite calculations. It will change
   *          value.
   * @param p2
   *          auxiliary Point for intermedate calculation. It will change value.
   */
  protected void drawLine(Graphics2D g, double x1, double y1, double x2,
      double y2, Point2D p1, Point2D p2) {

    Point2D t1, t2;

    t1 = this.userToScreenCoordinates(x1, y1, p1);
    t2 = this.userToScreenCoordinates(x2, y2, p2);
    if (t1 != null && t2 != null)
      g.drawLine((int) t1.getX(), (int) t1.getY(), (int) t2.getX(),
          (int) t2.getY());

  }

  /**
   * Calculate the linear transformation from the user cooridnate system to the
   * widget pixel space. The transformations are stored in the class' variables
   * forwardTransform and backwardTansform.
   * 
   * The transformations are based on the actual values of the variables
   * drawX/Y, border???, and xmin, xmax, ymin, ymax.
   * 
   * 
   */
  private void updateTransformation() {

    rangeX = maxX - minX;
    rangeY = maxY - minY;

    if (rangeX == 0 || rangeY == 0) {
      forwardTransform = null;
      backwardTransform = null;

      myLogger
          .error("Zero range. Transformation is set to null and will cause issues!\n Requested ranges: "
              + rangeX + " / " + rangeY);
      Thread.dumpStack();

      return;
    }

    double scaleX = ((double) (drawX - borderLeft - borderRight)) / rangeX;
    double scaleY = -((double) (drawY - borderUp - borderDown)) / rangeY; // Java

    double offsetX = 0;
    double offsetY = 0;

    offsetX = borderLeft - minX * scaleX;
    offsetY = drawY - borderDown - minY * scaleY;

    forwardTransform = new AffineTransform(scaleX, 0, 0, scaleY, offsetX,
        offsetY);

    if (scaleY != 0 && scaleX != 0)
      try {
        backwardTransform = forwardTransform.createInverse();
      } catch (Exception e) {
        myLogger
            .warn("Cannot create inverse coordinate transformation during init.");
        myLogger.warn("These are the paramters of the forward transform:");
        myLogger.warn("Scale: " + scaleX + " / " + scaleY);
        myLogger.warn("Offsets: " + offsetX + " / " + offsetY);

        backwardTransform = null;
      }

  }

  /**
   * Draw a coordinate system for this widget.
   * 
   * @param g
   * @param dimX
   * @param dimY
   */
  protected void drawCooSys(Graphics2D g, int dimX, int dimY) {
    // Write in the numbers along the y-axis.
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    if (forwardTransform == null || g == null)
      return;

    g.setFont(CooSysNumberFont);

    myNumberFormat.setMaximumFractionDigits(Math.max(0,
        0 - (int) Math.round(Math.log10(this.maxX - this.minX)) + 1));

    clearBackground(g);

    Point2D p1 = new Point2D.Double(0, 0);
    Point2D p2 = new Point2D.Double(0, 0);

    if (drawYgrid) {

      drawYGrid(g, p1, p2);
    }

    if (drawXgrid) {
      drawXGrid(g, p1, p2);
    }

    g.setColor(this.CooSysForeground);
    this.userToScreenCoordinates(rangeX, this.maxY, p1);
    this.userToScreenCoordinates(0, this.minY, p2);

    g.drawRect(borderLeft, borderUp, drawX - borderLeft - borderRight, drawY
        - borderUp - borderDown);

    if (yLabel != null) {
      drawYLabel(g);
    }

    if (xLabel != null) {
      drawXLabel(g);
    }
  }

  private void drawXLabel(Graphics2D g) {
    g.setFont(CooSysLabelFont);
    double width = CooSysLabelFont.getStringBounds(xLabel,
        g.getFontRenderContext()).getWidth();
    g.drawString(xLabel, (int) (borderLeft + (drawX - borderLeft - borderRight)
        / 2 - width / 2), drawY - borderDown / 4);
  }

  private void drawYLabel(Graphics2D g) {

    AffineTransform oldTransform = g.getTransform();
    g.setFont(CooSysLabelFont);

    double width = CooSysLabelFont.getStringBounds(yLabel,
        g.getFontRenderContext()).getWidth();
    int x0 = borderLeft / 3;
    int y0 = (int) (borderUp + (drawY - borderUp - borderDown) / 2 + width / 2);

    AffineTransform fontAT = new AffineTransform();
    fontAT.translate(x0, y0);
    fontAT.rotate(-90 * Math.PI / 180.);
    Font f = g.getFont().deriveFont(fontAT);
    g.setFont(f);

    g.drawString(yLabel, 0, 0);
    g.setTransform(oldTransform);
  }

  /**
   * Calculate a reasonable minimum, maximum value and a grid step for the
   * coordinate system grid and labels.
   * 
   * @param min
   *          minimum value of axis range.
   * @param max
   *          maximum value of axis range.
   * @return the proposed minimum value for the axis (element 0) and step width
   *         (element 1).
   */

  double[] getGridStartAndStep(double min, double max) {

    double[] retVal = new double[2];

    double step = Math.round(Math.log10(Math.abs(max - min)));
    step = Math.pow(10, Math.round(step - 1));
    while (Math.abs(max - min) / step > 11) {

      step *= 2;
      // System.out.println ("Step: " + step);
    }

    double start = Math.rint(min * 10) / 10;

    retVal[0] = start;
    retVal[1] = step;
    return retVal;

  }

  private void drawXGrid(Graphics2D g, Point2D p1, Point2D p2) {

    double[] gridConf = getGridStartAndStep(minX, maxX);
    double stepX = gridConf[1];

    double x1 = gridConf[0];

    double gridMin = x1;
    double gridMax = maxX;
    if (x1 > maxX) {
      gridMin = maxX;
      gridMax = x1;
    }
    int step = 0;
    for (double xx = gridMin; xx <= gridMax; xx += stepX) {

      this.userToScreenCoordinates(xx, minY, p1);
      this.userToScreenCoordinates(xx, maxY, p2);

      if (xx == 0)
        g.setColor(this.CooSysForeground);
      else
        g.setColor(this.CooSysGrid);

      g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(),
          (int) p2.getY());

      g.setColor(this.CooSysForeground);
      String label = myNumberFormat.format(xx * this.xLabelMultiplier);
      Rectangle2D d = CooSysNumberFont.getStringBounds(label,
          g.getFontRenderContext());
      double width = d.getWidth();
      double height = d.getHeight();
      if ((step++) % 2 == 0)
        g.drawString(label, (int) (p2.getX() - width / 2), (int) (this.drawY
            - borderDown + height + 2));

    }
  }

  private void drawYGrid(Graphics2D g, Point2D p1, Point2D p2) {

    double[] gridConf = getGridStartAndStep(minY, maxY);
    double stepY = gridConf[1];
    double y1 = gridConf[0];

    if (y1 < minY)
      y1 += stepY;

    g.setColor(this.CooSysGrid);
    g.setFont(CooSysNumberFont);

    for (double yy = y1; yy <= maxY; yy += stepY) {

      this.userToScreenCoordinates(minX, yy, p1);
      this.userToScreenCoordinates(maxX, yy, p2);

      if (yy == 0)
        g.setColor(this.CooSysForeground);
      else
        g.setColor(this.CooSysGrid);

      g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(),
          (int) p2.getY());

      g.setColor(this.CooSysForeground);
      String label = myNumberFormat.format(yy);
      Rectangle2D d = CooSysNumberFont.getStringBounds(label,
          g.getFontRenderContext());
      double width = d.getWidth();
      double height = d.getHeight();
      g.drawString(label, (int) (borderLeft - width - 5),
          (int) (p2.getY() + height / 2));

    }
  }

  protected void clearBackground(Graphics2D g) {
    g.setColor(CoosysBackground);
    g.fillRect(0, 0, drawX, drawY);
  }

  public Dimension getMinimumSize() {
    return new Dimension(drawX, drawY);

  }

  public Dimension getMaximumSize() {
    return getMinimumSize();

  }

  public Dimension getPreferredSize() {
    return getMinimumSize();

  }

  public String getXLabel() {
    return xLabel;
  }

  public void setXLabel(String label) {
    xLabel = label;
    reset();
  }

  public String getYLabel() {
    return yLabel;
  }

  public void setYLabel(String label) {
    yLabel = label;
    reset();
  }

  public void setXLabelMultiplier(double xLabelMultiplier) {
    this.xLabelMultiplier = xLabelMultiplier;
  }

  public double getXLabelMultiplier() {
    return xLabelMultiplier;
  }

  // Services for threaded mode of this class.
  //

  protected Semaphore renderTrigger = null;
  protected Semaphore dataLock = null;

  public void run() {
    // initial setup
    try {
      renderTrigger.acquire(1);
    } catch (Exception e) {
      myLogger.error("Error while waitingsetting up render trigger");
    }

    while (renderTrigger != null) {
      try {
        renderTrigger.acquire(1);
      } catch (Exception e) {
        myLogger.error("Error while waiting for render trigger");
      }

      long timeLapse = System.nanoTime() - lastRenderRequest ;
      if ( timeLapse < 80 * 1000  ) {
        try {
          Thread.sleep(80 - timeLapse / 1000 );
        } catch (InterruptedException e) {

          e.printStackTrace();
        }
      }
      render();
      repaint();
      lastRenderRequest = System.nanoTime();

    }

  }

  public void startThreadedMode() {
    if (renderTrigger != null)
      return;
    renderTrigger = new Semaphore(1);

    Thread t = new Thread(this);
    t.setName("CooSysComponent " + this.getClass().getCanonicalName());
    // t.setPriority(2);
    t.start();
    this.invokeRepaint();
  }
}
