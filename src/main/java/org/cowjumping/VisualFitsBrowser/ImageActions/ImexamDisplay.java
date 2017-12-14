package org.cowjumping.VisualFitsBrowser.ImageActions;

import org.cowjumping.FitsUtils.ImageContainer;
import org.cowjumping.FitsUtils.odiCentroidSupport;
import org.cowjumping.guiUtils.GuideStarDisplayComponent;
import org.cowjumping.guiUtils.RadialPlotComponent;
import org.cowjumping.guiUtils.ZScaleSelectorComponent;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class ImexamDisplay extends ImageEvaluator {


    private  RadialPlotComponent rp;
    private  GuideStarDisplayComponent gd;

    public ImexamDisplay() {
        super();
        setLayout(new BorderLayout());

        rp = new RadialPlotComponent(200,200);
        gd = new GuideStarDisplayComponent(200,200,0,65000);
        Box b = Box.createHorizontalBox();
        this.add(b);

        b.add (gd);
        b.add (rp);

        ZScaleSelectorComponent zs = new ZScaleSelectorComponent(gd,400,100);

        this.add (zs, BorderLayout.SOUTH);

    }

    @Override
    public int setImageContainer(Vector<ImageContainer> imageContainers) {


        if (imageContainers != null && imageContainers.size()>0) {

            ImageContainer gs = imageContainers.elementAt(0);
            odiCentroidSupport.findSkyandPeak(gs,2,2);
            odiCentroidSupport.MomentAnalysis(gs,1,gs.getImageDimX()-1,1, gs.getImageDimY()-1);
            rp.updateData(gs);
            gd.updateImage(gs.rawImageBuffer, gs.getImageDimX(), gs.getImageDimY(), gs.getCenterX(), gs.getCenterY(),0);
            gd.setZScale(gs.getBackground()- gs.getBackNoise(), gs.getPeak());
        }
        return -1;
    }
}
