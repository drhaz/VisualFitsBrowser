package org.cowjumping.guiUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cowjumping.FitsUtils.ImageContainer;
import org.cowjumping.FitsUtils.RadialProfile;
import org.cowjumping.FitsUtils.gaussImage;
import org.cowjumping.FitsUtils.odiCentroidSupport;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;


/**
 * Provides a radial profile plot of an input image (i.e., a
 * GuideStarContainer).
 *
 * @author harbeck
 */

@SuppressWarnings("serial")
public class RadialPlotComponent extends org.cowjumping.guiUtils.CooSysComponent {

    private final static Logger myLogger = LogManager.getLogger();

    RadialProfile myProfile = null;

    int dimX = 0;
    int dimY = 0;

    Color Background = Color.black;
    Color CooSys = Color.white;
    Color Point = Color.yellow;

    private double fwhm;
    private double peak;
    private double sky;

    private boolean plotRadialFit = true;

    public RadialPlotComponent() {
        this(150, 150);
    }

    public RadialPlotComponent(int drawX, int drawY) {
        this.updateSize(drawX, drawY);
        this.updateRange(0, 20, 0, 1.1);
        this.setBorders(10, 10, 10, 10);
        this.initForSize();

    }

//  public void updateData(Object newImage, int dimX, int dimY, double cX,
//      double cY, double fwhm, double peak, double sky) {

    public void updateData(ImageContainer gs) {

        initBuffers(gs.getImageDimX(), gs.getImageDimY());

        double fwhm = 0.5 * (gs.getFWHM_X() + gs.getFWHM_Y() );
        double peak = gs.getPeak();
        double sky = gs.getBackground();


        this.fwhm = fwhm;
        this.peak = peak;
        this.sky = sky;

        if (!this.lockData()) {
            if (myLogger.isDebugEnabled())
                myLogger.debug("update data skipped due to data lock.");
            return;
        }

        if (this.myProfile == null)
            myProfile = new RadialProfile();

        myProfile.update(gs);

        odiCentroidSupport.scaleAndBiasImage(myProfile.value, (float) (peak - sky),
                (float) sky);
        this.sky /= peak;
        this.peak /= peak;

        this.releaseData();

        this.invokeRepaint();
    }

    private void initBuffers(int dimX, int dimY) {
        if (dimX != this.dimX || dimY != this.dimY) {

            this.dimX = dimX;
            this.dimY = dimY;

        }
    }

    protected void drawCooSys(Graphics2D og, int dimX, int dimY) {
        og.setColor(Background);
        og.fillRect(0, 0, drawX, drawY);

        og.setColor(CooSys);

        Point2D p1 = new Point2D.Double(0, 0);
        Point2D p2 = new Point2D.Double(rangeX, 0);

        this.forwardTransform.transform(p1, p1);
        this.forwardTransform.transform(p2, p2);

        og.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(),
                (int) p2.getY());
        p2.setLocation(0, rangeY);
        forwardTransform.transform(p2, p2);
        og.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(),
                (int) p2.getY());

        for (int ii = 0; ii <= rangeX; ii++) {
            p1.setLocation(ii, 0);
            p2.setLocation(ii, 0);
            this.forwardTransform.transform(p1, p1);
            this.forwardTransform.transform(p2, p2);
            p2.setLocation(p1.getX(), p1.getY() + 5);
            og.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(),
                    (int) p2.getY());

        }
        for (float ii = 0; ii <= rangeY; ii += 0.1) {
            p1.setLocation(0, ii);
            p2.setLocation(0, ii);
            this.forwardTransform.transform(p1, p1);
            this.forwardTransform.transform(p2, p2);
            p2.setLocation(p1.getX() - 5, p1.getY());
            og.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(),
                    (int) p2.getY());

        }

    }

    public Dimension getPreferredSize() {
        return new Dimension(drawX, drawY);
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public boolean isPlotRadialFit() {
        return plotRadialFit;
    }

    public void setPlotRadialFit(boolean plotRadialFit) {
        this.plotRadialFit = plotRadialFit;
    }

    @Override
    protected void drawData(Graphics2D og) {

        if (forwardTransform == null) {
            myLogger.warn("drawData: no Transformation defined!");
            return;
        }

        og.setColor(Point);
        Point2D p1 = new Point2D.Double(0, 0);

        if (myProfile != null && myProfile.radius != null) {

            for (int ii = 0; ii < dimX * dimY; ii++) {

                p1.setLocation(myProfile.getRadius(ii), myProfile.getValue(ii));

                this.forwardTransform.transform(p1, p1);
                og.drawRect((int) p1.getX(), (int) p1.getY(), 1, 1);

            }
        }

        if (fwhm != 0 && plotRadialFit) {
            og.setColor(Color.green);
            Point2D p2 = new Point2D.Double(0, 0);
            p1.setLocation(0, peak);
            this.forwardTransform.transform(p1, p1);
            double c = fwhm / 2.35482;
            c = 2 * c * c;

            for (float xx = 1; xx < rangeX; xx += 0.5) {

                p2.setLocation(xx, (peak) * Math.exp(-xx * xx / c));
                this.forwardTransform.transform(p2, p2);

                og.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(),
                        (int) p2.getY());
                p1.setLocation(p2);
            }
        }
    }

    public void clear() {
        super.clear();
        this.myProfile = null;
        this.invokeRepaint();
    }



    private  static JFrame theFrame;
    private static RadialPlotComponent rp;
    private static GuideStarDisplayComponent gd;
    synchronized public static JFrame getRadialPlotFrame () {

        if (theFrame == null) {
            JFrame f = new JFrame("Radial Profile");
            rp = new RadialPlotComponent();
            gd = new GuideStarDisplayComponent();
            //f.getContentPane().add (rp);
            f.getContentPane().add (gd);
            Box b = Box.createHorizontalBox();

            f.getContentPane().add(b);
            b.add (gd);
            b.add (rp);
            f.pack();
            f.setVisible(true);
        }

        return theFrame;
    }

    public static void updateImage (ImageContainer gs) {
        getRadialPlotFrame();
        odiCentroidSupport.findSkyandPeak(gs,2,2);
        odiCentroidSupport.MomentAnalysis(gs,1,gs.getImageDimX()-1,1, gs.getImageDimY()-1);
        rp.updateData(gs);
        gd.updateImage(gs.rawImageBuffer, gs.getImageDimX(), gs.getImageDimY(), gs.getCenterX(), gs.getCenterY(),0);
        gd.setZScale(gs.getBackground()- gs.getBackNoise(), gs.getPeak());


    }


    public static void main (String args[]) {
        gaussImage g = new gaussImage(128, 128);
        g.setBinningY((short) 1);
        g.create(64, 64, 1000, 5, 5, 5, 100);

        updateImage(g);

    }
}
