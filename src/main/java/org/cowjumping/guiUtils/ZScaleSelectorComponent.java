package org.cowjumping.guiUtils;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;
import org.cowjumping.odi.ODIFitsReader.OBSTYPE;

/**
 * IMplements a GUI component that allows to set the z-range of an image display
 * to be selected (i.e., the contrast range). It can also send the command to
 * automatically apply an offset in the iamge display.
 * 
 * @author harbeck
 * 
 */

public class ZScaleSelectorComponent extends JPanel implements ChangeListener,
		ItemListener, ActionListener {

	// public enum ImgTypes {
	// BIAS, FLAT, IMAGE, PREIMAGE;

	/*
	 * public static ImgTypes getTypeByFileName(String fname) { String subStr =
	 * fname.substring(fname.lastIndexOf("/") + 1); ImgTypes retVal = null;
	 * 
	 * switch (subStr.charAt(0)) {
	 * 
	 * case 'b': case 'd': retVal = BIAS; break;
	 * 
	 * case 'f': retVal = FLAT; break; case 'o': retVal = IMAGE; break;
	 * 
	 * case 'p': if (subStr.startsWith("preimage")) { retVal = PREIMAGE; break;
	 * } }
	 * 
	 * return retVal; } };
	 */

	OBSTYPE currentImageMode = OBSTYPE.BIAS;

	private static final long serialVersionUID = 1L;
	private RangeSlider ZScaleSlider = null;
	private JCheckBox FaintStar = null;
	private JCheckBox ScaleResetButton = null;
	private JCheckBox RememberImageTypeButton = null;
	private JLabel currentImageModeLabel = null;
	HashMap<OBSTYPE, Point2D> LastZScale = null;

	/**
	 * The range to be displayed in the faint mode.
	 * 
	 */
	private double faintRange = 2000;

	/**
	 * A List of zScaleListeners to be notified upon changes
	 * 
	 */
	private Vector<zScaleListener> myClients = new Vector<zScaleListener>();

	private static final Logger myLogger = Logger
			.getLogger(ZScaleSelectorComponent.class);

	public ZScaleSelectorComponent(zScaleListener client) {
		this(client, 230, 50);
	}

	public ZScaleSelectorComponent(zScaleListener client, int dimX, int dimY) {
		super();
		if (client != null)
			this.addClient(client);

		LastZScale = new HashMap<OBSTYPE, Point2D>();
		LastZScale.put(OBSTYPE.BIAS, new Point2D.Double(85, 115));
		LastZScale.put(OBSTYPE.DARK, new Point2D.Double(85, 125));

		LastZScale.put(OBSTYPE.DOMEFLAT, new Point2D.Double(20000, 40000));
		LastZScale.put(OBSTYPE.OBJECT, new Point2D.Double(100, 1000));
		LastZScale.put(OBSTYPE.PREIMAGE, new Point2D.Double(90, 200));
		LastZScale.put(OBSTYPE.FOCUS, new Point2D.Double(90, 1000));
		LastZScale.put(OBSTYPE.INDEF, new Point2D.Double(90, 200));

		Box masterBox = Box.createHorizontalBox();
		this.add(masterBox);
		Box hBox;
		{
			hBox = Box.createHorizontalBox();
			hBox.setAlignmentX(Component.CENTER_ALIGNMENT);

			masterBox.add(hBox);

			ZScaleSlider = new RangeSlider("", 0, 65000);

			ZScaleSlider.setPreferredSize(new Dimension(dimX, dimY));
			ZScaleSlider.addChangeListener(this);
			ZScaleSlider.setFont(new Font("Helvetica", Font.PLAIN, 10));
			hBox.add(ZScaleSlider);

		}

		{
			hBox = Box.createHorizontalBox();

			hBox.setAlignmentX(Component.CENTER_ALIGNMENT);
			masterBox.add(hBox);

			FaintStar = new JCheckBox("faint range");
			FaintStar.setSelected(false);
			FaintStar.addItemListener(this);
			hBox.add(FaintStar);

			ScaleResetButton = new JCheckBox("Auto Bias");
			ScaleResetButton.setSelected(false);
			ScaleResetButton.addItemListener(this);

			// hBox.add (ScaleResetButton);
			hBox.add(Box.createHorizontalStrut(5));

			currentImageModeLabel = new JLabel();
			this.setImageMode(OBSTYPE.BIAS);
			hBox.add(currentImageModeLabel);

		}

	}

	public void setImageMode(OBSTYPE type) {
		currentImageMode = type;
		String label = "INDEF";
		if (type != null) {
			switch (type) {
			case BIAS:
				label = "[bias]";
				break;

			case DARK:
				label = "[dark]";
				break;

			case DOMEFLAT:
				label = "[flat]";
				break;

			case OBJECT:
				label = "[image]";
				break;

			case PREIMAGE:
				label = "[preimage]";
				break;

			case FOCUS:
				label = "[focus]";
				break;

			}
			this.currentImageModeLabel.setText(label);
			Point2D p = this.LastZScale.get(this.currentImageMode);
			if (p != null)
				this.setZScale((int) p.getX(), (int) p.getY());
		}

	}

	public void addClient(zScaleListener client) {

		if (myLogger.isDebugEnabled())
			myLogger.debug("Adding listener");

		if (client != null && myClients != null) {
			myClients.add(client);
		}
	}

	private void sendZScale(int z1, int z2) {
		for (zScaleListener listener : myClients) {

			listener.setZScale(z1, z2);
		}
	}

	private void sendExtraBias(boolean b) {
		for (zScaleListener listener : myClients) {
			listener.autoSetExtraZOffset(b);
		}

	}

	public void setZScale(int z1, int z2) {
		if (z2 <= this.faintRange)
			this.FaintStar.setSelected(true);
		else
			this.FaintStar.setSelected(false);

		ZScaleSlider.setValues(z1, z2);
		this.LastZScale.get(this.currentImageMode).setLocation(z1, z2);
		sendZScale(z1, z2);
	}

	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == ZScaleSlider) {

			double result[] = ((RangeSlider) e.getSource()).getMinMaxValues();

			sendZScale((int) result[0], (int) result[1]);
			if (currentImageMode != null)
				this.LastZScale.get(currentImageMode).setLocation(result[0],
						result[1]);
		}

	}

	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		if (source == FaintStar) {

			if (e.getStateChange() == ItemEvent.SELECTED) {
				double result[] = ZScaleSlider.getMinMaxValues();

				ZScaleSlider.setBounds(0, faintRange);
				if (result[0] < faintRange && result[1] < faintRange)
					ZScaleSlider.setValues(result[0], result[1]);

			} else {

				double result[] = ZScaleSlider.getMinMaxValues();

				ZScaleSlider.setBounds(0, 65000);

				ZScaleSlider.setValues(result[0], result[1]);
			}

			return;
		}

		if (e.getSource() == this.ScaleResetButton) {

			if (ScaleResetButton.isSelected())
				sendExtraBias(true);
			else
				sendExtraBias(false);

			return;
		}

	}

	public void actionPerformed(ActionEvent e) {

	}

}
