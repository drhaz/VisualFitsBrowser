package org.cowjumping.FitsUtils;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

import jsky.coords.DMS;
import jsky.coords.HMS;

import org.apache.log4j.Logger;

public class QuickHeaderInfo {

    private static final Logger myLogger = Logger.getLogger(QuickHeaderInfo.class);
    final static int FITSBLOCKSIZE = 2880;

    /**
     * Quickly reads in the primary header of a fits file and returns it as a
     * Vector of Strings. No Analysis of the header is done.
     *
     * @param f
     * @return
     */
    public static Vector<String> readFITSHeader(File f) {

        Vector<String> retVal = new Vector<String>();
        boolean extend = false;
        char b[] = new char[80];
        if (!f.exists()) {
            myLogger.error("Attempt to read from non-existing file: " + f.getAbsolutePath());
            return retVal;
        }

        myLogger.debug("Start read header");
        int linesRead = 0;
        // Read primary header & first extension
        try {
            BufferedReader r = new BufferedReader(new FileReader(f), FITSBLOCKSIZE * 1);

            // primary header
            while (r.read(b, 0, 80) == 80) {

                String line = new String(b);

                if (line.startsWith("EXTEND") && QuickHeaderInfo.getStringValue(line).equalsIgnoreCase("T")) {

                    extend = true;
                }

                if (line.startsWith("END"))
                    break;


                retVal.add(line);
                linesRead++;
            }

            // first extension header

            if (extend) {
                myLogger.debug("Reading header of first extension");
                while (r.read(b, 0, 80) == 80) {

                    String line = new String(b);
                    if (line.startsWith("END"))
                        break;

                    // Workaround for images that claim to be MEF, but are in fact not and you are swamped with lines
                    // of binary crap.

                    if (!Character.isLetter(line.charAt(0)) && !Character.isSpaceChar(line.charAt(0)))
                        break;

                    retVal.add(line);

                }
            }
            r.close();

        } catch (Exception e) {
            myLogger.error("Error while reading in primary hedaer for file " + f.getAbsolutePath(), e);

        }
        myLogger.debug("End read header " + linesRead);
        return retVal;
    }

    public static Point2D getPointing(Vector<String> header) {
        Point2D ret = new Point2D.Double();
        double ra_ = 0;
        double dec_ = 180;
        if (header != null)

            try {
                // ra_ = Double.parseDouble(getStringValue(header, "CRVAL1"));
                // dec_ = Double.parseDouble(getStringValue(header,"CRVAL2"));
                ra_ = (float) new HMS(getStringValue(header, "RA")).getVal() * 15;
                dec_ = (float) new DMS(getStringValue(header, "DEC")).getVal();

            } catch (Exception e) {
                ra_ = 0;
                dec_ = 180;
            }

        ret.setLocation(ra_, dec_);

        return ret;
    }

    public static double getRotatorOffset(Vector<String> header) {

        double retVal = 0;
        if (header != null) {
            retVal = getFloatValue(header, "ROTOFF");
        }

        return retVal;
    }

    public static Date getDateObs(Vector<String> header, boolean MST) {
        String fc = getStringValue(header, "DATE-OBS");

        if (fc != null) {
            Calendar c = javax.xml.bind.DatatypeConverter.parseDateTime(fc);
            if (c != null)

                return c.getTime();
        }

        return null;
    }

    public static float getExpTime(Vector<String> header) {

        float exptime = -1;
        String fc = null;
        if (header != null)

            for (String fitsCard : header) {

                if (fitsCard != null && (fitsCard.startsWith("EXPTIME") || fitsCard.startsWith("REQ_EXPT"))) {

                    StringTokenizer t = new StringTokenizer(fitsCard, "=/");
                    t.nextToken();
                    exptime = Float.parseFloat(t.nextToken().trim());
                    fc = fitsCard;

                    continue;
                }
            }

        return exptime;
    }

    public static String getLogfileContent(File f) {
        String retVal = "Logfile not available";
        if (f != null && f.isDirectory()) {
            File LogFile = new File(f.getAbsolutePath() + "/exposure.log");
            if (!LogFile.exists()) {
                return retVal;

            }
            StringBuilder text = new StringBuilder();
            Scanner scanner = null;

            try {
                scanner = new Scanner(new FileInputStream(LogFile), "UTF-8");
                while (scanner.hasNextLine()) {
                    text.append(scanner.nextLine() + "\n");
                }

            } catch (Exception e) {
                myLogger.error("Error while reading in logfile: " + LogFile.getAbsolutePath(), e);
                return retVal;
            } finally {
                scanner.close();
                retVal = text.toString();
            }

        }
        return retVal;
    }

