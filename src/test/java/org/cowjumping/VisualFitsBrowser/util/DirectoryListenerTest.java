package org.cowjumping.VisualFitsBrowser.util;

import junit.framework.TestCase;
import org.cowjumping.guiUtils.Preferences;

import java.io.File;
import java.util.Vector;

public class DirectoryListenerTest extends TestCase {

    public void testWaitToAbortReturnsPromptly() throws Exception {
        if (Preferences.thePreferences == null)
            Preferences.initPreferences("VisualFitsBrowserUnitTest");

        File dir = new File(System.getProperty("java.io.tmpdir"));
        DirectoryListener listener = new DirectoryListener(dir, new DirectoryChangeReceiver() {
            public void onDirectoryChanged(File myDirectory) {
            }

            public void addSingleNewItem(File newItem) {
            }

            public Vector<File> getListedFiles() {
                return new Vector<File>();
            }
        });
        Thread t = new Thread(listener);
        t.start();
        Thread.sleep(300); // let the listener reach its sleep

        long start = System.currentTimeMillis();
        listener.waitToabort();
        long elapsed = System.currentTimeMillis() - start;

        t.join(1000);
        assertFalse("listener thread did not stop", t.isAlive());
        assertTrue("waitToabort took " + elapsed + " ms", elapsed < 500);
    }
}
