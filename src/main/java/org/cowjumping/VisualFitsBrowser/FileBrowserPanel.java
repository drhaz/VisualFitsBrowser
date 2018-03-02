package org.cowjumping.VisualFitsBrowser;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;
import org.cowjumping.guiUtils.GUIConsts;
import org.cowjumping.guiUtils.Preferences;
import org.cowjumping.guiUtils.SAMPUtilities;
import org.cowjumping.guiUtils.ZebraJTable;
import org.cowjumping.guiUtils.TableCellRenderers.NumberFormatterCellRenderer;
import org.cowjumping.guiUtils.TableCellRenderers.mDateRenderer;

import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;

@SuppressWarnings("serial")
public class FileBrowserPanel extends JPanel implements DirectoryChangeReceiver {

    private final static Logger log = Logger.getLogger(FileBrowserPanel.class.getCanonicalName());

    String mRootDirectoryString = "/";

    /** How long to wait before displaying a newly arrived file; aims to prevent ds0 load errors when laoding while system is still writing the file
     *
     */
    private static int waitMilliSecondsBeforeDS9load = 1000;

    public File mRootDirectory = null;

    private DirectoryListener myDirectoryListener = null;
    public Vector<FitsFileEntry> mImageList;

    private JTable mTable;
    private FitsViewerTableModel mTableDataModel = null;
    private JLabel rootDirLabel;
    private JButton reloadButton = null;
    private JButton tomorrowLabel = null;
    private JButton yesterDayLabel = null;

    private final String PROP_LASTDIRECTORY = FileBrowserPanel.class.getCanonicalName() + ".LASTDIRECTORY";

    private OTAFileListListener mFileListListener = null;

    boolean autoLoadImageToListener = false;
    private static String DisplayedImage = null;


