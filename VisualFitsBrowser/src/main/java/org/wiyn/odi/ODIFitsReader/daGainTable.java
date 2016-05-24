package org.wiyn.odi.ODIFitsReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class daGainTable {

	protected float[] gain = new float[64 * 64];
	private static final Logger myLogger = Logger.getLogger(daGainTable.class);
	private static daGainTable theGainTable = null;
	
	public static daGainTable getTable () {
		if (theGainTable == null)
			theGainTable = new daGainTable();
		return theGainTable;
	}
	private daGainTable() {

		InputStream in = daGainTable.class.getClassLoader()
				.getResourceAsStream("resources/podinoisegain.dat");
		BufferedReader myReader = new BufferedReader(new InputStreamReader(in));
		String s = null;
		try {
			while ((s = myReader.readLine()) != null) {
				if (s.startsWith("#"))
					continue;

				StringTokenizer tok = new StringTokenizer(s, "\t ");
				int otaX = Integer.parseInt(tok.nextToken());
				int otaY = Integer.parseInt(tok.nextToken());

				int cellX = Integer.parseInt(tok.nextToken());
				int cellY = Integer.parseInt(tok.nextToken());

				float cellGain = Float.parseFloat(tok.nextToken()); 
				setGain (otaX, otaY, cellX, cellY, cellGain);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			myLogger.error("Error while trying to read gain file", e);
		}

	}

	void setGain(int otax, int otay, int cellx, int celly, float cellgain) {
		gain[(otay *8 + celly) * 64 + otax * 8 + cellx] = cellgain;
	}

	
	public float getGain (int otax, int otay, int cellx, int celly) {
		float cellGain = gain[(otay *8 + celly) * 64 + otax * 8 + cellx];
		if (cellGain < 0 || cellGain > 5) {
			cellGain = 1;
		}
		return cellGain;
		
	}
	
	public static void main(String args[]) {
		System.out.println ("Gain: " + daGainTable.getTable().getGain(0, 0, 5, 5));
		System.exit(0);
	}
}
