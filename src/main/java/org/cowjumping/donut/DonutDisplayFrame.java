package org.cowjumping.donut;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cowjumping.guiUtils.GUIConsts;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DonutDisplayFrame extends JFrame implements DonutBridgeResultListener {

    private static final Logger log = LogManager.getLogger(DonutDisplayFrame.class);

    private static DonutDisplayFrame singleInstance = null;

    JTextArea donutResults = new JTextArea(20, 40);
    JLabel donutFitDisplay = new JLabel();

    public DonutDisplayFrame() {

        super("Donut Display");

        JPanel centralPanel = new JPanel();
        centralPanel.setLayout(new BoxLayout(centralPanel, BoxLayout.Y_AXIS));
        this.getContentPane().add(centralPanel);
        donutResults.setFont(GUIConsts.TTFont16);
        Box donutBox = Box.createHorizontalBox();
        donutBox.add(donutFitDisplay);
        donutBox.add(donutResults);
        centralPanel.add(donutBox);
        pack();

    }

    public synchronized static DonutDisplayFrame getInstance() {

        if (singleInstance == null) {
            singleInstance = new DonutDisplayFrame();
        }
        return singleInstance;
    }

    public void clearImage() {

    }


    private Image getScaledImage(Image srcImg, int h){

        int w = srcImg.getWidth(null)* h / srcImg.getHeight(null);
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();

        return resizedImg;
    }

    @Override
    public void notifyResult(pyDonutBridge result) {

        donutResults.setText(result.resultsString);
        donutFitDisplay.setIcon(new ImageIcon(getScaledImage(result.resultImage, 400)));
        invalidate();
        pack();

    }


}
