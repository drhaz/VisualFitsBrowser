package org.cowjumping.VisualFitsBrowser.util.FitsComments;

import junit.framework.TestCase;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

import java.io.File;

public class FITSTextCommentSQLITEImpTest extends TestCase {

    final static String TESTDATABASENAME = "temp.db";

    public void setUp() throws Exception {
        super.setUp();
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.DEBUG);
    }

    public void testClose() {
        try {
            FitsFileEntry e = null;
            FITSTextCommentSQLITEImp db = new FITSTextCommentSQLITEImp(TESTDATABASENAME);
            db.setBackgroundOperation(false);
            assertTrue(db != null);
            assertTrue(db.isConnected());

            assertTrue(db.writeComment(FitsFileEntry.createFromComment("test1.fits.fz", "comment1")));

            e = FitsFileEntry.createFromComment("test1.fits.fz", "");
            assertEquals("", e.UserComment);

            assertTrue(db.readComment(e));
            assertEquals("comment1", e.UserComment);

            assertTrue(db.writeComment(FitsFileEntry.createFromComment("test1.fits.fz", "comment2")));
            assertTrue(db.writeComment(FitsFileEntry.createFromComment("test2.fits", "comment3")));


            e = FitsFileEntry.createFromComment("test1.fits.fz", "");
            assertTrue(db.readComment(e));
            assertEquals("comment2", e.UserComment);

            e = FitsFileEntry.createFromComment("test2.fits", "");
            assertTrue(db.readComment(e));
            assertEquals("comment3", e.UserComment);

            assertTrue(db.writeComment(FitsFileEntry.createFromComment("test4.fits", "comment4")));
            e = FitsFileEntry.createFromComment("test4.fits", "");
            assertTrue(db.readComment(e));
            assertEquals("comment4", e.UserComment);

            db.close();
            assertFalse(db.isConnected());
            assertTrue(db.isConnected(true));


            db.close();
            assertFalse(db.isConnected());

            db.close();
            assertFalse(db.isConnected());


        } catch (Exception er) {

        } finally {
            File f = new File(TESTDATABASENAME);
            f.delete();
        }

    }
}
