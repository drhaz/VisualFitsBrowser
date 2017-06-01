package org.cowjumping.VisualFitsBrowser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ProgressMonitor;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;
import org.cowjumping.VisualFitsBrowser.ImageActions.OTAFileListListener;
import org.cowjumping.VisualFitsBrowser.util.DirectoryChangeReceiver;
import org.cowjumping.VisualFitsBrowser.util.DirectoryListener;
import org.cowjumping.VisualFitsBrowser.util.ODIFitsFileEntry;
import org.cowjumping.VisualFitsBrowser.util.ODIFitsFileEntry.TRANSFERSTATUS;
import org.cowjumping.guiUtils.GUIConsts;
import org.cowjumping.guiUtils.Preferences;
import org.cowjumping.guiUtils.SAMPUtilities;
import org.cowjumping.guiUtils.ZebraJTable;
import org.cowjumping.guiUtils.TableCellRenderers.BooleanTableCellRenderer;
import org.cowjumping.guiUtils.TableCellRenderers.NumberFormatterCellRenderer;
import org.cowjumping.guiUtils.TableCellRenderers.mDateRenderer;

@SuppressWarnings("serial")
public class FileBrowserPanel extends JPanel implements DirectoryChangeReceiver {

	private final static Logger myLogger = Logger.getLogger(FileBrowserPanel.class.getCanonicalName());

	public String mRootDirectoryString = "/";

	public File mRootDirectory = null;
	// private Date lastUpdate = null;

	private DirectoryListener myDirectoryListener = null;
	public Vector<ODIFitsFileEntry> mImageList;

	private JTable mTable;
	private FitsViewerTableModel mTableDataModel = null;

	private JLabel rootDirLabel;
	// private JButton mFileOpenButton = null;
	// private JButton mReloadButton = null;

	protected int otaX = 0;
	protected int otaY = 0;

	private final String PROP_LASTDIRECTORY = FileBrowserPanel.class.getCanonicalName() + ".LASTDIRECTORY";

	private OTAFileListListener mFileListListener = null;

	private JButton reloadButton = null;

	boolean autoLoadImageToListener = false;

	private static String DisplayedImage = null;


	public FileBrowserPanel() {
		this(null);
	}