    public static Vector<String> getFitsHeader(File dir) {

        Vector<String> retVal = null;
        String tobequeriedFits = null;

        if (dir == null || !dir.exists()) {
            myLogger.warn("getODIHeader was given NULL or non-existent file. Quitting");
            return null;
        }

        if (dir.isDirectory()) {
            // This covers the special case where an image is stored in a directory.
            // Grab the first fits imgae and go with it.
            // Find the first available fits file from an ODI directory to
            // extract fits header

            String match = ".*\\.[Ff][Ii][Tt][Ss]";
            FilenameFilter ff = new RegExMatch(match);

            String[] list = dir.list(ff);

            myLogger.debug("Now parsing directory " + dir.getAbsolutePath() + " for suitable fits images: \n"
                    + (list != null && list.length > 0 ? list[0] : null));

            if (list != null && list.length > 0) {

                tobequeriedFits = dir.getAbsolutePath() + "/" + list[0];

            } else {

                myLogger.debug("Not fits files found. trying .fz files now");

                String matchFZ = match + "\\.fz";
                FilenameFilter ff_fz = new RegExMatch(matchFZ);
                String[] listFZ = dir.list(ff_fz);

                if (listFZ != null && listFZ.length > 0) {
                    tobequeriedFits = dir.getAbsolutePath() + "/" + listFZ[0];

                }

            }

            if (tobequeriedFits == null) {
                myLogger.debug("Could not find any suitable fits files in the sub directory!");
            }

            // End of ODI directory search
        } else {

            // We were given a file, not a directory; check if it is a fits of
            // .fz file, and proceed with it.
            myLogger.debug("Getting header from direct input file");
            if (dir.getName().endsWith(".fits") || dir.getName().endsWith(".fz"))
                tobequeriedFits = dir.getAbsolutePath();
        }

        if (tobequeriedFits != null) {
            myLogger.debug("Now readinging image header from: " + tobequeriedFits);
            retVal = readFITSHeader(new File(tobequeriedFits));
        } else {
            myLogger.error("Could not find a suitable fits file to read hedaer from!");
        }
        return retVal;

    }

    public static String getObject(Vector<String> fitsHeader) {

        String retVal = getStringValue(fitsHeader, "OBJECT");
        if (retVal == null)
            retVal = "";
        return retVal;
    }

    public static String getFilter(Vector<String> fitsHeader) {

        String retVal = getStringValue(fitsHeader, "FILTER");
        if (retVal == null)
            retVal = "";
        return retVal;

    }

    public static int getPONTime(Vector<String> fitsHeader) {
        Integer retVal = getIntValue(fitsHeader, "PONTIME");
        if (retVal == null)
            retVal = 0;
        return retVal;
    }

    public static String getStringKey(String fitsCard) {
        String retVal = null;
        StringTokenizer t = new StringTokenizer(fitsCard, "=/");
        retVal = t.nextToken().trim();

        return retVal;
    }

    public static String getStringValue(String fitsCard) {
        String retVal = null;
        StringTokenizer t = new StringTokenizer(fitsCard, "=/");
        t.nextToken();
        retVal = t.nextToken().trim();

        return retVal;
    }

    public static String getStringValue(Vector<String> fitsHeader, String key) {
        String value = null;
        if (fitsHeader != null) {
            for (String fitsCard : fitsHeader) {
                StringTokenizer t = new StringTokenizer(fitsCard, "=/");
                if ((t.nextToken().trim()).compareToIgnoreCase(key) == 0) {

                    value = (t.nextToken().trim());
                    value = value.replace("\'", "");
                    continue;
                }
            }
        } else {
            myLogger.error("getStringValue: Attempt to search in NULL fitsHeader aborted. Returning null");
            Thread.dumpStack();
        }
        return value;
    }

    public static float getFloatValue(Vector<String> fitsHeader, String key) {
        float value = Float.NaN;
        if (fitsHeader != null) {
            for (String fitsCard : fitsHeader) {
                StringTokenizer t = new StringTokenizer(fitsCard, "=/");
                if ((t.nextToken().trim()).compareToIgnoreCase(key) == 0) {
                    String valueString = (t.nextToken().trim());

                    try {
                        valueString = valueString.replace("\'", "");
                        value = Float.parseFloat(valueString);
                    } catch (Exception e) {
                        value = Float.NaN;
                        myLogger.warn("Error while reading flaot key word: " + valueString);
                    }

                    continue;
                }
            }
        } else {
            myLogger.warn("Attempt to search in NULL fitsHerader aborted. Returning null");
        }
        return value;
    }

    public static boolean getBooleanValue(Vector<String> fitsHeader, String key) {
        boolean value = false;
        String valuestr = getStringValue(fitsHeader, "EXTEND");
        if (valuestr != null) {
            try {
                myLogger.debug("bolean " + key + " " + valuestr);
                value = valuestr.equalsIgnoreCase("T");

            } catch (Exception e) {
                myLogger.error("While boolean parsing " + key + " -> " + valuestr, e);
                value = false;
            }
        }
        return value;
    }

    public static int getIntValue(Vector<String> fitsHeader, String key) {
        int value = 0;
        if (fitsHeader != null) {
            for (String fitsCard : fitsHeader) {
                StringTokenizer t = new StringTokenizer(fitsCard, "=/");
                if ((t.nextToken().trim()).compareToIgnoreCase(key) == 0) {

                    String valueString = (t.nextToken().trim());

                    value = Integer.parseInt(valueString);
                    continue;
                }
            }
        } else {
            myLogger.warn("Attempt to search in NULL fitsHerader aborted. Returning null");
        }
        return value;
    }

    public static boolean isBinned(Vector<String> fitsHeader) {

        String val = getStringValue(fitsHeader, "CCDSUM");
        if (val != null) {

            StringTokenizer st = new StringTokenizer(val, "[]: \t");
            int xbin = Integer.parseInt(st.nextToken());
            int ybin = Integer.parseInt(st.nextToken());
            return (xbin != 1) || (ybin != 1);
        } else
            return false;
    }

    public static Point2D geBinning(Vector<String> fitsHeader) {
        String val = getStringValue(fitsHeader, "CCDSUM");
        if (val != null) {

            StringTokenizer st = new StringTokenizer(val, "[]: \t");
            int xbin = Integer.parseInt(st.nextToken());
            int ybin = Integer.parseInt(st.nextToken());
            return new Point2D.Double(xbin, ybin);
        } else
            return null;

    }

}
