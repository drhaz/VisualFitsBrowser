package org.cowjumping.VisualFitsBrowser.util.FitsComments;

import junit.framework.TestCase;

public class FITSTextCommentSQLITEImpTest extends TestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testClose() {
        FITSTextCommentSQLITEImp db = new FITSTextCommentSQLITEImp("temp.db");
        assertTrue(db!=null);
        assertTrue(db.isConnected());
        db.close();
        assertFalse(db.isConnected());

        db.close();
        assertFalse(db.isConnected());
    }
}