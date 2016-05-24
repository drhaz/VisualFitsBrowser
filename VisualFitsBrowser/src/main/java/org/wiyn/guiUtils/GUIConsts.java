package org.wiyn.guiUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class GUIConsts {

	private static final Logger log = Logger.getLogger(GUIConsts.class);
	public static final Font LargeCountDownFont = new Font("Helvetica", Font.PLAIN, 48);
	public static final Font TitleFont = new Font("Helvetica", Font.PLAIN, 24);
	public static final Font LargeFont = new Font("Helvetica", Font.PLAIN, 20);
	public static final Font StatusFont = new Font("Helvetica", Font.PLAIN, 18);
	public static final Font InformationFont = new Font("Helvetica", Font.PLAIN, 14);
	public static final Font SmallStatusFont = new Font("Helvetica", Font.PLAIN, 10);

	public static final Font TTFont12 = new Font("monospaced", Font.PLAIN, 12);

	public static final Font TTFont14 = new Font("Monospaced", Font.PLAIN, 14);

	public static final Font TTFont16 = new Font("Monospaced", Font.PLAIN, 16);

	public static final Color InformationBackgroundColor = new Color(0.9f, 0.9f, 1.00f);

	public static final Color InformationBackgroundColor_Binned = new Color(1.0f, 0.9f, 1.00f);

	public static final Color GoodStatusBackgroundColor = new Color(0.9f, 1.0f, 0.9f);

	public static final Color ErrorStatusBackgroundColor = new Color(1.0f, 0.8f, 0.8f);

	public static final Color WarnStatusBackgroundColor = new Color(0.99f, 0.99f, 0.59f);

	public static final Color IdleStatusBackgroundColor = new Color(1.0f, 1.0f, 0.9f);

	public static final Color NormalFontColor = Color.black;
	public static final Color ErrorFontColor = new Color(0.3f, 0.3f, 0.6f);

	public static Color[] ChartColors = { Color.yellow, Color.green, Color.blue, Color.cyan };

	public final static String iso8601Dateformat = "yyyy-MM-dd'T'HH:mm:ss";
	public final static String shortDateformat = "EEE yyyy-MM-dd HH:mm";
	public final static String verySHortDate = "EEE MMM dd yy";

	static {
		System.setProperty("awt.useSystemAAFontSettings", "on");
		System.setProperty("swing.aatext", "true");
	}

	public static JMenu getDebugMenu() {

		JMenu menu = new JMenu("Debug");
		ButtonGroup group = new ButtonGroup();
		JMenuItem item;

		item = new JRadioButtonMenuItem("Errors only");
		menu.add(item);
		group.add(item);
		item.setSelected(true);
		item.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				Logger.getRootLogger().setLevel(Level.ERROR);
			}

		});

		item = new JRadioButtonMenuItem("Limited");
		menu.add(item);
		group.add(item);

		if (Logger.getRootLogger().isInfoEnabled()) {
			item.setSelected(true);
		}

		item.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				Logger.getRootLogger().setLevel(Level.INFO);
			}

		});

		item = new JRadioButtonMenuItem("All");
		menu.add(item);
		group.add(item);

		if (Logger.getRootLogger().isDebugEnabled()) {
			item.setSelected(true);
		}

		item.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				Logger.getRootLogger().setLevel(Level.ALL);
			}

		});

		return menu;

	}

	/**
	 * Return a label with the ODI Logo as a picture.
	 * 
	 * @return
	 */
	public static JLabel getODIImageLabel() {
		try {
			ImageIcon odiIcon = (new ImageIcon(GUIConsts.class.getResource("/resources/ODILOGObig-alpha.png")));
			JLabel l = new JLabel(odiIcon);
			return l;
		} catch (Exception e) {
			log.error("Could not fine odiIcon");

		}

		return new JLabel(new ImageIcon());
	}

	public static void setOSXExithandler(JFrame f) {
		// try {
		//
		// if (f.getClass().getDeclaredMethod("onExit", (Class[]) null) != null)
		// OSXAdapter.setQuitHandler(f,
		// f.getClass()
		// .getDeclaredMethod("onExit", (Class[]) null));
		// } catch (Exception e) {
		// System.err
		// .println("Setting the QuitHandler for Mac OS X failed. Get a Mac!");
		// }
	}

	/**
	 * Load an image icon from resources/icons/ and rescale it.
	 * 
	 */
	public static ImageIcon getIcon(String resource, int size) {
		ImageIcon icon = null;
		try {
			icon = new ImageIcon(GUIConsts.class.getResource(resource));
			Image newimg = icon.getImage().getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH);
			icon = new ImageIcon(newimg);
		} catch (Exception e) {
			log.error("could not load icon " + resource, e);
			icon = new ImageIcon();
		}
		return icon;
	}

	public static ImageIcon getListViewIcon() {
		return getIcon("/resources/icons/listview.png", 18);
	}

	public static ImageIcon getGraphViewIcon() {
		return getIcon("/resources/icons/graphview.png", 18);
	}

	public static String getOS() {
		String retVal = "inknown";
		String osString = System.getProperty("os.name");
		if (osString != null) {
			if (osString.toUpperCase().contains("LINUX"))
				return "linux";

		}
		return "osx";
	}

	public static void setLookAndFeel() {

		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
