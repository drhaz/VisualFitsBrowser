package org.cowjumping.FitsUtils;

import java.awt.Rectangle;

public class RadialProfile {

  public float[] radius = null;
  public float[] value = null;
  public float[] error = null;

  double xcenter = 0;
  double ycenter = 0;

  int minx;
  int maxx;
  int miny;
  int maxy;

  public RadialProfile(double xcenter, double ycenter) {
    updateCenter(xcenter, ycenter);

  }

  private void updateCenter(double xcenter, double ycenter) {
    if (xcenter == Double.NaN || ycenter == Double.NaN) {
      System.err.println("Invalid center given!");
      this.xcenter = 0;
      this.ycenter = 0;

    } else {
      this.xcenter = xcenter;
      this.ycenter = ycenter;
    }
  }

  public RadialProfile() {
    this(0, 0);
  }

  public RadialProfile( ImageContainer gs) {
    this(0, 0);
    update(gs);

  }

  /**
   * Generate a radial plot of an entire image

   */
  public void update( ImageContainer gs) {

    updateCenter(gs.getCenterX(), gs.getCenterY());
    loadFromImage(gs,
        new Rectangle(1, 1, gs.getImageDimX() - 2, gs.getImageDimY() - 2),
        Double.NEGATIVE_INFINITY);
  }

  /**
   * Generate a radial plto of a sub-window only
   * 
   * @param xcenter
   * @param ycenter

   * @param boundary
   */
  public void update(double xcenter, double ycenter, ImageContainer gs,
      Rectangle boundary) {

    updateCenter(xcenter, ycenter);
    loadFromImage(gs, boundary, Double.NEGATIVE_INFINITY);

  }

  public void update(double xcenter, double ycenter, ImageContainer gs,
      Rectangle boundary, double thres) {

    updateCenter(xcenter, ycenter);
    loadFromImage(gs, boundary, thres);

  }

  public int getNElements() {

    int retval = 0;
    if (radius != null)
      retval = radius.length;
    return retval;
  }

  public float getRadius(int n) {
    if (n < getNElements())
      return radius[n];
    else
      return 0;

  }

  public float getValue(int n) {
    if (n < getNElements())
      return value[n];
    else
      return 0;

  }

  public void swap(int a, int b) {
    float temp;

    if (a >= 0 && b >= 0 && a < this.getNElements() && b < this.getNElements()) {
      temp = value[a];
      value[a] = value[b];
      value[b] = temp;

      temp = radius[a];
      radius[a] = radius[b];
      radius[b] = temp;

      if (error != null) {

        temp = error[a];
        error[a] = error[b];
        error[b] = temp;
      }

    }

  }

  public double getMean(int maxGood) {
    double mean = 0;
    for (int ii = 0; ii < Math.min(maxGood, this.getNElements()); ii++) {
      mean += value[ii];

    }
    return mean;
  }

  public double getSigma(int maxGood) {
    double s = 0;
    double m = getMean(maxGood);
    for (int ii = 0; ii < Math.min(maxGood, this.getNElements()); ii++) {
      double v = value[ii] - m;
      s += v * v;

    }

    return Math.sqrt(s);

  }

  public float getError(int n) {
    if (error != null && n < error.length)
      return error[n];
    else
      return 1f;
  }

  private void loadFromImage(ImageContainer gs, Rectangle boundary,
      double thres) {
    if (getNElements() != boundary.height * boundary.width) {

      this.radius = new float[boundary.width * boundary.height];
      this.value = new float[boundary.width * boundary.height];

    }

    extractRadialProfile(gs, radius, value, boundary, thres);
  }

  

  /**
   * From a given center in the image, extract the radial profile by calculating
   * distance from the center. The radius is extracted only for a window.
   * 
   *
   *          center Y position
   * @param radius
   *          return value: array of radius
   * @param value
   *          return value: array of values

   * @param threshold
   *          minimum image value
   * @return number of elements in the returned array.
   */

  private static int extractRadialProfile(ImageContainer gs, float[] radius, float[] value, Rectangle bounds,
      double threshold) {

    int dimX = gs.getImageDimX();
    int dimY = gs.getImageDimY();
    double cX = gs.getCenterX();
    double cY = gs.getCenterY();
    
    // Ensure boundaries are safe!
    while (bounds.width > 0 && bounds.x + bounds.width >= dimX)
      bounds.width--;

    while (bounds.height > 0 && bounds.y + bounds.height >= dimY)
      bounds.height--;

    int count = 0;
    //

    for (int xx = bounds.x; xx < bounds.x + bounds.width; xx++) {
      for (int yy = bounds.y; yy < bounds.y + bounds.height; yy++) {
        float v = gs.rawImageBuffer[yy * dimX + xx] ;

        if (v >= threshold) {
          
          double t1 = (xx - cX) * gs.getBinningX();
          double t2 = (yy - cY) * gs.getBinningY();
          radius[count] = (float) Math.sqrt(t1 * t1 + t2 * t2);
          value[count] = v;
          count++;
        }
      }
    }

    return count;
  }

}
