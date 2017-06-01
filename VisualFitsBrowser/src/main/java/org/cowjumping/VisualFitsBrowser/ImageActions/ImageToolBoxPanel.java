package org.cowjumping.VisualFitsBrowser.ImageActions;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.cowjumping.VisualFitsBrowser.FileBrowserPanel;
import org.cowjumping.VisualFitsBrowser.util.ODIFitsFileEntry;
import org.cowjumping.guiUtils.GUIConsts;
import org.cowjumping.guiUtils.MultiFlickPanel;
import org.cowjumping.guiUtils.VariableGridLayout;

/**
 * 
 * A panel that contains everything to act on a list of images.
 * 
 * @author harbeck
 *
 */

public class ImageToolBoxPanel extends JPanel implements OTAFileListListener {
	private static final Logger log = Logger.getLogger(ImageToolBoxPanel.class);

	/**
	 * reference to file list
	 * 
	 */
	private FileBrowserPanel mBrowserPanel = null;

	/** A Panel for Action selectors. */
	JPanel ButtonPanel;

	/*
	 * Functional Panels come here
	 */

	public MultiFlickPanel myMultiPanel = null;

	// Actual items follow now:

	ODIImageInfoPanel myImageInfoPanel = null;
	private static final String INFOPANEL = "INFOVIEW";

	public ImageToolBoxPanel(FileBrowserPanel fbp) {

		super();
		this.setmBrowserPanel(fbp);

		BorderLayout mBorderLayout = new BorderLayout();
		this.setLayout(mBorderLayout);

		ButtonPanel = new JPanel();
		this.add(ButtonPanel, BorderLayout.WEST);

		myMultiPanel = new MultiFlickPanel();
		this.add(myMultiPanel, BorderLayout.EAST);

		this.fillButtonPanelForODI(ButtonPanel);
		this.fillMultiPanelView();

	}

	/**
	 * private method to query the associated file browser panel for selected
	 * files. safeguards against a null filebrowserpanel. moving code into own
	 * procedure avoids the need to check for null in every singel image action.
	 * 
	 * @return list of selected files. Could be empty, but will not be null.
	 */
	protected Vector<ODIFitsFileEntry> getFBPSelected() {

		if (this.getmBrowserPanel() != null) {

			return this.getmBrowserPanel().getSelected();
		}
		return new Vector<ODIFitsFileEntry>();
	}

	/**
	 * push a list of images into this Image toolbox
	 * 
	 * @param fileList
	 */
	public void pushFileSelection(Vector<ODIFitsFileEntry> fileList) {

		if (this.myMultiPanel.getTopComponent().equals(INFOPANEL) && fileList != null && fileList.size() == 1) {
			this.myImageInfoPanel.setImageList(fileList, -1, -1);
			return;

		}
	}

	/**
	 * Create the Button Panel context for VisualFitsBrowser use
	 * 
	 * @param ButtonPanel
	 */

	private void fillButtonPanelForODI(JPanel ButtonPanel) {

		// Generate Image Info Panel
		JButton generateHeader = new JButton("Image Header");
		generateHeader.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {

				Vector<ODIFitsFileEntry> fileList = ImageToolBoxPanel.this.getFBPSelected();
				myMultiPanel.setTopComponent(INFOPANEL);
				myImageInfoPanel.setMode(ODIImageInfoPanel.MODE_FITSHEADER);
				if (fileList != null && fileList.size() > 0) {

					myImageInfoPanel.setImageList(fileList, -1, -1);

				} else {
					log.debug("No file list or empty file list for image header display.");
				}

			}

		});

		GridLayout ButtonPanelLayout = new VariableGridLayout(12, 1);
		ButtonPanelLayout.setColumns(1);
		ButtonPanelLayout.setRows(19);
		ButtonPanelLayout.setHgap(5);
		ButtonPanelLayout.setVgap(5);

		ButtonPanel.setLayout(ButtonPanelLayout);
		ButtonPanel.setBackground(new java.awt.Color(196, 196, 217));

		JLabel ImageTitleLabel = new JLabel("Actions:");
		ImageTitleLabel.setFont(GUIConsts.InformationFont);

		ButtonPanel.add(ImageTitleLabel);

		ButtonPanel.add(generateHeader);

		ButtonPanel.add (Box.createVerticalGlue ());

		ButtonPanel.setMaximumSize(ButtonPanel.getMinimumSize());

	}

	private void fillMultiPanelView() {

		myImageInfoPanel = new ODIImageInfoPanel();
		myImageInfoPanel.setName(INFOPANEL);
		myMultiPanel.addComponent(myImageInfoPanel);

	}

	public FileBrowserPanel getmBrowserPanel() {
		return mBrowserPanel;
	}

	public void setmBrowserPanel(FileBrowserPanel mBrowserPanel) {
		this.mBrowserPanel = mBrowserPanel;
	}

	public static void main(String args[]) {
		JFrame f = new JFrame();
		ImageToolBoxPanel tbx = new ImageToolBoxPanel(null);
		f.add(tbx);
		f.pack();
		f.setVisible(true);

	}

}
