package org.wiyn.guiUtils;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MultiFlickPanel extends JPanel {

    StackLayout myLayout = null;

    public MultiFlickPanel() {

	myLayout = new StackLayout ();

	this.setLayout (myLayout);
    }

    public void addComponent (JComponent c) {

	add (c);
	setTopComponent (c.getName ());

    }

    public void setTopComponent (String c) {

	myLayout.setTopComponent (this, c);

    }

    public  String getTopComponent () {
	
	return myLayout.getTopComponent();
    }
    
    
    public static void main (String[] args) throws InterruptedException {

	JFrame f = new JFrame ();

	MultiFlickPanel p = new MultiFlickPanel ();

	JPanel p1 = new JPanel ();
	JLabel l1 = new JLabel ("Test Label 1");
	p1.setName (l1.getText ());
	p1.add (l1);
	p1.setBackground (Color.green);
	p.add (p1);

	JPanel p2 = new JPanel ();
	JLabel l2 = new JLabel ("Test Label 2");
	p2.setName (l2.getText ());
	p2.add (l2);
	p.add (p2);

	p.setPreferredSize (new Dimension (300, 300));
	f.getContentPane ().add (p);
	f.pack ();
	f.setVisible (true);

	for (int ii = 0; ii < 100; ii++) {
	    p.setTopComponent (p1.getName ());
	    Thread.sleep (2000);
	    p.setTopComponent (p2.getName ());
	    Thread.sleep (2000);

	}
    }
}
