package org.cowjumping.VisualFitsBrowser.ImageActions;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;
import org.cowjumping.VisualFitsBrowser.util.ODIFitsFileEntry;
import org.cowjumping.guiUtils.GUIConsts;
import org.cowjumping.odi.ODIFitsReader.QuickHeaderInfo;

/**
 * Display the FITS header of an image, or the acquisition log of an ODi image,
 * depending on the mode set.
 * 
 * @author harbeck
 *
 */

@SuppressWarnings("serial")
public class ODIImageInfoPanel extends ImageEvaluator {

	ODIFitsFileEntry myCurrentImage = null;
	private JLabel FileNameLabel;

	private int lastPanePosition = 0;
	private JScrollPane p = null;
	private JTextArea myTextArea = null;
	private final static Logger myLogger = Logger.getLogger(ODIImageInfoPanel.class);

	final static public int MODE_FITSHEADER = 0;
	final static public int MODE_LOGFILE = 1;

	private static int mode = MODE_FITSHEADER;

	public ODIImageInfoPanel() {
		super();
		initGUI();

	}

	public void setMode(int mode) {
		ODIImageInfoPanel.mode = mode;
	}

	@Override
	public int setImageList(Vector<ODIFitsFileEntry> imagelist, int otaX, int otaY) {

		if (imagelist != null && imagelist.size() > 0) {

			File f = new File(imagelist.firstElement().getAbsolutePath());

			if (f.exists()) {

				loadImageInfo(imagelist.firstElement(), otaX, otaY);
				return 1;

			} else {
				myLogger.warn("File " + f.getAbsolutePath() + " is not a valid input image");
			}

		} else {
			myLogger.warn("Received an invalid request for image update: " + imagelist);
		}

		loadImageInfo(null, -1, -1);
		return 0;
	}

	protected void loadImageInfo(ODIFitsFileEntry image, int otaX, int otaY) {

		if (image == null) {
			myLogger.warn("load null image requested.aborting");
			return;
		}

		clearImageInfo();
		File f = new File(image.getAbsolutePath());

		if (f.exists() && myTextArea != null) {
			myLogger.debug ("Start filling in text area");
			this.FileNameLabel.setText(image.FName);

			switch (ODIImageInfoPanel.mode) {
			case MODE_FITSHEADER: {
				Vector<String> fitsHeader = QuickHeaderInfo.readFITSHeader(f);
				if (fitsHeader != null)
					for (String card : fitsHeader) {
					myLogger.debug ("adding: " + card);
						myTextArea.append(card + "\n");
					}
					myLogger.debug ("Done reading fits header into image area");

				try {
					myTextArea.setCaretPosition(this.lastPanePosition);
				} catch (Exception e) {
					myLogger.debug("probably bad carret position");
				}
				break;
			}
			case MODE_LOGFILE: {
				myLogger.error("This not implemented for type of fits file");
				/*
				 * String logfile = QuickHeaderInfo.getLogfileContent(f);
				 * myTextArea.append(logfile);
				 * myTextArea.setCaretPosition(this.lastPanePosition);
				 */
			}

			}
			this.repaint();

		} else {
			myLogger.error("could not read header information for file " + f.getAbsolutePath());
		}

	}

	protected void clearImageInfo() {

		if (myTextArea != null) {
			this.lastPanePosition = myTextArea.getCaretPosition();
			myTextArea.setText("");
		}
	}

	private void initGUI() {

		BorderLayout thisLayout = new BorderLayout();

		this.setLayout(thisLayout);
		this.setPreferredSize(new java.awt.Dimension(600, 515));

		{
			FileNameLabel = new JLabel();
			FileNameLabel.setFont(GUIConsts.TitleFont);
			FileNameLabel.setBackground(GUIConsts.InformationBackgroundColor);
			FileNameLabel.setOpaque(true);
			FileNameLabel.setText("N/A");
			FileNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
			Dimension d = FileNameLabel.getPreferredSize();
			FileNameLabel.setPreferredSize(new Dimension(this.getPreferredSize().width, d.height));
			this.add(FileNameLabel, BorderLayout.NORTH);

		}

		{

			this.myTextArea = new AntiAliasedTextPane(25, 80);
			myTextArea.setFont(GUIConsts.TTFont12);
			p = new JScrollPane(myTextArea);
			p.setAutoscrolls(false);
			this.add(p, BorderLayout.CENTER);

		}

	}

	class AntiAliasedTextPane extends JTextArea {

		public AntiAliasedTextPane(int a, int b) {
			super(a, b);
		}

		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			super.paintComponent(g2);
		}
	}

}