    private void maskButton (JButton b) {
        b.setPreferredSize(b.getMinimumSize());
        b.setOpaque(false);
        b.setFocusPainted(false);
        // reloadButton.setRolloverEnabled(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }

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

        mImageList = new Vector<FitsFileEntry>(50);

        this.setLayout(new BorderLayout());

        {

            rootDirLabel = new JLabel(mRootDirectory.getAbsolutePath());
            rootDirLabel.setFont(GUIConsts.TitleFont);
            rootDirLabel.setHorizontalAlignment(SwingConstants.CENTER);
            rootDirLabel.setOpaque(true);
            rootDirLabel.setBackground(GUIConsts.InformationBackgroundColor);
            rootDirLabel.setToolTipText("This is the current directory");

            ImageIcon reload = GUIConsts.getIcon("/resources/icons/reload.png", 18);

            reloadButton = new JButton(reload);
            reloadButton.setToolTipText("Reload the current directory");
            this.maskButton(reloadButton);
            reloadButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    reload();

                }

            });


            tomorrowLabel = new JButton (">");
            tomorrowLabel.setToolTipText("Load equivalent directory for tomorrow's date");
            this.maskButton(tomorrowLabel);
            yesterDayLabel = new JButton ("<");
            yesterDayLabel.setToolTipText("Load equivalent directory for yesterday's date");

            this.maskButton(yesterDayLabel);

            ActionListener l = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String c = ((JButton) e.getSource()).getActionCommand();
                    int deltadays = 0;

                    if (c.equalsIgnoreCase("<"))
                        deltadays = -1;
                    if (c.equalsIgnoreCase(">"))
                        deltadays = +1;

                   if (deltadays != 0)
                       changeDirectoryDate (deltadays);

                }


            };
            tomorrowLabel.addActionListener (l);
            yesterDayLabel.addActionListener(l);



            Box topBox = Box.createHorizontalBox();
            topBox.setOpaque(true);
            topBox.setBackground(GUIConsts.InformationBackgroundColor);
            topBox.add(reloadButton);
            topBox.add(rootDirLabel);
            topBox.add(Box.createHorizontalGlue());
            topBox.add (yesterDayLabel);
            topBox.add (tomorrowLabel);
            add(topBox, BorderLayout.NORTH);

        }

        {
            mTable = new ZebraJTable(mTableDataModel) {
                public String getToolTipText(MouseEvent e) {
                    String tip;
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

                    } else tip = super.getToolTipText(e);

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

            mTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            mTable.setFont(GUIConsts.TTFont16);
            mTable.setRowHeight(GUIConsts.TTFont16.getSize() + 8);

            mTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
            JScrollPane scrollPane = new JScrollPane(mTable);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);


            mTable.getColumnModel().getColumn(FitsViewerTableModel.FNAME_COL).setPreferredWidth(400);
            mTable.getColumnModel().getColumn(FitsViewerTableModel.OBJECT_COL).setPreferredWidth(200);
            mTable.getColumnModel().getColumn(FitsViewerTableModel.TEXP_COL).setPreferredWidth(70);
            mTable.getColumnModel().getColumn(FitsViewerTableModel.FILTER_COL).setPreferredWidth(100);
            mTable.getColumnModel().getColumn(FitsViewerTableModel.AIRMASS_COL).setPreferredWidth(45);
            mTable.getColumnModel().getColumn(FitsViewerTableModel.DATEOBS_COL).setPreferredWidth(100);
            mTable.getColumnModel().getColumn(FitsViewerTableModel.USERCOMMENT_COL).setPreferredWidth(300);


            for (int ii = 0; ii < mTable.getColumnCount(); ii++) {
                TableColumn tc = mTable.getColumnModel().getColumn(ii);
                tc.setMaxWidth(tc.getPreferredWidth());
            }

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

                    if (e.getClickCount() == 2) {
                        int frame = 0;

                        if (e.getButton() == MouseEvent.BUTTON2) {
                            frame = 1;
                        }
                        if (e.getButton() == MouseEvent.BUTTON3) {
                            frame = 2;
                        }

                        int row = mTable.getSelectedRow();
                        row = mTable.convertRowIndexToModel(row);

                        if (row >= 0 && row < mTable.getRowCount()) {
                            FitsFileEntry selectedFits = mImageList.elementAt(row);
                            if (selectedFits != null) {
                                String fname = selectedFits.getAbsolutePath();


                                boolean funpack = ((e.getModifiersEx() & SHIFT_DOWN_MASK) == SHIFT_DOWN_MASK);


                                SAMPUtilities.loadMEFSaveDS9(fname, frame, funpack);


                            }
                        }

                    }


                }
            });
        }

        readDirectory(mRootDirectory);

    }




    public void sendAllSelectedtods9() {

        StringBuilder sb = new StringBuilder();
        int rows[] = mTable.getSelectedRows();

        if ((rows != null) && (rows.length > 0)) {
            SAMPUtilities.clearAllFrames();

            for (int ii = 0; ii < rows.length; ii++) {
                rows[ii] = mTable.convertRowIndexToModel(rows[ii]);
            }
            boolean ismef = SAMPUtilities.isMEF(mImageList.elementAt(rows[0]).getAbsolutePath());
            for (int ii = 0; ii < rows.length; ii++) {
                String fname = mImageList.elementAt(rows[ii]).getAbsolutePath();
                if (ismef)
                    SAMPUtilities.loadMosaicDS9(fname, ii);
                else
                    SAMPUtilities.loadImageDS9(fname, ii);
            }

            SAMPUtilities.selectFrameDS9(0);
            SAMPUtilities.lockAll();
        }
    }


    public void sendAllSelectedToClipBoard() {
        StringBuilder sb = new StringBuilder();
        int rows[] = mTable.getSelectedRows();

        if ((rows != null) && (rows.length > 0)) {

            for (int ii = 0; ii < rows.length; ii++) {
                int idx = mTable.convertRowIndexToModel(rows[ii]);
                String fname = mImageList.elementAt(idx).getAbsolutePath();
                sb.append(fname + " ");
            }

            StringSelection stringSelection = new StringSelection(sb.toString());
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);
        }

    }


    public void changeDirectoryDate(int deltadays) {
        if (this.mRootDirectoryString != null) {

            Date d = this.getDateComponentofDirectory(this.mRootDirectoryString);
            if (d != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                c.add (Calendar.DATE, deltadays);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                String olddate = sdf.format(d.getTime());
                String newdate = sdf.format(c.getTime());

                log.debug ("Input date: " + d + " transformed to " + c);
                log.info ("other day exchange from " + olddate + " to " + newdate);
                String newDirectory = mRootDirectoryString.replace(olddate, newdate);
                File f = new File (newDirectory);
                if (f.exists() && f.isDirectory())
                    readDirectory (f);
                else
                    log.warn ("other day directory " + newDirectory + " does not exist.");

            }

        }
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
                log.error("Error while updating table upon displayed image notification");
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

    public Vector<FitsFileEntry> getSelected() {

        Vector<FitsFileEntry> selected = new Vector<FitsFileEntry>();

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

            log.info("You chose to open this file: " + chooser.getSelectedFile().getAbsolutePath());

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
            log.info("Directory changed event registered ");
            readDirectory(this.mRootDirectory);

        }

    }

    int getNumberOfEntries() {
        int retVal = -1;
        if (this.mImageList != null)
            retVal = mImageList.size();
        return retVal;
    }

    Vector<FitsFileEntry> getImageList() {
        return (Vector<FitsFileEntry>) mImageList.clone();
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
                FitsFileEntry f = mImageList.get(ii);
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
            for (FitsFileEntry test : mImageList) {

                if (test.getAbsolutePath().equals(newItem.getAbsolutePath())) {

                    log.warn("new Item " + newItem.getAbsolutePath()
                            + " was already in the file list. Rejecting as duplicate.");
                    return;
                }

            }
        }
        final FitsFileEntry e = FitsFileEntry.createFromFile(newItem);

        log.debug("Reacting to addSingleNewItem event for file: " + newItem.getAbsoluteFile()
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
                log.error("Error while adding element to file table", e1);
            } finally {

                final String fname = newItem.getAbsolutePath();
                log.debug("Autoloading image: " + autoLoadImageToListener);
                new Thread (new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep (waitMilliSecondsBeforeDS9load);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        if (autoLoadImageToListener) {

                            SAMPUtilities.loadMEFSaveDS9(fname, 1, false);

                        }
                    }
                }).start();
            }
        }

    }

    /**
     * Read an entire directory in from scratch
     *
     * @param RootDirectory
     */
    private void readDirectory(final File RootDirectory) {

        // stop the directory listener.
        if (myDirectoryListener != null) {
            log.debug("Stopping directory listener");
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
                log.error("Error whle notifying table about table clean");
            }
        }

        log.info("Reading  directory  in " + RootDirectory);

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
                    log.warn("Imagelist did not exist. that is strange; fixed it now");
                    mImageList = new Vector<FitsFileEntry>();
                }

                // load images in the new directory
                Vector<FitsFileEntry> newList = FitsFileEntry.getImagesInDirectory(RootDirectory,
                        mProgressMonitor);

                if (newList != null && newList.size() > 0) {
                    mImageList.addAll(newList);
                }

                log.debug("done with reading the directory, notifying table");
                // This should be safe without wrapper since invoked from
                // Swingworker:
                mTableDataModel.fireTableDataChanged();

                packColumn(mTable, 0, 1);
                packColumn(mTable, 1, 1);
                packColumn(mTable, 2, 2);
                packColumn(mTable, 3, 1);

                mProgressMonitor.close();

                if (getDateComponentofDirectory(RootDirectory.getAbsolutePath()) != null) {
                    tomorrowLabel.setVisible(true);
                    yesterDayLabel.setVisible(true);
                } else {
                    tomorrowLabel.setVisible(false);
                    yesterDayLabel.setVisible(false);
                }

                // Now that the directory is fully read in, instsnciate a new
                // DirectoryListener.

                myDirectoryListener = new DirectoryListener(RootDirectory, FileBrowserPanel.this);

                new Thread(myDirectoryListener).start();
                return null;
            }
        }.execute();
    }


    private Date getDateComponentofDirectory(String name) {

        Date ret = null;

        Pattern p = Pattern.compile(".*\\/([12][890]\\d\\d[01]\\d[0123]\\d).*");
        Matcher m = p.matcher(name);
        String t = null;
        if (m.matches()) {
            try {
                t = m.group(1);
                log.debug ("Found date component: " + t);
                DateFormat df = new SimpleDateFormat("yyyyMMdd");
                ret = df.parse(t);

            } catch (Exception e) {
                log.warn("Error while parsing date in directory name " + t, e);
            }
        } else {
            log.info ("directory string " + name + " does not contain a date");
        }

        return ret;

    }


    private class SelectionListener implements ListSelectionListener {

        SelectionListener() {

        }

        public void valueChanged(ListSelectionEvent e) {

            if (log.isDebugEnabled())
                log.debug("Table change event: " + e);

            if (mFileListListener == null)
                return;

            if (!e.getValueIsAdjusting()) {

                Vector<FitsFileEntry> selected = getSelected();
                if (selected != null && selected.size() == 1) {

                    mFileListListener.pushFileSelection(selected);

                }

            }

        }
    }

    private class FitsViewerTableModel extends AbstractTableModel {


        final static int FNAME_COL = 0;
        final static int OBJECT_COL = 1;
        final static int TEXP_COL = 2;
        final static int FILTER_COL = 3;
        final static int AIRMASS_COL = 4;
        final static int DATEOBS_COL = 5;
        final static int USERCOMMENT_COL = 6;


        boolean displayExtra = false;

        FitsViewerTableModel() {
            super();
        }

        public int getColumnCount() {

            int defaultColumns = 7;
            return displayExtra ? defaultColumns + 1 : defaultColumns;

        }

        public int getRowCount() {
            if (mImageList != null)
                return mImageList.size();
            return 0;
        }

        public Object getValueAt(int row, int col) {

            if (mImageList != null && mImageList.size() > row) {
                FitsFileEntry entry = mImageList.elementAt(row);


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
                FitsFileEntry entry = null;
                switch (col) {


                    case USERCOMMENT_COL:

                        entry = mImageList.elementAt(row);
                        entry.UserComment = (String) Value;
                        entry.writeBackMetaInformation();
                        break;


                    default:
                        log.warn("SetValueAt request for non-editable field");

                }
                fireTableCellUpdated(row, col);
            } else {
                log.warn("SetvalueAt for row/col " + row + "/" + col + " failed");
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

            for (FitsFileEntry e : this.mImageList) {
                retVec.add(e.DirectoryFile);
            }

        }

        return retVec;
    }

}

