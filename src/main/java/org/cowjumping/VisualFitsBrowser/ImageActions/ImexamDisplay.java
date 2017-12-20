package org.cowjumping.VisualFitsBrowser.ImageActions;

import org.cowjumping.FitsUtils.ImageContainer;
import org.cowjumping.FitsUtils.gaussImage;
import org.cowjumping.FitsUtils.odiCentroidSupport;
import org.cowjumping.guiUtils.GuideStarDisplayComponent;
import org.cowjumping.guiUtils.RadialPlotComponent;
import org.cowjumping.guiUtils.ZScaleSelectorComponent;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class ImexamDisplay extends ImageEvaluator {


    private RadialPlotComponent rp;
    private GuideStarDisplayComponent gd;

    public ImexamDisplay() {
        super();
        setLayout(new BorderLayout());


        rp = new RadialPlotComponent(250, 250);
        gd = new GuideStarDisplayComponent(250, 250, 0, 65000);

        Box h = Box.createVerticalBox();
        {
            Box b = Box.createHorizontalBox();


            b.add(Box.createHorizontalGlue());
            b.add(gd);
            b.add(Box.createHorizontalGlue());
            b.add(rp);
            b.add(Box.createHorizontalGlue());
            h.add(b);
        }

        ZScaleSelectorComponent zs = new ZScaleSelectorComponent(gd, 400, 100);
        h.add(zs);

        this.add(h, BorderLayout.CENTER);
    }

    @Override
    public int setImageContainer(Vector<ImageContainer> imageContainers) {


        if (imageContainers != null && imageContainers.size() > 0) {

            ImageContainer gs = imageContainers.elementAt(0);

            odiCentroidSupport.findSkyandPeak(gs, 2, 3);
            double peakX = gs.getCenterX();
            double peakY = gs.getCenterY();
            odiCentroidSupport.MomentAnalysis(gs, 2, gs.getImageDimX() - 2, 2, gs.getImageDimY() - 2);
            double deltaX = Math.min(15, gs.getFWHM_X());
            double deltaY = Math.min(15, gs.getFWHM_Y());
            int minx = (int) (gs.getCenterX() - deltaX);
            int maxx = (int) (gs.getCenterX() + deltaX);
            int miny = (int) (gs.getCenterY() - deltaY);
            int maxy = (int) (gs.getCenterY() + deltaY);
            odiCentroidSupport.MomentAnalysis(gs,minx, maxx, miny, maxy);
            odiCentroidSupport.gaussianFitFWHM (gs);
            odiCentroidSupport.aperturePhotometry(gs);
            rp.updateData(gs);
            gd.updateImage(gs.rawImageBuffer, gs.getImageDimX(), gs.getImageDimY(), gs.getCenterX(), gs.getCenterY(), 0);
            gd.setZScale(gs.getBackground() - gs.getBackNoise(), gs.getPeak());
        }
        return -1;
    }


    public static void main(String args[]) {
        JFrame f = new JFrame();
        ImexamDisplay d = new ImexamDisplay();
        f.getContentPane().add(d);

        f.pack();
        f.setVisible(true);
        gaussImage gi = new gaussImage(50, 50);
        gi.create(20, 20, 1000, 4, 5, 10, 100);
        Vector<ImageContainer> v = new Vector<ImageContainer>();
        v.add(gi);
        d.setImageContainer(v);


    }
}
