package org.cowjumping.VisualFitsBrowser.ImageActions;

import java.awt.Dimension;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JPanel;

import org.cowjumping.FitsUtils.ImageContainer;
import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

/**
 * Base class for a tool shown in the Toolbox ({@link ImageToolBoxPanel}).
 * <p>
 * A tool is a panel that receives either a list of files selected in the file
 * browser ({@link #setImageList(Vector)}) or pixel data, e.g. a cutout fetched
 * from ds9 ({@link #setImageContainer(Vector)}), and displays a result. Subclasses
 * override the input method(s) they support. Heavy computations should be submitted
 * to {@link #myThreadPool} rather than run on the Swing event thread.
 * <p>
 * See docs/developer/adding-a-tool.md for how to register a new tool.
 *
 * @author harbeck
 */
@SuppressWarnings("serial")
public abstract class ImageEvaluator extends JPanel {

	/** A central treadpool that al iamge operation scan utilize to do
	 * heavy-lifting.
     */
	final static protected ExecutorService myThreadPool = Executors.newFixedThreadPool(8);

	public ImageEvaluator() {

		super();
		this.setPreferredSize(new Dimension(570, 630));

	}

	/**
	 * Provide the files to evaluate. The default implementation does nothing.
	 *
	 * @param imagelist files selected in the file browser; may be null.
	 * @return number of images accepted, or -1 if the input was null.
	 */
	public int setImageList(Vector<FitsFileEntry> imagelist) {

		if (imagelist != null)
			return imagelist.size();

		return -1;
	}


	/**
	 * Provide pixel data to evaluate, e.g. an image cutout received from ds9. The
	 * default implementation does nothing.
	 *
	 * @param imageContainers image buffers; may be null.
	 * @return number of buffers accepted, or -1 if the input was null.
	 */
	public int setImageContainer (Vector<ImageContainer> imageContainers) {

	        if (imageContainers != null)
	            return imageContainers.size();

	        return -1;
    }

}
