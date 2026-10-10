package org.cowjumping.VisualFitsBrowser;

import junit.framework.TestCase;
import org.cowjumping.guiUtils.Preferences;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Checks that the file table is only changed on the Swing event thread, whichever thread triggers the change.
 */
public class FileBrowserPanelTest extends TestCase {

    private File dir;
    private FileBrowserPanel panel;
    private final List<String> offEdtEvents = new CopyOnWriteArrayList<String>();

    @Override
    protected void setUp() throws Exception {
        if (Preferences.thePreferences == null)
            Preferences.initPreferences("VisualFitsBrowserUnitTest");

        dir = new File(System.getProperty("java.io.tmpdir"), "vfb-table-test-" + System.nanoTime());
        assertTrue(dir.mkdir());
        for (int i = 1; i <= 3; i++)
            writeFits(new File(dir, "img" + i + ".fits"), "OBJ" + i);

        Preferences.thePreferences.setProperty(
                FileBrowserPanel.class.getCanonicalName() + ".LASTDIRECTORY", dir.getAbsolutePath());

        // Constructed off the event thread, as VisualFitsBrowserApp does at start-up.
        panel = new FileBrowserPanel(null);
        panel.getTableModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (!SwingUtilities.isEventDispatchThread())
                    offEdtEvents.add(Thread.currentThread().getName() + " type=" + e.getType());
            }
        });
        waitForRows(3);
    }

    @Override
    protected void tearDown() throws Exception {
        if (panel != null)
            panel.stopWatching();
        File[] files = dir.listFiles();
        if (files != null)
            for (File f : files)
                f.delete();
        dir.delete();
    }

    public void testInitialListing() throws Exception {
        assertEquals(Arrays.asList("img1.fits", "img2.fits", "img3.fits"), sortedNames());
    }

    public void testNewFileFromBackgroundThreadIsAddedOnEdt() throws Exception {
        final File newFile = new File(dir, "img4.fits");
        writeFits(newFile, "OBJ4");

        Thread t = new Thread(new Runnable() {
            public void run() {
                panel.addSingleNewItem(newFile);
                panel.addSingleNewItem(newFile); // duplicate must be ignored
            }
        }, "fake directory listener");
        t.start();
        t.join();

        waitForRows(4);
        settle();
        assertEquals(4, rowCount());
        assertTrue("table events fired off the event thread: " + offEdtEvents, offEdtEvents.isEmpty());
    }

    public void testFileFromOtherDirectoryIsIgnored() throws Exception {
        File otherDir = new File(dir, "sub");
        assertTrue(otherDir.mkdir());
        final File stray = new File(otherDir, "stray.fits");
        writeFits(stray, "STRAY");
        try {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    panel.addSingleNewItem(stray);
                }
            });
            t.start();
            t.join();
            settle();
            assertEquals(3, rowCount());
        } finally {
            stray.delete();
            otherDir.delete();
        }
    }

    public void testRapidReloadsDoNotDuplicateRows() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                panel.reload();
                panel.reload();
                panel.reload();
            }
        });
        waitForRows(3);
        // Give any (discarded) stale workers time to finish.
        Thread.sleep(500);
        settle();
        assertEquals(3, rowCount());
        assertTrue("table events fired off the event thread: " + offEdtEvents, offEdtEvents.isEmpty());
    }

    // --- helpers ---------------------------------------------------------------------------------------------

    /** Wait until all events queued so far on the event thread have been processed. */
    private static void settle() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
            }
        });
    }

    private int rowCount() throws Exception {
        final int[] n = new int[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                n[0] = panel.getTableModel().getRowCount();
            }
        });
        return n[0];
    }

    private List<String> sortedNames() throws Exception {
        final String[] names = new String[rowCount()];
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                for (int i = 0; i < names.length; i++)
                    names[i] = panel.mImageList.get(i).FName;
            }
        });
        Arrays.sort(names);
        return Arrays.asList(names);
    }

    private void waitForRows(int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        while (rowCount() != expected) {
            if (System.currentTimeMillis() > deadline)
                fail("expected " + expected + " rows, have " + rowCount());
            Thread.sleep(20);
        }
    }

    /** Write a minimal valid FITS file (header only, no data). */
    private static void writeFits(File f, String object) throws IOException {
        StringBuilder sb = new StringBuilder();
        card(sb, "SIMPLE  =                    T");
        card(sb, "BITPIX  =                    8");
        card(sb, "NAXIS   =                    0");
        card(sb, "OBJECT  = '" + object + "'");
        card(sb, "FILTER  = 'r'");
        card(sb, "EXPTIME =                 30.0");
        card(sb, "DATE-OBS= '2024-03-15T03:04:05'");
        card(sb, "END");
        while (sb.length() % 2880 != 0)
            sb.append(' ');
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(sb.toString().getBytes("US-ASCII"));
        } finally {
            out.close();
        }
    }

    private static void card(StringBuilder sb, String content) {
        sb.append(content);
        for (int i = content.length(); i < 80; i++)
            sb.append(' ');
    }
}