	/**
	 * Initialize file browser panel.
	 *
	 * @param mFileListListener a listener to file select events. can be null.
	 */
	FileBrowserPanel(OTAFileListListener mFileListListener) {

		super();
		this.mFileListListener = mFileListListener;
		mRootDirectoryString = Preferences.thePreferences.getProperty(PROP_LASTDIRECTORY, mRootDirectoryString);

		mRootDirectory = new File(this.mRootDirectoryString);

		mImageList = new Vector<ODIFitsFileEntry>(50);

		this.setLayout(new BorderLayout());

		{

			rootDirLabel = new JLabel(mRootDirectory.getAbsolutePath());
			rootDirLabel.setFont(GUIConsts.TitleFont);
			rootDirLabel.setHorizontalAlignment(SwingConstants.CENTER);
			rootDirLabel.setOpaque(true);
			rootDirLabel.setBackground(GUIConsts.InformationBackgroundColor);

			ImageIcon reload = GUIConsts.getIcon("/resources/icons/reload.png", 18);

			reloadButton = new JButton(reload);
			reloadButton.setPreferredSize(reloadButton.getMinimumSize());
			reloadButton.setOpaque(false);
			reloadButton.setFocusPainted(false);
			// reloadButton.setRolloverEnabled(false);
			reloadButton.setOpaque(false);
			reloadButton.setContentAreaFilled(false);
			reloadButton.setBorderPainted(false);
			reloadButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			reloadButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					reload();

				}

			});
			Box topBox = Box.createHorizontalBox();
			topBox.setOpaque(true);
			topBox.setBackground(GUIConsts.InformationBackgroundColor);
			topBox.add(reloadButton);
			topBox.add(rootDirLabel);
			topBox.add(Box.createHorizontalGlue());
			add(topBox, BorderLayout.NORTH);

		}

		{
			mTable = new ZebraJTable(mTableDataModel) {
				public String getToolTipText(MouseEvent e) {
					String tip = null;
					java.awt.Point p = e.getPoint();
					int rowIndex = rowAtPoint(p);
					int colIndex = columnAtPoint(p);
					int realColumnIndex = convertColumnIndexToModel(colIndex);

					if ((rowIndex >= 0) && (colIndex >= 0)
							&& ((realColumnIndex == FitsViewerTableModel.USERCOMMENT_COL)
							|| (realColumnIndex == FitsViewerTableModel.OBJECT_COL)
							|| (realColumnIndex == FitsViewerTableModel.FNAME_COL))) {
						Object o = getValueAt(rowIndex, colIndex);
						tip = o != null ? (String) o : "";

					} else {
						tip = super.getToolTipText(e);
					}
					return tip;
				}
			};

			mTableDataModel = new FitsViewerTableModel();
			mTable.setModel(mTableDataModel);
			mTable.setFillsViewportHeight(true);
			mTable.setRowSorter(new TableRowSorter<FitsViewerTableModel>(mTableDataModel));
			// Start out with a sort order where the newest iamge is on top of
			// the list.
			mTable.getRowSorter().toggleSortOrder(FitsViewerTableModel.DATEOBS_COL);
			mTable.getRowSorter().toggleSortOrder(FitsViewerTableModel.DATEOBS_COL);

			mTable.getColumnModel().getColumn(FitsViewerTableModel.DATEOBS_COL)
					.setCellRenderer(new mDateRenderer(0, false));

			mTable.getColumnModel().getColumn(FitsViewerTableModel.TEXP_COL)
					.setCellRenderer(new NumberFormatterCellRenderer("% 5.1f", "% 5.3f"));

			mTable.getColumnModel().getColumn(FitsViewerTableModel.AIRMASS_COL)
					.setCellRenderer(new NumberFormatterCellRenderer("%2.1f"));



			mTable.getColumnModel().getColumn(FitsViewerTableModel.FNAME_COL).setCellRenderer(new ImageIDRenderer());

			// mTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			mTable.setFont(GUIConsts.TTFont16);
			mTable.setRowHeight(GUIConsts.TTFont16.getSize() + 8);

			mTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
			JScrollPane scrollPane = new JScrollPane(mTable);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);


			mTable.getColumnModel().getColumn(FitsViewerTableModel.FNAME_COL).setPreferredWidth(230);
			mTable.getColumnModel().getColumn(FitsViewerTableModel.OBJECT_COL).setPreferredWidth(300);
			mTable.getColumnModel().getColumn(FitsViewerTableModel.TEXP_COL).setPreferredWidth(70);
			mTable.getColumnModel().getColumn(FitsViewerTableModel.FILTER_COL).setPreferredWidth(100);
			mTable.getColumnModel().getColumn(FitsViewerTableModel.AIRMASS_COL).setPreferredWidth(45);
			mTable.getColumnModel().getColumn(FitsViewerTableModel.DATEOBS_COL).setPreferredWidth(100);
			mTable.getColumnModel().getColumn(FitsViewerTableModel.USERCOMMENT_COL).setPreferredWidth(300);


			for (int ii = 0; ii < mTable.getColumnCount(); ii++) {
				TableColumn tc = mTable.getColumnModel().getColumn(ii);
				tc.setMaxWidth(tc.getPreferredWidth());
			}

			// scrollPane.setPreferredSize(new Dimension(1100, 550));
			scrollPane.setPreferredSize(new Dimension(mTable.getMaximumSize().width, 550));
			add(scrollPane, BorderLayout.CENTER);

		}

		{
			// Set up a selection listening model
			mTable.getSelectionModel().addListSelectionListener(new SelectionListener());
		}

		{
			// capture double clicks
			mTable.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {

						int row = (mTable.getSelectedRow());
						if (row >= 0 && row < mTable.getRowCount()) {
							ODIFitsFileEntry selectedFits = mImageList.elementAt(mTable.convertRowIndexToModel(row));
							if (selectedFits != null) {
								String fname = selectedFits.getAbsolutePath();

								SAMPUtilities.loadMosaicDS9(fname, 1);

							}
						}

					}

				}
			});
		}

		readDirectory(mRootDirectory);

	}

	private void hideColumn(JTable table, int c) {
		table.getColumnModel().getColumn(c).setMinWidth(0);
		table.getColumnModel().getColumn(c).setMaxWidth(0);
		table.getColumnModel().getColumn(c).setWidth(0);
	}

	synchronized void setDisplayedImage(final String fname) {

		if (!fname.equals("preimage")) {
			final int lastIndex;
			if (DisplayedImage != null)
				lastIndex = getRowbyName(DisplayedImage);
			else
				lastIndex = -1;

			DisplayedImage = fname.trim();

			try {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {

						int index = FileBrowserPanel.this.getRowbyName(fname);

						if (index >= 0)
							mTableDataModel.fireTableRowsUpdated(index, index);
						if (lastIndex >= 0)
							mTableDataModel.fireTableRowsUpdated(lastIndex, lastIndex);
					}
				});
			} catch (Exception e) {
				myLogger.error("Error while updating table upon displayed image notification");
			}

		}
	}


	private class ImageIDRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
													   int row, int column) {
			JLabel renderer = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
					column);

			Color c = Color.black;

			Font f = renderer.getFont().deriveFont(Font.PLAIN);
			if (DisplayedImage != null && DisplayedImage.trim().equals(((String) value).trim())) {
				c = Color.MAGENTA;
				f = f.deriveFont(Font.BOLD);

			}
			renderer.setForeground(c);
			renderer.setFont(f);

			return renderer;
		}

	}


	public void packColumns(JTable table, int margin) {
		for (int c = 0; c < table.getColumnCount(); c++) {
			packColumn(table, c, margin);
		}
	}

	// Sets the preferred width of the visible column specified by vColIndex.
	// The column
	// will be just wide enough to show the column head and the widest cell in
	// the column.
	// margin pixels are added to the left and right
	// (resulting in an additional width of 2*margin pixels).

	private void packColumn(JTable table, int vColIndex, int margin) {
		// TableModel model = table.getModel ();
		DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
		TableColumn col = colModel.getColumn(vColIndex);
		int width = 0;

		// Get width of column header
		TableCellRenderer renderer = col.getHeaderRenderer();
		if (renderer == null) {
			renderer = table.getTableHeader().getDefaultRenderer();
		}
		Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
		width = comp.getPreferredSize().width;

		// Get maximum width of column data
		for (int r = 0; r < table.getRowCount(); r++) {
			renderer = table.getCellRenderer(r, vColIndex);
			comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false, r,
					vColIndex);
			width = Math.max(width, comp.getPreferredSize().width);
		}

		// Add margin
		width += 2 * margin;

		// Set the width
		col.setPreferredWidth(width);
	}

	public Vector<ODIFitsFileEntry> getSelected() {

		Vector<ODIFitsFileEntry> selected = new Vector<ODIFitsFileEntry>();

		synchronized (mImageList) {
			if (mTable.getSelectedRowCount() > 0) {

				int rows[] = mTable.getSelectedRows();
				for (int row : rows) {

					selected.add(mImageList.elementAt(mTable.convertRowIndexToModel(row)));

				}
			}
		}
		return selected;
	}

	/**
	 * Open a new directory for browsing
	 */

	void selectNewDirectory() {

		// FsURL retVal = null;
		JFileChooser chooser = new JFileChooser();

		if (mRootDirectory != null)
			chooser.setCurrentDirectory(mRootDirectory.getParentFile());

		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = chooser.showOpenDialog(this);

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			myLogger.info("You chose to open this file: " + chooser.getSelectedFile().getAbsolutePath());

			mRootDirectory = chooser.getSelectedFile();

			if (mRootDirectory.exists() && mRootDirectory.isDirectory()) {

				this.rootDirLabel.setText(mRootDirectory.getAbsolutePath());
				this.mRootDirectoryString = mRootDirectory.getAbsolutePath();

				readDirectory(mRootDirectory);

			}

		}
	}

	void reload() {
		if (mRootDirectory != null && mRootDirectory.exists())
			readDirectory(mRootDirectory);

	}

	public void onDirectoryChanged(File f) {

		if (f.getAbsoluteFile().equals(mRootDirectory.getAbsoluteFile())) {
			myLogger.info("Directory changed event registered ");
			readDirectory(this.mRootDirectory);

		}

	}

	int getNumberOfEntries() {
		int retVal = -1;
		if (this.mImageList != null)
			retVal = mImageList.size();
		return retVal;
	}

	Vector<ODIFitsFileEntry> getImageList() {
		return (Vector<ODIFitsFileEntry>) mImageList.clone();
	}

	public Vector<String> getMyUnconfirmedFileIDs() {
		Vector<String> ret = new Vector<String>();

		synchronized (mImageList) {
			for (ODIFitsFileEntry f : mImageList) {
				if (f.TransferStatus != TRANSFERSTATUS.CONFIRMED_PPA) {
					ret.add(f.FName);
				}
			}
		}

		return ret;

	}



	/**
	 * Finds an image entry by name and returns the row of the underlying table
	 * model
	 *
	 * @param Name
	 * @return
	 */
	private int getRowbyName(String Name) {
		int retVal = -1;

		synchronized (mImageList) {
			for (int ii = 0; ii < mImageList.size(); ii++) {
				ODIFitsFileEntry f = mImageList.get(ii);
				if (f != null) {
					if (f.FName.equals(Name))
						return ii;

				}
			}
		}
		return retVal;
	}

	public void addSingleNewItem(File newItem) {

		// Issue at hand is that java only handles last modification date.
		synchronized (mImageList) {
			for (ODIFitsFileEntry test : mImageList) {

				if (test.getAbsolutePath().equals(newItem.getAbsolutePath())) {

					myLogger.warn("new Item " + newItem.getAbsolutePath()
							+ " was already in the file list. Rejecting as duplicate.");
					return;
				}

			}
		}
		final ODIFitsFileEntry e = ODIFitsFileEntry.createFromFile(newItem);

		myLogger.debug("Reacting to addSingleNewItem event for file: " + newItem.getAbsoluteFile()
				+ " \n This file converts to entry: " + e);

		if (e != null) {
			try {
				synchronized (mImageList) {
					mImageList.add(e);
				}

				// Swing Thread-unsafeness safety wrapper here
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {

						mTableDataModel.fireTableRowsInserted(mImageList.size() - 1, mImageList.size() - 1);
					}
				});
			} catch (Exception e1) {
				myLogger.error("Error while adding element to file table", e1);
			} finally {
				myLogger.debug("Autoloading image: " + autoLoadImageToListener);
				if (autoLoadImageToListener) {
					String fname = newItem.getAbsolutePath();
					SAMPUtilities.loadMosaicDS9(fname, 1);

				}
			}
		}

	}

	/**
	 * Read an entire directory in from scratch
	 *
	 * @param RootDirectory
	 */
	void readDirectory(final File RootDirectory) {

		// stop the directory listener.
		if (myDirectoryListener != null) {
			myLogger.debug("Stopping directory listener");
			myDirectoryListener.waitToabort();
			myDirectoryListener = null;
		}

		if (mImageList != null) {
			mImageList.clear();
			try {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						mTableDataModel.fireTableDataChanged();
					}
				});
			} catch (Exception e) {
				myLogger.error("Error whle notifying table about table clean");
			}
		}

		myLogger.info("Reading  directory  in " + RootDirectory);

		this.mRootDirectory = RootDirectory.getAbsoluteFile();
		this.rootDirLabel.setText(RootDirectory.getAbsolutePath());
		this.mRootDirectoryString = RootDirectory.getAbsolutePath();

		Preferences.thePreferences.setProperty(this.PROP_LASTDIRECTORY, RootDirectory.getAbsolutePath());

		final ProgressMonitor mProgressMonitor = new ProgressMonitor(FileBrowserPanel.this, "Parsing Directory", "", 0,
				50);
		mProgressMonitor.setMillisToDecideToPopup(200);

		new SwingWorker<String, String>() {

			public String doInBackground() {

				// Reset internal image list
				if (mImageList == null) {
					myLogger.warn("Imagelist did not exist. that is strange; fixed it now");
					mImageList = new Vector<ODIFitsFileEntry>();
				}

				// load images in the new directory
				Vector<ODIFitsFileEntry> newList = ODIFitsFileEntry.getImagesInDirectory(RootDirectory,
						mProgressMonitor);

				if (newList != null && newList.size() > 0) {
					mImageList.addAll(newList);
				}

				myLogger.debug("done with reading the directory, notifying table");
				// This should be safe without wrapper since invoked from
				// Swingworker:
				mTableDataModel.fireTableDataChanged();

				packColumn(mTable, 0, 1);
				packColumn(mTable, 1, 1);
				packColumn(mTable, 2, 2);
				packColumn(mTable, 3, 1);

				mProgressMonitor.close();

				// Now that the directory is fully read in, instsnciate a new
				// DirectoryListener.

				myDirectoryListener = new DirectoryListener(RootDirectory, FileBrowserPanel.this);

				new Thread(myDirectoryListener).start();
				return null;
			}
		}.execute();
	}

	private class SelectionListener implements ListSelectionListener {

		SelectionListener() {

		}

		public void valueChanged(ListSelectionEvent e) {

			if (myLogger.isDebugEnabled())
				myLogger.debug("Table change event: " + e);

			if (mFileListListener == null)
				return;

			if (!e.getValueIsAdjusting()) {

				Vector<ODIFitsFileEntry> selected = getSelected();
				if (selected != null && selected.size() == 1) {

					mFileListListener.pushFileSelection(selected);

				}

			}

		}
	}

	private class FitsViewerTableModel extends AbstractTableModel {


		final static int FNAME_COL = 1;
		final static int OBJECT_COL = 2;
		final static int TEXP_COL = 3;
		final static int FILTER_COL = 4;
		final static int AIRMASS_COL = 5;
		final static int DATEOBS_COL = 6;
		final static int USERCOMMENT_COL = 7;


		boolean displayExtra = false;

		FitsViewerTableModel() {
			super();
		}

		public int getColumnCount() {

			int defaultColumns = 9;
			return displayExtra ? defaultColumns + 1 : defaultColumns;

		}

		public int getRowCount() {
			if (mImageList != null)
				return mImageList.size();
			return 0;
		}

		public Object getValueAt(int row, int col) {

			if (mImageList != null && mImageList.size() > row) {
				ODIFitsFileEntry entry = mImageList.elementAt(row);



				if (col == FNAME_COL) {
					String prefix;
					prefix = " ";
					if (entry.isBinned)
						prefix = "\u00B7";
					return prefix + entry.FName;
				}

				if (col == OBJECT_COL)
					return entry.ObjName;

				if (col == FILTER_COL)
					return entry.Filter;

				if (col == TEXP_COL)
					return entry.ExpTime;


				if (col == DATEOBS_COL)
					return entry.DateObs;



				if (col == USERCOMMENT_COL)
					return entry.UserComment;



				if (col == AIRMASS_COL)
					return entry.Airmass;


			}

			return null;
		}

		@Override
		public void setValueAt(Object Value, int row, int col) {

			if (mImageList != null && mImageList.size() > row && row >= 0) {
				ODIFitsFileEntry entry = null;
				switch (col) {



					case USERCOMMENT_COL:

						entry = mImageList.elementAt(row);
						entry.UserComment = (String) Value;
						entry.writeBackMetaInformation();
						break;



					default:
						myLogger.warn("SetValueAt request for non-editable field");

				}
				fireTableCellUpdated(row, col);
			} else {
				myLogger.warn("SetvalueAt for row/col " + row + "/" + col + " failed");
			}
		}

		public String getColumnName(int col) {



			if (col == FNAME_COL)
				return "Filename";
			if (col == OBJECT_COL)
				return "OBJECT";

			if (col == FILTER_COL)
				return "Filter";
			if (col == TEXP_COL)
				return "Exp Time";

			if (col == DATEOBS_COL)
				return "DATE_OBS";


			if (col == AIRMASS_COL)
				return "X";

			if (col == USERCOMMENT_COL)
				return "Comment";
			return null;

		}

		public boolean isCellEditable(int row, int col) {



			if (col == USERCOMMENT_COL) {
				return true;
			}

			return false;

		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {


			if (columnIndex == TEXP_COL)
				return Float.class;

			if (columnIndex == DATEOBS_COL)
				return Date.class;
			return super.getColumnClass(columnIndex);
		}

	}

	public Vector<File> getListedFiles() {

		Vector<File> retVec = new Vector<File>();
		synchronized (mImageList) {

			for (ODIFitsFileEntry e : this.mImageList) {
				retVec.add(e.DirectoryFile);
			}

		}

		return retVec;
	}

}

