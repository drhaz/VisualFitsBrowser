package org.wiyn.VisualFitsBrowser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

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
import javax.swing.event.ChangeEvent;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.HubConnection;
import org.wiyn.VisualFitsBrowser.ImageActions.ImageToolBoxPanel;
import org.wiyn.VisualFitsBrowser.util.Filelist2Latex;
import org.wiyn.VisualFitsBrowser.util.ODIFitsFileEntry;
import org.wiyn.guiUtils.GUIConsts;
import org.wiyn.guiUtils.OSXAdapter;
import org.wiyn.guiUtils.Preferences;
import org.wiyn.odi.ODIFitsReader.SAMPUtilities;

@SuppressWarnings("serial")
public class VisualFitsBrowserApp extends JFrame {

	final static String VersionString = "Version 1";
	final static String LOOKANDFEEL = "Ocean";
	private final static Logger myLogger = Logger.getLogger(VisualFitsBrowserApp.class);

	private final static String PROP_WINDOWLOCATION_ROOT = VisualFitsBrowserApp.class.getCanonicalName()
			+ ".WindowLocation";
	private final static String PROP_WINDOWLOCATION_TOOLBOX = VisualFitsBrowserApp.class.getCanonicalName()
			+ ".ToolsBoxWindowLocation";
	private final static String PROP_SHOWUTILITIES = VisualFitsBrowserApp.class.getCanonicalName() + ".SHOWUTILITIES";
	private final static String PROP_AUTODISPLAY = VisualFitsBrowserApp.class.getCanonicalName() + ".AUTODISPLAY";

	/**
	 * Configuration Parameter: Omit all ODI-things from FileBrowser and
	 * configure for flat fits files.
	 * 
	 */

	static public boolean noODI = true;

	/**
	 * Class to Manage & Display image directory
	 * 
	 */
	private static FileBrowserPanel mBrowserPanel = null;
	private static ImageToolBoxPanel mToolBoxPanel = null;
	private JFrame ToolBoxFrame = null;

	private boolean showUtilities;

	/**
	 * A singleton file browser application
	 * 
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

		getContentPane().setBackground(new java.awt.Color(198, 206, 217));
		try {
			Thread.sleep(20);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
					setResizable(false);
					OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("onExit", (Class[]) null));
				} catch (Exception e) {
					myLogger.info("Could not bind to MacOS X Quit Handler. Get a Mac!");

				}
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

	private JMenuBar createTheMenu() {

		JMenuBar theMenu = new JMenuBar();

		JMenu menu = new JMenu("File");
		JMenuItem menuItem;

		menu.getAccessibleContext().setAccessibleDescription("File Menu");
		theMenu.add(menu);

		{
			menuItem = new JCheckBoxMenuItem("ToolBox");
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

		menu.add(new JSeparator());

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
						Filelist2Latex.openLatex("/tmp/" + fName.replace("tex", "pdf"));
					} else {
						myLogger.info("Cannot genreate Latex logsheet since threre is no viable filelist");
					}
				}
			});
		}

		JMenu debugMenu = GUIConsts.getDebugMenu();

		// theMenu.add(debugMenu);

		theMenu.add(Box.createHorizontalGlue());

		final JLabel ds9Label = new JLabel("DS9  ");
		theMenu.add(ds9Label);

		final JLabel fileWatch = new JLabel("Files watched: " + 0);
		theMenu.add(fileWatch);

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
							if (progress < 8000)
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

	public void onExit() {
		Preferences.thePreferences.storeWindowLocation(this, PROP_WINDOWLOCATION_ROOT);
		Preferences.thePreferences.storeWindowLocation(this.ToolBoxFrame, this.PROP_WINDOWLOCATION_TOOLBOX);
		Preferences.thePreferences.save();
	}

	static void initSampHub() {

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

		SAMPUtilities.getHubConnector().addMessageHandler(new AbstractMessageHandler("odi.broadCastProposalID") {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.astrogrid.samp.client.AbstractMessageHandler#processCall
			 * (org.astrogrid.samp.client.HubConnection, java.lang.String,
			 * org.astrogrid.samp.Message)
			 */
			public Map processCall(HubConnection c, String senderId, Message msg) {
				myLogger.info("SAMP message handler got message " + msg);
				// TODO: Insert code to derive directory name for
				// current proposal.
				String newDirectory = Preferences.thePreferences.getProperty("odi.archive.root",
						"/Volumes/odifile/archive/podi");
				Date AstronomerDate = new Date();
				AstronomerDate.setTime(AstronomerDate.getTime() - (12 * 3600 * 1000));
				SimpleDateFormat lagDateFmt = new SimpleDateFormat("yyyy.MM.dd");
				newDirectory += "/" + msg.getParam("PROPID") + "/" + lagDateFmt.format(AstronomerDate);
				File f = new File(newDirectory);
				if (f.exists()) {
					if (getmBrowserPanel() != null) {

						getmBrowserPanel().readDirectory(f);
					}
				} else
					myLogger.warn("Directory " + newDirectory + " does not exist!");

				return null;
			}
		});

	}

	public static void parseArgs(String[] args) {
		Options options = new Options();

		options.addOption("noodi", false, "Ommit ODI-specific GUI items.");
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("noodi")) {

				ODIFitsFileEntry.ArchiveMode = true;
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
		System.out.println("(c) 2016 Daniel Harbeck, WIYN Observatory");
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

	static long IRAF_MSGID = 0;
	static long IrafLastAttemt = 0;

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
