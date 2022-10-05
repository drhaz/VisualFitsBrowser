package org.cowjumping.VisualFitsBrowser.util.FitsComments;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

import java.sql.*;
import java.util.concurrent.*;


public class FITSTextCommentSQLITEImp implements FitsCommentInterface {


    private final static Logger log = LogManager.getLogger(FITSTextCommentSQLITEImp.class);

    private Connection conn = null;
    private String dbURL = null;
    private boolean bgop = false;
    private String createstatement = "CREATE TABLE IF NOT EXISTS usercomments (" +
            "filename TEXT PRIMARY KEY, " +
            "comment text)";


    private boolean connectToDB() {
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
        this.setBackgroundOperation(true);
    }

    public void setBackgroundOperation(boolean bgop) {
        this.bgop = bgop;
    }

    public boolean isConnected() {
        return isConnected(false);
    }

    public boolean isConnected(boolean retry) {
        try {

            boolean isOK = (this.conn != null) && (this.conn.isValid(1000));
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
                this.executor.awaitTermination(5, TimeUnit.SECONDS);
                this.conn.close();
                this.conn = null;
            } catch (Exception e) {
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
                } else {
                    log.debug("No entry found for file name " + entry.FName);
                }

                return true;

            } catch (SQLException e) {
                log.error(e);
            }
        } else {
            log.warn("Database is not connected, cannot read comment");
        }
        return false;

    }


    private ExecutorService executor = Executors.newFixedThreadPool(1);


    @Override
    public boolean writeComment(FitsFileEntry e) {

        Callable<Boolean> submisionTask = () -> {


            String sql = "INSERT OR REPLACE INTO usercomments(filename,comment) VALUES(?,?)";

            if (this.isConnected()) {
                try {
                    PreparedStatement pstmt = this.conn.prepareStatement(sql);
                    pstmt.setString(1, e.FName);
                    pstmt.setString(2, e.UserComment);
                    pstmt.executeUpdate();
                } catch (SQLException err) {
                    log.error(err.getMessage());
                    return false;
                }

            }
            return true;

        };
        Future<Boolean> future = executor.submit(submisionTask);

        if (!this.bgop) {
            try {
                boolean retStat = future.get();
                return retStat;
            } catch (Exception ex) {
                log.error(ex.getMessage());
                return false;
            }

        } else {
            return true;
        }

    }
}
