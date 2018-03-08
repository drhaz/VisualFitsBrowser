package org.cowjumping.VisualFitsBrowser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.ResponseHandler;
import org.cowjumping.FitsUtils.ImageContainer;
import org.cowjumping.VisualFitsBrowser.ImageActions.ImageToolBoxPanel;
import org.cowjumping.VisualFitsBrowser.util.Filelist2Latex;
import org.cowjumping.donut.DonutDisplayFrame;
import org.cowjumping.donut.pyDonutBridge;
import org.cowjumping.guiUtils.*;

@SuppressWarnings("serial")
public class VisualFitsBrowserApp extends JFrame {


    private final static Logger myLogger = Logger.getLogger(VisualFitsBrowserApp.class);

    private final static String PROP_WINDOWLOCATION_ROOT = VisualFitsBrowserApp.class.getCanonicalName()
            + ".WindowLocation";
    private final static String PROP_WINDOWLOCATION_TOOLBOX = VisualFitsBrowserApp.class.getCanonicalName()
            + ".ToolsBoxWindowLocation";
    private final static String PROP_WINDOWLOCATION_WAVEFRONT = VisualFitsBrowserApp.class.getCanonicalName()
            + ".WavefrontWindowLocation";
    private final static String PROP_SHOWUTILITIES = VisualFitsBrowserApp.class.getCanonicalName() + ".SHOWUTILITIES";
    private final static String PROP_SHOWWAVEFRONT = VisualFitsBrowserApp.class.getCanonicalName() + ".SHOWWAVEFRONT";

    private final static String PROP_AUTODISPLAY = VisualFitsBrowserApp.class.getCanonicalName() + ".AUTODISPLAY";
    private final static String PROP_DS9EXEC = VisualFitsBrowserApp.class.getCanonicalName() + ".DS9EXEC";


    /**
     * Class to Manage & Display image directory
     */
    private static FileBrowserPanel mBrowserPanel;
    private static ImageToolBoxPanel mToolBoxPanel = null;
    private static JFrame ToolBoxFrame = null;
    private static DonutDisplayFrame DonutFrame = null;

    private boolean showUtilities;
    private boolean showWavefront;


    static long IRAF_MSGID = 0;

    /**
     * A singleton file browser application
     */
    public static VisualFitsBrowserApp theFileBrowserApp = null;

