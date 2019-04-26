package org.cowjumping.VisualFitsBrowser.util.FitsComments;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;
import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

import java.sql.*;


public class FITSTextCommentSQLITEImp implements FitsCommentInterface {


    private final static Logger log = Logger.getLogger(FITSTextCommentSQLITEImp.class);

    private Connection conn = null;
    private String dbURL = null;

    private String createstatement = "CREATE TABLE IF NOT EXISTS usercomments (" +
            "filename TEXT PRIMARY KEY, " +
            "comment text)";



    private boolean connectToDB () {
        try {
            this.conn = DriverManager.getConnection(this.dbURL);
            if (this.conn != null) {
                Statement stmt = conn.createStatement();
                stmt.execute(createstatement);
            } else {
                return false;
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            this.conn = null;
            return false;
        }
        return true;

    }

    public FITSTextCommentSQLITEImp(String Filename) {


        this.dbURL = "jdbc:sqlite:" + Filename;
        this.connectToDB();

    }


    public boolean isConnected() {
        return isConnected(false);
    }

    public boolean isConnected(boolean retry) {
        try {

            boolean isOK =  (this.conn != null) && (this.conn.isValid(1000));
            if (!isOK & retry) {
                return this.connectToDB();
            } else
                return isOK;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void close() {
        if (this.conn != null) {
            try {
                this.conn.close();
                this.conn=null;
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }


    @Override
    public boolean readComment(FitsFileEntry entry) {
        String sql = "SELECT filename,comment from usercomments where filename = ?";

        if (isConnected()) {


            try {
                PreparedStatement stmt = this.conn.prepareStatement(sql);
                stmt.setString(1, entry.FName);
               ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    entry.UserComment = rs.getString(2);
                }

                return true;

            } catch (SQLException e) {
                log.error(e);
            }
        }
        return false;

    }

    @Override
    public boolean writeComment(FitsFileEntry e) {

        String sql = "INSERT OR REPLACE INTO usercomments(filename,comment) VALUES(?,?)";

        if (this.isConnected()) {
            try {
                PreparedStatement pstmt = this.conn.prepareStatement(sql);
                pstmt.setString(1, e.FName);
                pstmt.setString(2, e.UserComment);
                pstmt.executeUpdate();
            } catch (SQLException err) {
                log.error(err.getMessage());
                return (false);
            }
        }
        return (true);
    }
}