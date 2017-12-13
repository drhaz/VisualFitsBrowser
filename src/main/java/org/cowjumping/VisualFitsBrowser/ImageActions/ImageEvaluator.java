package org.cowjumping.VisualFitsBrowser.ImageActions;

import java.awt.Dimension;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JPanel;

import org.cowjumping.FitsUtils.ImageContainer;
import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

@SuppressWarnings("serial")
/**
 * 
 * A JPanel class that accepts a list of ODIFitsEntry files as input, does
 * something with those images, and finaly displays the result in an appropaite
 * manner.
 * 
 * 
 * @author harbeck
 *
 */
public abstract class ImageEvaluator extends JPanel {

	/** A central treadpool that al iamge operation scan utilize to do
	 * heavy-lifting.
     */
	final static protected ExecutorService myThreadPool = Executors.newFixedThreadPool(8);

	public ImageEvaluator() {

		super();
		this.setPreferredSize(new Dimension(570, 630));

	}

	 public int setImageList(Vector<FitsFileEntry> imagelist) {

		if (imagelist != null)
			return imagelist.size();

		return -1;
	}


	public int setImageContainer (Vector<ImageContainer> imageContainers) {

	        if (imageContainers != null)
	            return imageContainers.size();

	        return -1;
    }

}
