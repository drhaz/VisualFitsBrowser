package org.cowjumping.VisualFitsBrowser.util.FitsComments;

import org.apache.log4j.Logger;
import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;



public class FITSTextCommentSQLITEImp implements FitsCommentInterface {



    private final static Logger log = Logger.getLogger(FITSTextCommentSQLITEImp.class);

    private Connection conn = null;

    public FITSTextCommentSQLITEImp(String Filename) {


        String url = "jdbc:sqlite:" + Filename;

        try {
            this.conn = DriverManager.getConnection(url);
            if (this.conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                log.debug("The driver name is " + meta.getDriverName());
                log.debug("A new database has been created.");
            }

        } catch (SQLException e) {
            log.error(e.getMessage());
            this.conn = null;
        }
    }


    public boolean isConnected () {
        try {
            return (this.conn != null) && (this.conn.isValid(1000));
        } catch (SQLException e) {
            return false;
        }
    }


    public void close() {
        if (this.conn != null) {
            try {
                this.conn.close();
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }



    @Override
    public void readComment(FitsFileEntry entry) {

    }

    @Override
    public void writeComment(FitsFileEntry e) {

    }




}
