package org.cowjumping.donut;

import javax.swing.*;
import org.apache.log4j.Logger;
import org.cowjumping.guiUtils.GUIConsts;

public class DonutDisplayFrame extends JFrame implements DonutBridgeResultListener {

    private static final Logger log = Logger.getLogger(DonutDisplayFrame.class);

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

    @Override
    public void notifyResult(pyDonutBridge result) {

        donutResults.setText(result.resultsString);
        donutFitDisplay.setIcon(new ImageIcon(result.resultImage));
        invalidate();
        pack();

    }


}