@SuppressWarnings("serial")
class TRANSFERCellRenderer extends JLabel implements TableCellRenderer {

	private static HashMap<ODIFitsFileEntry.TRANSFERSTATUS, ImageIcon> statusIcons;
	private final static int iconSize = 16;

	public static ImageIcon getImageIcon(ODIFitsFileEntry.TRANSFERSTATUS status) {
		ImageIcon retVal = null;

		if (statusIcons == null) {
			statusIcons = new HashMap<ODIFitsFileEntry.TRANSFERSTATUS, ImageIcon>();
			ImageIcon myImage = new ImageIcon(
					FileBrowserPanel.class.getClassLoader().getResource("resources/icons/Folder blue mydocuments.png"));
			statusIcons.put(ODIFitsFileEntry.TRANSFERSTATUS.INDEF, new ImageIcon(
					myImage.getImage().getScaledInstance(iconSize, iconSize, java.awt.Image.SCALE_SMOOTH)));

			myImage = new ImageIcon(
					FileBrowserPanel.class.getClassLoader().getResource("resources/icons/TransportGreenTruck.png"));
			statusIcons.put(ODIFitsFileEntry.TRANSFERSTATUS.CONFIRMED_DTS, new ImageIcon(
					myImage.getImage().getScaledInstance(iconSize, iconSize, java.awt.Image.SCALE_SMOOTH)));

			myImage = new ImageIcon(
					FileBrowserPanel.class.getClassLoader().getResource("resources/icons/icon_Archive.svg.png"));
			statusIcons.put(ODIFitsFileEntry.TRANSFERSTATUS.CONFIRMED_PPA, new ImageIcon(
					myImage.getImage().getScaledInstance(iconSize, iconSize, java.awt.Image.SCALE_SMOOTH)));

			myImage = new ImageIcon(FileBrowserPanel.class.getClassLoader().getResource("resources/icons/Error.png"));
			statusIcons.put(ODIFitsFileEntry.TRANSFERSTATUS.ERROR, new ImageIcon(
					myImage.getImage().getScaledInstance(iconSize, iconSize, java.awt.Image.SCALE_SMOOTH)));
		}

		retVal = statusIcons.get(status);
		if (retVal == null)
			retVal = statusIcons.get(ODIFitsFileEntry.TRANSFERSTATUS.INDEF);
		return retVal;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
												   int rowIndex, int vColIndex) {
		this.setIcon(getImageIcon((ODIFitsFileEntry.TRANSFERSTATUS) value));

		return this;
	}
}
