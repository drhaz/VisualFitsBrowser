package org.wiyn.util.FitsComments;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.wiyn.odi.VisualFitsBrowser.ImageActions.OTAInFocalPlaneSelector;

/**
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI
 * Builder, which is free for non-commercial use. If Jigloo is being used
 * commercially (ie, by a corporation, company or business for any purpose
 * whatever) then you should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details. Use of Jigloo implies
 * acceptance of these licensing terms. A COMMERCIAL LICENSE HAS NOT BEEN
 * PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED LEGALLY FOR
 * ANY CORPORATE OR COMMERCIAL PURPOSE.
 */
public class OTASelectionPanel extends JPanel implements
	OTAInFocalPlaneSelector {

    final static String[] OTAIndexList = { "0", "1", "2", "3", "4", "5", "6",
	    "7", "-1" };
    Object callback = null;
    private JComboBox otaXSpinner;
    private JComboBox otaYSpinner;

    public OTASelectionPanel(Object callback, int selecttX, int selectY) {
	super ();
	
	this.callback = callback;
	GridLayout mLayout = new GridLayout (2, 2);
	this.setLayout (mLayout);

	JLabel xlab = new JLabel ("  OTA X:");
	JLabel ylab = new JLabel ("  OTA Y:");

	otaXSpinner = new JComboBox (this.OTAIndexList);
	otaXSpinner.setSelectedIndex (selecttX);
	otaYSpinner = new JComboBox (this.OTAIndexList);
	otaYSpinner.setSelectedIndex (selectY);

	final Object mycallback = callback;

	otaXSpinner.addActionListener (new ActionListener () {

	    public void actionPerformed (ActionEvent e) {
		JComboBox cb = (JComboBox) e.getSource ();
		String number = (String) cb.getSelectedItem ();
		setOTAValue (mycallback, "otaX", Integer.parseInt (number));
		System.out.println ("OTA X Selection modified");
	    }

	});

	otaYSpinner.addActionListener (new ActionListener () {
	    public void actionPerformed (ActionEvent e) {
		JComboBox cb = (JComboBox) e.getSource ();
		String number = (String) cb.getSelectedItem ();
		setOTAValue (mycallback, "otaY", Integer.parseInt (number));

	    }

	});

	JPanel p;
	p = new JPanel ();p.setOpaque (false);
	p.add (xlab);
	add (p);
	p = new JPanel ();p.setOpaque (false);
	p.add (otaXSpinner);
	add (p);

	p = new JPanel ();p.setOpaque (false);
	p.add (ylab);
	add (p);
	p = new JPanel ();p.setOpaque (false);
	p.add (otaYSpinner);
	add (p);

	this.setBorder (BorderFactory.createTitledBorder ("OTA Selection"));
	this.setMaximumSize (this.getMinimumSize ());
	this.setPreferredSize (this.getMinimumSize ());
    }

    static void setOTAValue (Object callback, String varName, int Value) {

	if (callback != null) {

	    try {
		java.lang.reflect.Field field = callback.getClass ()
			.getDeclaredField (varName);

		if (field != null)
		    field.setInt (callback, Value);

	    } catch (Exception e) {
		// TODO Auto-generated catch block
		System.err
			.println ("Error while seting OTA selection via reflection: "
				+ e);

	    }
	}
    }

    public void setSelectedOTA (int x, int y) {
	if (x >= 0 && x < 8) {
	    this.otaXSpinner.setSelectedIndex (x);
	}
	if (y >= 0 && y < 8) {
	    this.otaYSpinner.setSelectedIndex (y);
	}

    }
}
