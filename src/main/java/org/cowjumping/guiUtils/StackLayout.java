package org.cowjumping.guiUtils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

public class StackLayout implements LayoutManager2 {

    public static final String BOTTOM = "bottom";
    public static final String TOP = "top";

    private List<Component> components = new LinkedList<Component> ();
    private JComponent topComponent;

    public void addLayoutComponent (Component comp, Object constraints) {

	synchronized (comp.getTreeLock ()) {

	    this.topComponent = (JComponent) comp;

	    if (BOTTOM.equals (constraints)) {

		components.add (0, comp);

	    } else if (TOP.equals (constraints)) {

		components.add (comp);

	    } else {

		components.add (comp);

	    }
	}
    }

    public void addLayoutComponent (String name, Component comp) {

	addLayoutComponent (comp, TOP);

    }

    public void removeLayoutComponent (Component comp) {

	synchronized (comp.getTreeLock ()) {

	    components.remove (comp);

	}
    }

    public float getLayoutAlignmentX (Container target) {
	return 0.5f;
    }

    public float getLayoutAlignmentY (Container target) {
	return 0.5f;
    }

    public void invalidateLayout (Container target) {
    }

    public Dimension preferredLayoutSize (Container parent) {
	synchronized (parent.getTreeLock ()) {
	    int width = 0;
	    int height = 0;

	    for (Component comp : components) {
		Dimension size = comp.getPreferredSize ();
		width = Math.max (size.width, width);
		height = Math.max (size.height, height);
	    }

	    Insets insets = parent.getInsets ();
	    width += insets.left + insets.right;
	    height += insets.top + insets.bottom;

	    return new Dimension (width, height);
	}
    }

    public Dimension minimumLayoutSize (Container parent) {
	synchronized (parent.getTreeLock ()) {
	    int width = 0;
	    int height = 0;

	    for (Component comp : components) {
		Dimension size = comp.getMinimumSize ();
		width = Math.max (size.width, width);
		height = Math.max (size.height, height);
	    }

	    Insets insets = parent.getInsets ();
	    width += insets.left + insets.right;
	    height += insets.top + insets.bottom;

	    return new Dimension (width, height);
	}
    }

    public Dimension maximumLayoutSize (Container target) {
	return new Dimension (Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public void layoutContainer (Container parent) {

	synchronized (parent.getTreeLock ()) {
	    int width = parent.getWidth ();
	    int height = parent.getHeight ();

	    Rectangle bounds = new Rectangle (0, 0, width, height);

	    int componentsCount = components.size ();

	    for (int i = 0; i < componentsCount; i++) {
		Component comp = components.get (i);
		comp.setBounds (bounds);
		parent.setComponentZOrder (comp, componentsCount - i - 1);
	    }
	}
    }

    public void setTopComponent (final Container parent, String name) {

	synchronized (parent.getTreeLock ()) {

	    int componentsCount = components.size ();
	    final JComponent oldTop = topComponent;
	    final int width = parent.getWidth ();
	    final int height = parent.getHeight ();

	    if (topComponent.getName ().equals (name))
		return;

	    int n = 1;

	    for (Component c : components) {
		if (name.equals (c.getName ())) {
		    parent.setComponentZOrder (c, 0);
		    this.topComponent = (JComponent) c;
		    c.setVisible (true);
		} else {
		    parent.setComponentZOrder (c, n++);
		    c.setVisible (false);
		}

	    }

	    // oldTop.setVisible (true);

	    Runnable animator = new Runnable () {

		public void run () {
		    int nsteps = 30;
		    int stepWidth = (int) (width / nsteps);
		    oldTop.setVisible (true);

		    for (int ii = 0; ii < nsteps; ii++) {

			Rectangle newBound = new Rectangle (width - ii
				* stepWidth, 0, width, height);
			Rectangle oldBound = new Rectangle (-ii * stepWidth, 0,
				width, height);

			topComponent.setBounds (newBound);
			oldTop.setBounds (oldBound);

			parent.repaint ();

			try {
			    Thread.sleep (10);
			} catch (InterruptedException e) {
			    // TODO Auto-generated catch block
			    e.printStackTrace ();
			}
			// System.err.println (newBound);
		    }

		    oldTop.setBounds (new Rectangle (0, 0, width, height));
		    topComponent
			    .setBounds (new Rectangle (0, 0, width, height));
		    oldTop.setVisible (false);
		    parent.repaint ();
		}

	    };

	    new Thread (animator).start ();
	}

    }

    public String getTopComponent () {
	// TODO Auto-generated method stub
	return topComponent.getName ();
    }
}
