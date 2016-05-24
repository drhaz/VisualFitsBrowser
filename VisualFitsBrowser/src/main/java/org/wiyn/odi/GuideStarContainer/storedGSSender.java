package org.wiyn.odi.GuideStarContainer;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

/**
 * A little utility class that is reading a previously crated guide star catlog
 * steam dump to a gudie star receiver.
 * 
 * @author harbeck
 * 
 */

public class storedGSSender {

    private final static Logger myLogger = Logger
	    .getLogger (storedGSSender.class);

    public static void transmitStoredGSFile (String fname, String targetHost) {

	try {

	    FileInputStream in = new FileInputStream (fname);

	    Socket mySocket = new Socket (targetHost,
		    GuideStarReceiver.DEFAULT_PORT);
	    OutputStream out = mySocket.getOutputStream ();
	    int n;
	    byte[] buffer = new byte[4096];
	    
	    while ((n = in.read (buffer)) > 0) {

		out.write (buffer, 0, n);

	    }

	    out.close ();
	    mySocket.close ();
	    in.close ();

	} catch (Exception e) {
	    myLogger.error ("Error while sending out archived guide star file "
		    + fname + " to host " + targetHost);
	}

    }

    public static void main (String[] args) throws Exception {
	transmitStoredGSFile ("guideStarcatalog.cat", "localhost");
    }
}
