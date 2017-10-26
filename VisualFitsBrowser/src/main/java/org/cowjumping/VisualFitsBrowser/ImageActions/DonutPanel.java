package org.cowjumping.VisualFitsBrowser.ImageActions;

import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;
import org.cowjumping.donut.DonutBridgeResultListener;
import org.cowjumping.donut.pyDonutBridge;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class DonutPanel extends  ImageEvaluator implements DonutBridgeResultListener {


    JLabel iconLabel;
    public DonutPanel () {
        super();

        BorderLayout mBorderLayout = new BorderLayout();
        this.setLayout(mBorderLayout);
        this.setPreferredSize(new java.awt.Dimension(600, 515));

        iconLabel = new JLabel ();
        add (iconLabel, BorderLayout.CENTER);


    }

    @Override
    public int setImageList(Vector<FitsFileEntry> imagelist, int
            otaX, int otaY) {

        if (imagelist != null)
            return imagelist.size();

        return -1;
    }


    @Override
    public void notifyResult(pyDonutBridge result) {
        ImageIcon icon = new ImageIcon (result.resultImage.getScaledInstance(iconLabel.getWidth(),iconLabel.getHeight(),Image.SCALE_SMOOTH));
        this.iconLabel.setIcon(icon);
        System.out.println (result.resultsString);
    }
}
