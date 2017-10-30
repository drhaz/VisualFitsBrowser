package org.cowjumping.VisualFitsBrowser.ImageActions;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.Vector;

import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import org.apache.log4j.Logger;
import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;
import org.cowjumping.guiUtils.GUIConsts;
import org.cowjumping.odi.ODIFitsReader.QuickHeaderInfo;

/**
 * Display the FITS header of an image, or the acquisition log of an ODi image,
 * depending on the mode set.
 *
 * @author harbeck
 */

@SuppressWarnings("serial")
public class FITSHeaderInspection extends ImageEvaluator {

    FitsFileEntry myCurrentImage = null;
    private JLabel FileNameLabel;

    private int lastPanePosition = 0;
    private JScrollPane p = null;
    private JTextArea myTextArea = null;
    private JTextField searchInput = null;
    private final static Logger log = Logger.getLogger(FITSHeaderInspection.class);

    final static public int MODE_FITSHEADER = 0;
    final static public int MODE_LOGFILE = 1;
    private static int mode = MODE_FITSHEADER;

    static String lastsearchword = null;
    static int lastsearchoffset = 0;

    public FITSHeaderInspection() {
        super();
        initGUI();

    }

    public void setMode(int mode) {
        FITSHeaderInspection.mode = mode;
    }

    @Override
    public int setImageList(Vector<FitsFileEntry> imagelist, int otaX, int otaY) {

        if (imagelist != null && imagelist.size() > 0) {

            File f = new File(imagelist.firstElement().getAbsolutePath());

            if (f.exists()) {

                loadImageInfo(imagelist.firstElement(), otaX, otaY);
                return 1;

            } else {
                log.warn("File " + f.getAbsolutePath() + " is not a valid input image");
            }

        } else {
            log.warn("Received an invalid request for image update: " + imagelist);
        }

        loadImageInfo(null, -1, -1);
        return 0;
    }

    protected void loadImageInfo(FitsFileEntry image, int otaX, int otaY) {

        if (image == null) {
            log.warn("load null image requested.aborting");
            return;
        }

        clearImageInfo();
        File f = new File(image.getAbsolutePath());

        if (f.exists() && myTextArea != null) {
            log.debug("Start filling in text area");
            this.FileNameLabel.setText(image.FName);

            switch (FITSHeaderInspection.mode) {
                case MODE_FITSHEADER: {
                    Vector<String> fitsHeader = QuickHeaderInfo.readFITSHeader(f);
                    if (fitsHeader != null)
                        for (String card : fitsHeader) {

                            myTextArea.append(card + "\n");
                        }
                    log.debug("Done reading fits header into image area");

                    try {
                        myTextArea.setCaretPosition(this.lastPanePosition);
                    } catch (Exception e) {
                        log.debug("probably bad carret position");
                    }
                    break;
                }
                case MODE_LOGFILE: {
                    log.error("This not implemented for type of fits file");
                /*
                 * String logfile = QuickHeaderInfo.getLogfileContent(f);
				 * myTextArea.append(logfile);
				 * myTextArea.setCaretPosition(this.lastPanePosition);
				 */
                }

            }
            this.repaint();

        } else {
            log.error("could not read header information for file " + f.getAbsolutePath());
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

        {
            searchInput = new JTextField(30);
            searchInput.setFont(GUIConsts.TTFont12);
            this.add(searchInput, BorderLayout.SOUTH);
            Highlighter.HighlightPainter painter =
                    new DefaultHighlighter.DefaultHighlightPainter(Color.cyan);


            searchInput.addKeyListener(new KeyAdapter() {

                @Override
                public void keyTyped(KeyEvent e) {
                    String searchTerm = searchInput.getText().toLowerCase();
                    String text = myTextArea.getText().toLowerCase();
                    int length = searchTerm.length();
                    int offset = text.indexOf(searchTerm, searchTerm.equalsIgnoreCase(lastsearchword) ?
                            lastsearchoffset + 1 : 0);


                    myTextArea.getHighlighter().removeAllHighlights();
                    int found = 0;

                    try {

                        myTextArea.getHighlighter().addHighlight(offset, offset + length, painter);
                        myTextArea.setCaretPosition((int) (offset));

                    } catch (Exception ble) {
                        System.out.println(ble);
                    }
                    log.info("Search term find: " + offset + " found: " + (++found));
                    lastsearchword = searchTerm;
                    lastsearchoffset = offset;

                    super.keyTyped(e);

                }

                @Override
                public void keyPressed(KeyEvent e) {


                }

                @Override
                public void keyReleased(KeyEvent e) {

                }
            });


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
