package org.cowjumping.VisualFitsBrowser.ImageActions;

import org.apache.log4j.BasicConfigurator;
import org.cowjumping.FitsUtils.ImageContainer;
import org.cowjumping.FitsUtils.gaussImage;
import org.cowjumping.FitsUtils.odiCentroidSupport;
import org.cowjumping.guiUtils.GUIConsts;
import org.cowjumping.guiUtils.GuideStarDisplayComponent;
import org.cowjumping.guiUtils.RadialPlotComponent;
import org.cowjumping.guiUtils.ZScaleSelectorComponent;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class ImexamDisplay extends ImageEvaluator {


    private RadialPlotComponent rp;
    private GuideStarDisplayComponent gd;
    private ImageBufferStatusDisplay sd;
    public ImexamDisplay() {
        super();
        setLayout(new BorderLayout());


        rp = new RadialPlotComponent(250, 250);
        gd = new GuideStarDisplayComponent(250, 250, 0, 65000);
        sd = new ImageBufferStatusDisplay();

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
        h.add (Box.createVerticalGlue());
        h.add(sd);
        h.add (Box.createVerticalGlue());

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


            sd.update(gs);
        }
        return -1;
    }


    public static void main(String args[]) {
        BasicConfigurator.configure();
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



class ImageBufferStatusDisplay extends JTextArea {


    JTextArea t;
    public ImageBufferStatusDisplay() {
        super(40,12);

        setFont(GUIConsts.TTFont16);

    }


    public void update (ImageContainer gs) {

        StringBuilder sb = new StringBuilder();
        sb.append ("\n");
        sb.append(String.format(" center x     % 6.2f [pix]   " , gs.getWindow_Offset_X() + gs.getCenterX() ));
        sb.append(String.format(" center y     % 6.2f [pix]\n" ,   gs.getWindow_Offset_Y() + gs.getCenterY() ));
        sb.append(String.format(" FWHM         % 6.2f [pix]\n\n", (gs.getFWHM_X() + gs.getFWHM_Y())/2.));
        sb.append(String.format(" Flux:      % 10.2f [ADU]\n", gs.getFlux()));
        sb.append(String.format(" Inst mag       % 5.2f [mag]\n\n", gs.getInstMag()));

        sb.append(String.format(" Sky          % 6.2f \\pm % 4.2f [ADU]\n", gs.getBackground(), gs.getBackNoise()));

        System.out.println (sb);
        setText(sb.toString());

    }

}