    private VisualFitsBrowserApp() {

        super("VisualFitsBrowser " + getVersion());
        theFileBrowserApp = this;

        // Get a saved parameter instance for this application.
        Preferences.initPreferences("VisualFitsBrowserApp");

        /*
         * Build the GUI: Basic Layout: three vertical panels from left to right
         */
        BorderLayout mBorderLayout = new BorderLayout();
        this.setLayout(mBorderLayout);

        this.mToolBoxPanel = new ImageToolBoxPanel(null);
        FileBrowserPanel fbp = new FileBrowserPanel(this.mToolBoxPanel);

        this.setmBrowserPanel(fbp);
        this.add(getmBrowserPanel(), BorderLayout.CENTER);
        this.mToolBoxPanel.setmBrowserPanel(mBrowserPanel);

        this.ToolBoxFrame = new JFrame("Fits ToolBox");

        ToolBoxFrame.getContentPane().add(mToolBoxPanel);
        ToolBoxFrame.pack();

        /*
         * Wavefront analysis
         */

        DonutFrame = DonutDisplayFrame.getInstance();

        /*
         * Define the MenuBar
         */

        this.setJMenuBar(createTheMenu());

        /*
         * Ensure graceful handling of window close events
         */
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myLogger.info("Closing down Directory Listener");
                onExit();
                System.exit(0);
            }
        });

        /*
         * Finally, restore window location and some look and feel fine-tuning
         */
        Preferences.thePreferences.restoreWindowLocation(this, PROP_WINDOWLOCATION_ROOT);
        Preferences.thePreferences.restoreWindowLocation(this.ToolBoxFrame, this.PROP_WINDOWLOCATION_TOOLBOX);
        Preferences.thePreferences.restoreWindowLocation(this.DonutFrame, this.PROP_WINDOWLOCATION_WAVEFRONT);

        getContentPane().setBackground(new java.awt.Color(198, 206, 217));
        try {
            Thread.sleep(20);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            myLogger.error (e1);
        }

        pack();
        setVisible(true);

        this.setShowUtiltiies(
                Boolean.parseBoolean(Preferences.thePreferences.getProperty(PROP_SHOWUTILITIES, "false")));

        /*
         * Ensure graceful handling when Command-Q is pressed in Mac Os X
         */
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                    //setResizable(false);
                    OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("onExit", (Class[]) null));
                } catch (Exception e) {
                    myLogger.info("Could not bind to MacOS X Quit Handler. Get a Mac!");

                }
            }
        });

        final int preferredWidth = this.getWidth();

        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                setSize(new Dimension(preferredWidth, getHeight()));
                super.componentResized(e);
            }

        });

    }

    /**
     * Enable or disable display of the toolbox section right of the list.
     *
     * @param show
     */

    private void setShowUtiltiies(boolean show) {

        Preferences.thePreferences.setProperty(PROP_SHOWUTILITIES, show + "");
        this.showUtilities = show;
        if (this.ToolBoxFrame != null) {

            this.ToolBoxFrame.setVisible(show);
        }

    }


    private void setShowWavefront(boolean show) {

        Preferences.thePreferences.setProperty(PROP_SHOWUTILITIES, show + "");
        this.showWavefront = show;
        if (this.DonutFrame != null) {

            this.DonutFrame.setVisible(show);
        }

    }

    private JMenuBar createTheMenu() {

        JMenuBar theMenu = new JMenuBar();

        JMenu menu = new JMenu("File");
        JMenuItem menuItem;

        menu.getAccessibleContext().setAccessibleDescription("File Menu");
        theMenu.add(menu);


        {
            menuItem = new JMenuItem("Change Directory ...", KeyEvent.VK_L);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (getmBrowserPanel() != null)
                        getmBrowserPanel().selectNewDirectory();
                }
            });

            menu.add(menuItem);
        }

        {
            menuItem = new JMenuItem("Reload Directory", KeyEvent.VK_R);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
            menu.add(menuItem);// Possible error source: generate thumbnail
            // after closure
            // finished.);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (getmBrowserPanel() != null)
                        getmBrowserPanel().reload();
                }
            });
        }


        {
            menuItem = new JMenuItem("Send all selected to ds9", KeyEvent.VK_A);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
            menu.add(menuItem);// Possible error source: generate thumbnail
            // after closure
            // finished.);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (getmBrowserPanel() != null)
                        getmBrowserPanel().sendAllSelectedtods9();
                }
            });
        }


        {
            menuItem = new JMenuItem("Send all selected to clipboard", KeyEvent.VK_A);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK + ActionEvent.SHIFT_MASK));
            menu.add(menuItem);// Possible error source: generate thumbnail
            // after closure
            // finished.);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (getmBrowserPanel() != null)
                        getmBrowserPanel().sendAllSelectedToClipBoard();
                }
            });
        }


        menu.add(new JSeparator());

        {
            menuItem = new JCheckBoxMenuItem("Show ToolBox");
            menuItem.setSelected(
                    Boolean.parseBoolean(Preferences.thePreferences.getProperty(PROP_SHOWUTILITIES, "false")));

            menu.add(menuItem);
            menuItem.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    boolean show = ((JCheckBoxMenuItem) e.getSource()).getState();
                    setShowUtiltiies(show);

                }

            });
        }


        {
            menuItem = Filelist2Latex.getPDFLogFileMenuItem(this.mBrowserPanel);
            menu.add(menuItem);
        }


        {
            menuItem = new JCheckBoxMenuItem("Auto display new image in ds9");
            menuItem.setSelected(Boolean.parseBoolean(Preferences.thePreferences.getProperty(PROP_AUTODISPLAY, "false")));
            if (this.getmBrowserPanel() != null)
                getmBrowserPanel().autoLoadImageToListener =
                        Boolean.parseBoolean(Preferences.thePreferences.getProperty(PROP_AUTODISPLAY, "false"));

            menuItem.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    boolean auto = ((JCheckBoxMenuItem) e.getSource()).getState();
                    if (getmBrowserPanel() != null)
                        getmBrowserPanel().autoLoadImageToListener = auto;

                    Preferences.thePreferences.setProperty(PROP_AUTODISPLAY, auto + "");
                }

            });
            menu.add(menuItem);
        }


        menu.add(new JSeparator());

        {
            menuItem = new JMenuItem("Exit", KeyEvent.VK_X);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.ALT_MASK));
            menu.add(menuItem);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onExit();
                    System.exit(0);
                }
            });
        }

        menu = new JMenu("LogSheets");
        menu.getAccessibleContext().setAccessibleDescription("Logsheets Menu");

        if (false)
            theMenu.add(menu);

        {
            menuItem = new JMenuItem("Create Logsheets", KeyEvent.VK_P);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
            menu.add(menuItem);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (getmBrowserPanel() != null && getmBrowserPanel().getNumberOfEntries() > 0) {
                        // mBrowserPanel.print();

                        String fName = "Log_" + getmBrowserPanel().mRootDirectoryString
                                .substring(getmBrowserPanel().mRootDirectoryString.indexOf("podi") + 5);
                        fName = fName.replaceAll("/", "_");
                        fName = fName.replaceAll("\\.", "") + ".tex";
                        Filelist2Latex.writeFileList2Latex(getmBrowserPanel().mRootDirectoryString,
                                getmBrowserPanel().getImageList(), "/tmp/" + fName);
                        Filelist2Latex.processLatex("/tmp/", fName);
                        Filelist2Latex.processLatex("/tmp/", fName);
                        Filelist2Latex.openLatexPDF("/tmp/" + fName.replace("tex", "pdf"));
                    } else {
                        myLogger.info("Cannot genreate Latex logsheet since threre is no viable filelist");
                    }
                }
            });
        }


        menu = new JMenu("Wavefront");
        theMenu.add(menu);


        {
            menuItem = new JCheckBoxMenuItem("Show Wavefront Frame");
            menuItem.setSelected(
                    Boolean.parseBoolean(Preferences.thePreferences.getProperty(PROP_SHOWWAVEFRONT, "false")));

            menu.add(menuItem);
            menuItem.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    boolean show = ((JCheckBoxMenuItem) e.getSource()).getState();
                    setShowWavefront(show);

                }

            });
        }

        {

            menuItem = new JMenuItem("DONUT from ds9 pick");

            menuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    SAMPUtilities.getDS9Cursor("donut");
                }

            });
            menu.add(menuItem);

        }


        {

            menuItem = new JMenuItem("ds9 Imexam");

            menuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    SAMPUtilities.getDS9ImageCutout("imexam", 20);
                }

            });
            menu.add(menuItem);

        }

        JMenu debugMenu = GUIConsts.getDebugMenu();

        // theMenu.add(debugMenu);

        theMenu.add(Box.createHorizontalGlue());

        final JLabel ds9Label = new JLabel("DS9  ");
        ds9Label.setToolTipText("Launch a new instance of ds9, if available.");
        theMenu.add(ds9Label);
        ds9Label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    if (!ds9Label.isEnabled()) {
                        System.out.println("DS9 label double clicked");

                        ds9Label.setEnabled(true);
                        SAMPUtilities.launchds9(Preferences.thePreferences.getProperty(PROP_DS9EXEC, SAMPUtilities.searchds9Binary()));
                    }
                }
            }
        });

        final JLabel fileWatch = new JLabel("Files watched: " + 0);
        // theMenu.add(fileWatch);

        final JLabel memorylabel = new JLabel("  "); // "RAM: " +
        // Runtime.getRuntime().totalMemory()
        // / 1024 / 1024 + "
        // MB");

        theMenu.add(memorylabel);

        final JProgressBar mProgressBar = new JProgressBar(0, 1000);
        mProgressBar.setStringPainted(true);

        mProgressBar.setMaximumSize(new Dimension(100, mProgressBar.getPreferredSize().height));
        theMenu.add(mProgressBar);

        Thread t = new Thread(new Runnable() {

            public void run() {

                final Runtime rt = Runtime.getRuntime();
                double free = 0;
                double total = 1;
                // boolean heart = true;
                while (true) {

                    boolean ds9Status = SAMPUtilities.isClientAvailable("DS9");
                    ds9Label.setEnabled(ds9Status);

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        myLogger.warn("Error in memory monitoring thread.", e);

                    }

                    if (getmBrowserPanel() != null && getmBrowserPanel().mRootDirectory != null
                            && getmBrowserPanel().mRootDirectory.exists()) {

                        free = getmBrowserPanel().mRootDirectory.getFreeSpace();

                        total = getmBrowserPanel().mRootDirectory.getTotalSpace();
                        if (total != 0) {
                            int progress = (int) ((total - free) / total * 1000);

                            mProgressBar.setValue(progress);
                            if (progress < 800)
                                mProgressBar.setForeground(GUIConsts.GoodStatusBackgroundColor);
                            else
                                mProgressBar.setForeground(GUIConsts.WarnStatusBackgroundColor);
                            mProgressBar.setString(String.format("%6.2f TB free", free / 1000. / 1000 / 1000 / 1000));

                        }

                    }
                }
            }

        });
        t.setName("Memory Display Thread");
        t.start();

        return theMenu;

    }

    private void onExit() {
        SAMPUtilities.onExit();
        Preferences.thePreferences.storeWindowLocation(this, PROP_WINDOWLOCATION_ROOT);
        Preferences.thePreferences.storeWindowLocation(this.ToolBoxFrame, PROP_WINDOWLOCATION_TOOLBOX);
        Preferences.thePreferences.storeWindowLocation(this.DonutFrame, PROP_WINDOWLOCATION_WAVEFRONT);
        Preferences.thePreferences.save();
    }

    private static void initSampHub() {

        SAMPUtilities.initHubConnector("ODI File Browser", "ODI File Browser & Image Analysis tool.", true);

        SAMPUtilities.getHubConnector().addMessageHandler(new AbstractMessageHandler("odi.otalistener.displayedImage") {
            public Map processCall(HubConnection c, String senderId, Message msg) {
                myLogger.debug("SAMP message handler");
                if (getmBrowserPanel() != null) {
                    getmBrowserPanel().setDisplayedImage((String) msg.getParam("fname"));
                }

                return null;
            }
        });

        SAMPUtilities.getHubConnector().addResponseHandler(new ResponseHandler() {
            @Override
            public boolean ownsTag(String s) {
                if (s.toLowerCase().contentEquals("donut")) {
                    return true;
                }

                return false;
            }

            @Override
            public void receiveResponse(HubConnection hubConnection, String responderID, String tag, Response msg) throws Exception {

                System.out.println("Received donut response: " + msg);
                if (msg.isOK()) {

                    String result = (String) msg.getResult().get("value");
                    System.out.println("Message result has value: " + result);
                    Pattern pattern = Pattern.compile("\\{(.+)\\}\\s+\\{(.+)\\}\\s+\\{(.+)\\}\\s+\\{(.*)\\}");
                    Matcher matcher = pattern.matcher(result);
                    if (matcher.matches()) {
                        String fname = matcher.group(1);

                        // get rid of the extension identifier - we do not support that yet!
                        fname = fname.replaceAll("\\[.*\\]", "");

                        Double x = Double.parseDouble(matcher.group(2));
                        Double y = Double.parseDouble(matcher.group(3));
                        String ext = (matcher.group(4));
                        System.out.println(String.format("Fname %s x %f y %f  ext %s", fname, x, y, ext));
                        pyDonutBridge newtask = new pyDonutBridge(new File(fname), false, x.intValue(), y.intValue(), 200);
                        newtask.setResultListener(DonutFrame);
                        pyDonutBridge.submitTask(newtask);
                    }
                }

            }
        });


        // imexam
        SAMPUtilities.getHubConnector().addResponseHandler(new ResponseHandler() {
            @Override
            public boolean ownsTag(String s) {
                if (s.toLowerCase().contentEquals("imexam")) {
                    return true;
                }

                return false;
            }

            @Override
            public void receiveResponse(HubConnection hubConnection, String responderID, String tag, Response msg) throws Exception {

                if (msg.isOK()) {

                    String result = (String) msg.getResult().get("value");
                    System.out.println("Message result has value: " + result);

                    try {
                        StringTokenizer tok = new StringTokenizer(result);
                        String key = tok.nextToken();

                        ImageContainer im = new ImageContainer(tok.nextToken(""));
                        Vector<ImageContainer> v = new Vector<ImageContainer>();
                        v.add(im);
                        if (!key.equalsIgnoreCase("Q")) {
                            SAMPUtilities.getDS9ImageCutout("imexam", 50);
                            mToolBoxPanel.pushImageBufferSelection(v);
                        }
                    } catch (Exception e) {
                        myLogger.error ("Something wrong with response for iexam: cannot get all the tokens " + result, e);
                    } finally {

                    }


                }

            }
        });

    }

    private static void parseArgs(String[] args) {
        Options options = new Options();

        options.addOption("debug", false, "Debug");
        options.addOption("h", false, "show help");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("debug")) {

                Logger.getRootLogger().setLevel(Level.DEBUG);
            }

        } catch (Exception e) {
            myLogger.warn("Command line parsing error!");

        }

    }

    public static void main(String[] args) {

        PropertyConfigurator.configure(
                VisualFitsBrowserApp.class.getClassLoader().getResourceAsStream("resources/VisualFitsBrowser.log4j"));

        parseArgs(args);

        GUIConsts.setLookAndFeel();

        System.out.println("VisualFitsBrowser Version " + getVersion());
        System.out.println("(c) 2017 Daniel Harbeck, cowjumping.org");
        System.out.println("Starting Samp Interface ...");
        initSampHub();

        System.out.println("Starting Directory Browser & Listener ...");
        @SuppressWarnings("unused")
        VisualFitsBrowserApp b = new VisualFitsBrowserApp();

        System.out.println("Registering functions with SAMP ...");
        SAMPUtilities.getHubConnector().addMessageHandler(new AbstractMessageHandler("odi.iraf.imageLoadReply") {
            public Map processCall(HubConnection c, String senderId, Message msg) {

                if (msg.getParam("msgid") == null) {
                    myLogger.info("Invalid image return message for awaiting: " + IRAF_MSGID + "\n Got event: " + msg);
                    return null;
                }
                long msgID = Integer.parseInt((String) msg.getParam("msgID"));
                if (msgID != IRAF_MSGID) {
                    myLogger.debug("image load response does not match a request of mine: " + msgID + " != "
                            + IRAF_MSGID + " . Ignoring");
                }

                myLogger.debug("SAMP message handler: Reply to loaded Image received.");

                return null;
            }
        });


        SAMPUtilities.getHubConnector().declareSubscriptions(SAMPUtilities.getHubConnector().computeSubscriptions());

        System.out.println("File Browser is up and running.");

    }


    public static FileBrowserPanel getmBrowserPanel() {
        return mBrowserPanel;
    }

    public static void setmBrowserPanel(FileBrowserPanel mBrowserPanel) {
        VisualFitsBrowserApp.mBrowserPanel = mBrowserPanel;
    }

    class StreamGobbler extends Thread {
        InputStream is;
        String type;

        StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (type.matches("stderr"))
                        myLogger.error(line);
                    if (type.matches("stdout"))
                        myLogger.debug(line);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private static String getVersion() {

        String v = "";
        try {
            InputStream i = VisualFitsBrowserApp.class.getResourceAsStream("/resources/properties");
            if (i == null) {
                myLogger.warn("Could not read version file.");
                return v;
            }
            java.util.Properties p = new Properties();
            p.load(i);
            v = p.getProperty("version");
        } catch (Exception e) {
            myLogger.error("While reading version number: ", e);
        }

        return v;
    }
}
