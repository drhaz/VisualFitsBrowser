package org.cowjumping.VisualFitsBrowser.util.FitsComments;

import junit.framework.TestCase;
import org.apache.log4j.BasicConfigurator;
import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

import java.util.logging.Logger;

public class FITSTextCommentSQLITEImpTest extends TestCase {

    public void setUp() throws Exception {
        super.setUp();
        BasicConfigurator.configure();
    }

    public void testClose() {
        FITSTextCommentSQLITEImp db = new FITSTextCommentSQLITEImp("temp.db");
        assertTrue(db!=null);
        assertTrue(db.isConnected());

        assertTrue(db.writeComment(FitsFileEntry.createFromComment("test1.fits.fz", "comment1")));

        assertTrue(db.writeComment(FitsFileEntry.createFromComment("test1.fits.fz", "comment2")));

        assertTrue(db.writeComment(FitsFileEntry.createFromComment("test2.fits", "comment3")));


        FitsFileEntry e = FitsFileEntry.createFromComment("test1.fits.fz","");
        assertTrue(db.readComment(e));
        assertEquals("comment2", e.UserComment);

        e = FitsFileEntry.createFromComment("test2.fits","");
        assertTrue(db.readComment(e));
        db.close();
        assertFalse(db.isConnected());
        assertTrue(db.isConnected(true));
        assertEquals("comment3", e.UserComment);

        db.close();
        assertFalse(db.isConnected());

        db.close();
        assertFalse(db.isConnected());
    }
}