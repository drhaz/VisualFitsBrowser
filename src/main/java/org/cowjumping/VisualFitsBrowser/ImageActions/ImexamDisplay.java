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
            odiCentroidSupport.findSkyandPeak(gs, 1, 5);
            odiCentroidSupport.MomentAnalysis(gs, 1, gs.getImageDimX() - 1, 1, gs.getImageDimY() - 1);
            float delta = (int) (gs.getFWHM_X() * 3.);
            odiCentroidSupport.MomentAnalysis(gs, (int) (gs.getCenterX()-delta),
                    (int) (gs.getCenterX() +delta),
                    (int) (gs.getCenterY()-delta),
                    (int) (gs.getCenterY() + delta));

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
