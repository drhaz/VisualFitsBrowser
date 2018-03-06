package org.cowjumping.FitsUtils;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

@SuppressWarnings("serial")
/**
 * A data container to hold a 16 bit unsigned image plus descriptors, designed
 * to allow fast handling of fitslike images packages.
 *
 * Images are internally stored in an short[] buffer, and should be interpreted
 * for display as a USHORT Buffered image type. The class holds information
 * about the image x and y dimensions.
 *
 * Metadata are is stored in a Java HashTable, but a series of
 * default setter/getter access methods are declared to enforce conventions in
 * the HasTable. These getter/setter classes also enforce type checking.
 *
 * The package defined also classes for tcp/ip guide star package transfer plus
 * additional traffic control. see GuideStarReceiver/Sender classes.
 *
 * This package originates from the WIYN ODI control software
 *
 *
 */
public class ImageContainer implements Serializable, Comparable<ImageContainer> {

    /**
     * Internal logging capability for this class
     */
    private static final Logger log = Logger.getLogger(ImageContainer.class);

	/*
     * Image container
	 */

    /**
     * Internal fast-access for image X-dimension (NAXIS1)
     */
    protected int imageDimX = 0;

    /**
     * Internal fast-access for image Y-dimension (NAXIS2)
     */
    protected int imageDimY = 0;

    /**
     * Internal buffer to hold the image
     */
    public float[] rawImageBuffer = null;

    /**
     * Hashmap to contain all meta information
     */
    public HashMap<String, Object> MetaInfo;

    /**
     * Utility to hold an entire fits header if needed. For exceptional uses
     * only!
     */
    public Vector<String> completeHeader;

    // private float mag_instrumental;

    public final static int ORIGIN_INDEF = 0;
    public final static int ORIGIN_MANUALPICK = 1;
    public final static int ORIGIN_AUTOPICK = 2;
    public final static int ORIGIN_VIDEO = 3;

    public final static int STARTEXPOSURE = -100;
    public final static int ENDEXPOSURE = -101;
    public final static int STARTCATALOG = -102;
    public final static int ENDCATALOG = -103;
    public final static int OTSTATUSMSG = -110;

    public final static int ENDOFCONNECTION = -201;

    /**
     * Definition of default content plus typisation for the meta data array
     *
     * @author harbeck
     */

    public enum Header {

        // / Keywords describing image geometry etc.

        // Package descriptor
        /**
         * ID of guide star
         */
        ID("ID", Integer.class), //

        GAIAID("GAIAID", Long.class),

        /**
         * Exposure time of the guide star in seconds
         */
        EXPTIME("EXPTIME", Float.class), CYCLE("CYCLE", Integer.class),

        /**
         * Time of exposure for guide star image
         */
        TIMESTAMP("TIMESTAMP", Double.class),

        /**
         * OTA Detector X-coordinatet [0-7] in focal plane
         */
        OTA_X("OTA_X", Short.class),

        /**
         * OTA Detector Y-coordinatet [0-7] in focal plane
         */
        OTA_Y("OTA_Y", Short.class),

        /**
         * Extension number if grabed from a MEF file
         */
        FITSEXTENSIONNUM("EXTNUM", Integer.class),

        /**
         * OTA Cell X-coordinatet [0-7]
         */
        CELL_X("CELL_X", Short.class),

        /**
         * OTA Cell X-coordinatet [0-7]
         */
        CELL_Y("CELL_Y", Short.class),

        /**
         * lower left X origin relative to ccd amplifier of cut out ara
         */
        DATASEC_X("DATASEC_X", Integer.class),
        /**
         * lower left Y origin relative to ccd amplifier of cut out ara
         */
        DATASEC_Y("DATASEC_Y", Integer.class),

        WINDOW_X("WINDOW_X", Short.class), // lower left origin of cut out

        WINDOW_Y("WINDOW_Y", Short.class), // lower left origin of cut out

        /**
         * Binning in X direction
         */
        BINNING_X("BIN_X", Short.class),
        /**
         * Binning in Y diection
         */
        BINNING_Y("BIN_Y", Short.class),

        // /// Keywods describing the object in the image
        /**
         * Descriptor of guide star origin
         */
        ORIGIN("ORIGIN", Integer.class),

        // Object Descriptors
        CENTER_X("CENTER_X", Float.class), // Centroid (X) of object
        CENTER_Y("CENTER_Y", Float.class), // Centroid (Y) of object
        PEAK_X("PEAK_X", Float.class), // Centroid (X) of object
        PEAK_Y("PEAK_Y", Float.class), // Centroid (Y) of object
        FWHM_X("FWHM_X", Float.class), // FWHM in X
        FWHM_X_sec("FWHM_X_sec", Float.class), // FWHM in X
        FWHM_Y("FWHM_Y", Float.class), // FWHM in X
        MOMENT_XY("MOM_XY", Float.class), // MOment Analysis XY
        MOMENT_X("MOM_X", Float.class), // MOment Analysis X
        MOMENT_Y("MOM_Y", Float.class), // MOment Analysis Y
        FLUX("FLUX", Float.class), // Integrated Flux of GS
        PEAK("PEAK", Float.class), // Peak value (includes sky)

        BZERO("BZERO", Integer.class), // Remaining Overscan level
        GAIN("GAIN", Float.class), // Gain in e- / ADU
        RON("RON", Float.class), // Readnoise in e-

        OVLEVEL("OVLVL", Float.class), // Remaining Overscan level
        SUBOVLEVEL("SUBOVLVL", Float.class), // subtracted Overscan levels
        BACKGROUND("BACKGRND", Float.class), // BackGround
        OVNOISE("OVNOIS", Float.class), // Noise in the overscan
        // Value
        BACKNOISE("BCKNOISE", Float.class), // BackGround Noise
        SN("SN", Float.class), // Signal to noise of object
        BADPIXELCOUNT("BPC", Integer.class), // NUmber of bad pixels in image
        ROUNDNESS("ROUND", Float.class), // How round is the object?
        CHI("XI", Float.class), // How good was a gauss fit?
        SHARP("SHARP", Float.class),

        INSTMAG("INSTMAG", Float.class), //
        DEC("DEC", Float.class), // RA of object or image
        RA("RA", Float.class), // DEC of object or image
        ROTOFFSET("ROTOFF", float.class), // rotator angle offset.
        EQUINOX("EQNX", Float.class), // Equinox of coordiantes

        // Guide Catalog & OT setup stuff
        VIABLE("VIABLE", Boolean.class), // Guide star is good enough to use in
        // OT mode
        USE("USE", Boolean.class), // Use guide star for OT mode

        // / Descriptors for meta-information

        NGUIDESTARS("NGUIDESTARS", Integer.class), // Total number of GS
        // Misc generic containers

        OTSHIFTX("otX", Integer.class), OTSHIFTY("otY", Integer.class),;

        private String hash;
        private Class myClass;

        private Header(String hash, Class myClass) {
            this.hash = hash;
            this.myClass = myClass;
        }

        public static Header find(String key) {

            for (Header t : Header.values()) {

                if (t.hash.equals(key))
                    return t;
            }

            return null;
        }

    }

    /**
     * Generate an empty guide star container
     */
    public ImageContainer() {
        MetaInfo = new HashMap<String, Object>();
    }


    public ImageContainer (String listOfPixelValues) {
        this();
        Vector<Float> db = new Vector<Float>();
        StringTokenizer st = new StringTokenizer (listOfPixelValues);
        while (st.hasMoreTokens()) {
            try {
                Float d = Float.parseFloat(st.nextToken());
                db.add(d);
            } catch (Exception e) {
                log.error ("While parsing output from imexam: " + e.getMessage() + " Data were: " +listOfPixelValues);
                break;
            }
        }

        double size = Math.sqrt(db.size());
        if (size * size != db.size()) {
            log.error ("Image dimension does not fit! " + size + " "  + db.size());
            return;
        }


        log.debug ("Got imexam image dimension " + size);
        this.imageDimX = (int) size;
        this.imageDimY = (int) size;
        this.rawImageBuffer =  new float [db.size()];
        for (int ii = 0; ii < db.size(); ii++) {
            this.rawImageBuffer[ii] = db.elementAt(ii);
        }


        db.clear();

    }


    public ImageContainer(ImageContainer gs) {
        this(gs, true);
    }

    /**
     * Clone a guide star container. s
     *
     * @param gs
     */

    public ImageContainer(ImageContainer gs, boolean copyImage) {

        MetaInfo = (HashMap<String, Object>) gs.MetaInfo.clone();
        if (gs.rawImageBuffer != null && copyImage) {
            this.imageDimX = gs.imageDimX;
            this.imageDimY = gs.imageDimY;
            this.rawImageBuffer = new float[gs.rawImageBuffer.length];
            System.arraycopy(gs.rawImageBuffer, 0, this.rawImageBuffer, 0, rawImageBuffer.length);
        }

    }



    /**
     * Create a guide star container that holds only metadata, but has no image.
     *
     * @param xce
     * @param yce
     * @param ra
     * @param dec
     */

    public ImageContainer(float xce, float yce, double ra, double dec) {
        this();
        this.setRA(ra);
        this.setDec(dec);
        this.setCenterX(xce);
        this.setCenterY(yce);
    }

    public double getPixel(int i) {
        double v = rawImageBuffer[i];
        if (v < 0)
            v += Short.MAX_VALUE * 2;
        return v;
    }

    public double getPixel(int x, int y) {

        return getPixel(y * this.getImageDimX() + x);
    }

    protected static int clamp(int value) {
        if (value < 0)
            return 0;
        if (value > 65535)
            return 65535;
        return value;
    }

    public void setPixel(int i, double v) {
        this.rawImageBuffer[i] = (short) (clamp(i) & 0xFFFF);
    }

    public void setPixel(int x, int y, double v) {

        setPixel(y * this.getImageDimX() + x, v);

    }




    public int getDATASEC_X() {

        Object o = MetaInfo.get(Header.DATASEC_X.hash);

        if (o == null) {

            return imageDimX;

        }

        return ((Integer) o);
    }

    public void setDATASEC_Y(int i) {

        MetaInfo.put(Header.DATASEC_Y.hash, new Integer(i));

    }

    public int getDATASEC_Y() {

        Object o = MetaInfo.get(Header.DATASEC_Y.hash);

        if (o == null) {

            return imageDimY;

        }

        return ((Integer) o);
    }

    public void setDATASEC_X(int i) {

        MetaInfo.put(Header.DATASEC_X.hash, new Integer(i));

    }

    // ////////////
    // / Guide Window Identification
    // ///////////////////////

    public short getWindow_Offset_X() {

        Object o = MetaInfo.get(Header.WINDOW_X.hash);

        if (o == null) {

            return -1;

        }

        return ((Short) o);
    }

    public void setWindow_Offset_X(short windowOffsetX) {

        MetaInfo.put(Header.WINDOW_X.hash, new Short(windowOffsetX));

    }

    public short getWindow_Offset_Y() {

        Object o = MetaInfo.get(Header.WINDOW_Y.hash);

        if (o == null) {

            return -1;

        }

        return ((Short) o);
    }

    public void setWindow_Offset_Y(short windowOffsetY) {

        MetaInfo.put(Header.WINDOW_Y.hash, new Short(windowOffsetY));

    }

    public void setBinningX(short bX) {
        MetaInfo.put(Header.BINNING_X.hash, new Short(bX));
    }

    public void setBinningY(short bY) {
        MetaInfo.put(Header.BINNING_Y.hash, new Short(bY));
    }

    public short getBinningX() {
        Object o = MetaInfo.get(Header.BINNING_X.hash);

        if (o == null) {

            return 1;

        }

        return ((Short) o);

    }

    public short getBinningY() {
        Object o = MetaInfo.get(Header.BINNING_Y.hash);

        if (o == null) {

            return 1;

        }

        return ((Short) o);

    }

    // //////////////////////
    // / RA / DEC
    // ///////////////////////

    public double getRA() {

        Object o = MetaInfo.get(Header.RA.hash);

        if (o == null) {

            return new Double(0);

        }

        return new Double(((Float) o).doubleValue());
    }

    public void setRA(Double ra) {
        Float f = new Float(ra);
        MetaInfo.put(Header.RA.hash, f);

    }

    public double getDec() {

        Object o = MetaInfo.get(Header.DEC.hash);

        if (o == null) {

            return 0.;

        }


        return new Double( ((Float) o).doubleValue());
    }

    public void setDec(Double dec) {

        MetaInfo.put(Header.DEC.hash, new Float(dec));

    }

    public void setRotOffset(Float rot) {
        MetaInfo.put(Header.ROTOFFSET.hash, new Float(rot));

    }

    public double getRotOffset() {
        Object o = MetaInfo.get(Header.ROTOFFSET.hash);

        if (o == null) {

            return 0.f;

        }

        return ((Float) o);
    }

    // / Equinox
    public Float getEquinox() {

        Object o = MetaInfo.get(Header.EQUINOX.hash);

        if (o == null) {

            return 2000.f;

        }

        return ((Float) o);
    }

    public void setEquinox(Float eqnx) {

        MetaInfo.put(Header.EQUINOX.hash, new Float(eqnx));

    }

    // /////////////////////////
    // / OTA mode related information
    // //////////////////////////
    public Boolean getViable() {

        Object o = MetaInfo.get(Header.VIABLE.hash);

        if (o == null) {

            return true;

        }

        return ((Boolean) o);
    }

    public void setViable(Boolean b) {

        MetaInfo.put(Header.VIABLE.hash, Header.VIABLE.myClass.cast(b));

    }

    public Boolean getUse() {
        Object o = MetaInfo.get(Header.USE.hash);

        if (o == null) {

            return false;

        }

        return ((Boolean) o);
    }

    public void setUse(Boolean b) {

        MetaInfo.put(Header.USE.hash, Header.USE.myClass.cast(b));

    }

    /**
     * Get the gain of the image in units e- / ADU
     *
     * @return
     */

    public Float getGain() {
        Object o = MetaInfo.get(Header.GAIN.hash);

        if (o == null) {

            return 1.0f;

        }

        return ((Float) o);
    }

    public void setGain(Float b) {

        MetaInfo.put(Header.GAIN.hash, Header.GAIN.myClass.cast(b));

    }

    /**
     * Set the X- dimensions for the image
     *
     * @param imageDimX
     */
    public void setImageDimX(int imageDimX) {
        this.imageDimX = imageDimX;
    }

    /**
     * get the X-dimension of the image
     *
     * @return
     */
    public int getImageDimX() {
        return imageDimX;
    }

    /**
     * get the Y-dimension of the image
     *
     * @return
     */
    public int getImageDimY() {
        return imageDimY;
    }

    /**
     * Return the Y dimension of the image
     *
     * @param imageDimY
     */

    public void setImageDimY(int imageDimY) {
        this.imageDimY = imageDimY;
    }

    public float[] getRawImageBuffer() {
        return rawImageBuffer;
    }

    public void setRawImageBuffer(float[] rawImageBuffer) {
        this.rawImageBuffer = rawImageBuffer;
    }



    public void setImage(float[] ImageBuffer, int dimX, int dimY) {

        this.rawImageBuffer = ImageBuffer;
        this.imageDimX = dimX;
        this.imageDimY = dimY;
    }

    public void setCycle(Integer cycle) {
        MetaInfo.put(Header.CYCLE.hash, Header.CYCLE.myClass.cast(cycle));

    }

    public Integer getCycle() {

        Object o = MetaInfo.get(Header.CYCLE.hash);
        if (o == null)
            return new Integer(-1);
        return (Integer) Header.CYCLE.myClass.cast(o);
    }

    public Integer getNGuideStars() {
        Object o = MetaInfo.get(Header.NGUIDESTARS.hash);
        if (o == null)
            return new Integer(-1);
        return (Integer) Header.NGUIDESTARS.myClass.cast(o);

    }

    public void setNGuideStars(Integer n) {
        MetaInfo.put(Header.NGUIDESTARS.hash, Header.NGUIDESTARS.myClass.cast(n));

    }

    /**
     * Set the center of the guide star centroid
     *
     * @param x
     */
    public void setCenterX(float x) {
        MetaInfo.put(Header.CENTER_X.hash, x);
    }

    public Float getCenterX() {
        Object o = MetaInfo.get(Header.CENTER_X.hash);
        if (o == null)
            return new Float(-1);
        return (Float) Header.CENTER_X.myClass.cast(o);
    }

    public void setCenterY(float y) {
        MetaInfo.put(Header.CENTER_Y.hash, y);
    }

    public Float getCenterY() {
        Object o = MetaInfo.get(Header.CENTER_Y.hash);
        if (o == null)
            return new Float(-1);
        return (Float) Header.CENTER_Y.myClass.cast(o);
    }




    public void setPeakX(float x) {
        MetaInfo.put(Header.PEAK_X.hash, x);
    }

    public Float getPeakX() {
        Object o = MetaInfo.get(Header.PEAK_X.hash);
        if (o == null)
            return new Float(-1);
        return (Float) Header.PEAK_X.myClass.cast(o);
    }

    public void setPeakY(float y) {
        MetaInfo.put(Header.PEAK_Y.hash, y);
    }

    public Float getPeakY() {
        Object o = MetaInfo.get(Header.PEAK_Y.hash);
        if (o == null)
            return new Float(-1);
        return (Float) Header.PEAK_Y.myClass.cast(o);
    }





    public void setBackground(float y) {
        MetaInfo.put(Header.BACKGROUND.hash, y);
    }

    public Float getBackground() {
        Object o = MetaInfo.get(Header.BACKGROUND.hash);
        if (o == null)
            return new Float(0);
        return (Float) Header.BACKGROUND.myClass.cast(o);
    }

    public void setBackNoise(float y) {
        MetaInfo.put(Header.BACKNOISE.hash, y);
    }

    public Float getBackNoise() {
        Object o = MetaInfo.get(Header.BACKNOISE.hash);
        if (o == null)
            return new Float(0);
        return (Float) Header.BACKNOISE.myClass.cast(o);
    }

    public void setOVNoise(float y) {
        MetaInfo.put(Header.OVNOISE.hash, y);
    }

    public Float getOVNoise() {
        Object o = MetaInfo.get(Header.OVNOISE.hash);
        if (o == null)
            return new Float(0);
        return (Float) Header.OVNOISE.myClass.cast(o);
    }

    public void setSUBOVLevel(float y) {
        MetaInfo.put(Header.SUBOVLEVEL.hash, y);
    }

    public Float getSUBOVLevel() {
        Object o = MetaInfo.get(Header.SUBOVLEVEL.hash);
        if (o == null)
            return new Float(0);
        return (Float) Header.SUBOVLEVEL.myClass.cast(o);
    }

    public void setBZERO(int y) {
        MetaInfo.put(Header.BZERO.hash, y);
    }

    public int getBZERO() {
        Object o = MetaInfo.get(Header.BZERO.hash);
        if (o == null)
            return new Integer(0);
        return (Integer) Header.BZERO.myClass.cast(o);
    }

    public void setOVLevel(float y) {
        MetaInfo.put(Header.OVLEVEL.hash, y);
    }

    public Float getOVLevel() {
        Object o = MetaInfo.get(Header.OVLEVEL.hash);
        if (o == null)
            return new Float(0);
        return (Float) Header.OVLEVEL.myClass.cast(o);
    }

    public void setFWHM_X(float x) {
        MetaInfo.put(Header.FWHM_X.hash, x);

    }

    public Float getFWHM_X() {
        Object o = MetaInfo.get(Header.FWHM_X.hash);
        if (o == null)
            return new Float(0);
        return (Float) Header.FWHM_X.myClass.cast(o);
    }

    public Float getFWHM_X_sec() {
        Object o = MetaInfo.get(Header.FWHM_X.hash);
        if (o == null)
            return new Float(0);
        return ((Float) (Header.FWHM_X.myClass.cast(o)) * getBinningX() * 0.11f);
    }

    public void setFWHM_Y(float x) {
        MetaInfo.put(Header.FWHM_Y.hash, x);

    }

    public Float getFWHM_Y() {
        Object o = MetaInfo.get(Header.FWHM_Y.hash);
        if (o == null)
            return new Float(0);
        return (Float) Header.FWHM_Y.myClass.cast(o);
    }

    public void setPeak(float y) {
        MetaInfo.put(Header.PEAK.hash, y);

    }

    public Float getPeak() {
        Object o = MetaInfo.get(Header.PEAK.hash);
        if (o == null)
            return new Float(0);
        return (Float) Header.PEAK.myClass.cast(o);
    }

    public double getSN() {
        Object o = MetaInfo.get(Header.SN.hash);
        if (o == null)
            return new Float(0);
        return (Float) Header.SN.myClass.cast(o);
    }

    public void setSN(Float f) {
        MetaInfo.put(Header.SN.hash, f);
    }

    public Integer getID() {
        Object o = MetaInfo.get(Header.ID.hash);
        if (o == null)
            return (new Integer(-1));
        return (Integer) Header.ID.myClass.cast(o);
    }

    public void setID(Integer f) {
        MetaInfo.put(Header.ID.hash, f);
    }


    public Long getGAIAID() {
        Object o = MetaInfo.get(Header.GAIAID.hash);
        if (o == null)
            return (new Long(-1));
        return (Long) Header.GAIAID.myClass.cast(o);
    }

    public void setGAIAID(Long f) {
        MetaInfo.put(Header.GAIAID.hash, f);
    }

    public Float getFlux() {
        Object o = MetaInfo.get(Header.FLUX.hash);
        if (o == null)
            return new Float(0);
        return (Float) Header.FLUX.myClass.cast(o);
    }

    public void setFlux(Float f) {
        MetaInfo.put(Header.FLUX.hash, f);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("\n");


        sb.append("\t| imageDimX=" + this.imageDimX + "\n");
        sb.append("\t| imageDimY=" + this.imageDimY + "\n");
        for (Iterator<Entry<String, Object>> it = MetaInfo.entrySet().iterator(); it.hasNext(); ) {

            Entry<String, Object> e = it.next();
            sb.append("\t| " + e.getKey() + "=" + e.getValue() + "\n");
        }

        return sb.toString();

    }

    public String Hash2String() {
        StringBuffer sb = new StringBuffer();
        for (Iterator<Entry<String, Object>> it = MetaInfo.entrySet().iterator(); it.hasNext(); ) {

            Entry<String, Object> e = it.next();
            sb.append(e.getKey() + "=" + e.getValue() + "\n");
        }

        return sb.toString();
    }

    public void imageToByteBuffer(ByteBuffer myBBuffer) {

        if (this.rawImageBuffer != null) {

            int max = rawImageBuffer.length;
            if (myBBuffer.capacity() < 2 * max) {
                log.warn("ByteBuffer is too small (" + myBBuffer.capacity() + "< " + 2 * max
                        + ")! Filling is as much as I can." + "\nGuide Star Dump:\n " + this.Hash2String());

                max = myBBuffer.capacity() / 2;
            }

            for (int ii = 0; ii < max; ii++) {
                myBBuffer.putShort((short) this.rawImageBuffer[ii]);

            }

        }

    }

    /**
     * Add a has value from a single name=value line
     */

    public void addHashFromString(String s) {

        StringTokenizer st = new StringTokenizer(s, "=");
        if (st.countTokens() != 2) {
            log.error("Error reading line as a Hashmap entry:" + s);

            return;
        }

        String key = st.nextToken();
        String value = st.nextToken();
        // log.debug ("Setting key/value pair: " + key + " " + value);

        Header h = Header.find(key);
        if (h != null) {
            Object o = null;

            if (h.myClass.equals(Integer.class)) {
                o = Integer.parseInt(value);
            }

            if (h.myClass.equals(Short.class)) {
                o = Short.parseShort(value);
            }

            if (h.myClass.equals(Float.class)) {
                o = new Float(Double.parseDouble(value));
            }

            if (h.myClass.equals(Boolean.class)) {
                o = Boolean.parseBoolean(value);
            }
            if (o != null)
                MetaInfo.put(key, o);
        } else {
            log.warn("Ignoring invalid key/value pair: " + s);
        }

    }

    public void readImageFromByteBuffer(ByteBuffer b, int size) {

        this.rawImageBuffer = new float[size];
        // System.arraycopy (b.asShortBuffer (), 0, rawImageBuffer, 0, size);

        for (int ii = 0; ii < size; ii++) {
            this.rawImageBuffer[ii] = b.getShort();
        }
        log.debug("read " + size + " shorts");
    }

    public ImageContainer cloneTelemetry() {

        ImageContainer gs = new ImageContainer(this, false);

        return gs;
    }






    public static void main(String[] args) {
        BasicConfigurator.configure();
        ImageContainer gs = new ImageContainer();
        gs.setCycle(10);
        gs.setImage(new float[10 * 10], 10, 10);
        gs.addHashFromString("EXPTIME=0.020");

        System.out.println(gs.Hash2String());

    }




    public int compareTo(ImageContainer o) {

        // Compare by magnitude
        // return (int) (this.magnitude - ((OTAGuideStar) o).magnitude);

        // Compare by distance to output amplifier
        return Double.compare(this.getInstMag(), o.getInstMag());

    }



    public Integer getExtension() {
        Object o = MetaInfo.get(Header.FITSEXTENSIONNUM.hash);
        if (o == null)
            return (-1);
        return (Integer) Header.FITSEXTENSIONNUM.myClass.cast(o);
    }

    public void setExtension(Integer f) {
        MetaInfo.put(Header.FITSEXTENSIONNUM.hash, f);
    }

    public Integer getBadPixelCount() {
        Object o = MetaInfo.get(Header.BADPIXELCOUNT.hash);
        if (o == null)
            return (-1);
        return (Integer) Header.BADPIXELCOUNT.myClass.cast(o);
    }

    public void setBadPixelCount(Integer f) {
        MetaInfo.put(Header.BADPIXELCOUNT.hash, f);
    }

    public static void extractThumImage(ImageContainer source, ImageContainer target, int tsize, int centerX,
                                        int centerY) {
        int x1 = centerX - tsize / 2;

        int y1 = centerY - tsize / 2;

        target.setWindow_Offset_X((short) x1);
        target.setWindow_Offset_Y((short) y1);
        float[] myBuffer = new float[tsize * tsize];
        int dimX = source.getImageDimX();
        int dimY = source.getImageDimY();
        for (int yy = 0; yy < tsize; yy++) {
            for (int xx = 0; xx < tsize; xx++) {

                if (x1 + xx >= 0 && x1 + xx < dimX && y1 + yy >= 0 && y1 + yy < dimY) {

                    myBuffer[yy * tsize + xx] = (source.getRawImageBuffer()[(y1 + yy) * dimX + x1 + xx]);

                }
            }
        }

        target.setImage(myBuffer, tsize, tsize);
        target.setWindow_Offset_X((short) (centerX - tsize / 2));
        target.setWindow_Offset_Y((short) (centerY - tsize / 2));
    }

    public Float getRoundness() {
        Object o = MetaInfo.get(Header.ROUNDNESS.hash);
        if (o == null)
            return (1f);
        return (Float) Header.ROUNDNESS.myClass.cast(o);
    }

    public void setRoundness(Float f) {
        MetaInfo.put(Header.ROUNDNESS.hash, f);
    }

    public Float getChi() {
        Object o = MetaInfo.get(Header.CHI.hash);
        if (o == null)
            return (1f);
        return (Float) Header.CHI.myClass.cast(o);
    }

    public void setChi(Float f) {
        MetaInfo.put(Header.CHI.hash, f);
    }

    public Float getSharp() {
        Object o = MetaInfo.get(Header.SHARP.hash);
        if (o == null)
            return (1f);
        return (Float) Header.SHARP.myClass.cast(o);
    }

    public void setSharp(Float f) {
        MetaInfo.put(Header.SHARP.hash, f);
    }

    public float getFloat(Header what) {
        Object o = MetaInfo.get(what.hash);
        if (o == null)
            return 0;
        if (o instanceof Float)
            return (Float) o;

        if (o instanceof Short)
            return ((Short) o);
        return 0f;
    }

    public void setExptime(Float f) {
        MetaInfo.put(Header.EXPTIME.hash, f);
    }

    public Float getExptime() {
        Object o = MetaInfo.get(Header.EXPTIME.hash);
        if (o == null)
            return (1f);
        return (Float) Header.EXPTIME.myClass.cast(o);
    }

    public Float getInstMag() {
        Object o = MetaInfo.get(Header.INSTMAG.hash);
        if (o == null)
            return (Float.NaN);
        return (Float) Header.INSTMAG.myClass.cast(o);
    }

    public void setInstMag(Float f) {
        MetaInfo.put(Header.INSTMAG.hash, f);
    }


    public void setMomentXY(float momentXY) {
        MetaInfo.put(Header.MOMENT_XY.hash, momentXY);

    }

    public float getMomentXY() {
        Object o = MetaInfo.get(Header.MOMENT_XY.hash);
        if (o == null)
            return (Float.NaN);
        return (Float) Header.MOMENT_XY.myClass.cast(o);
    }

    public void setMomentX(float momentX) {
        MetaInfo.put(Header.MOMENT_X.hash, momentX);

    }

    public float getMomentX() {
        Object o = MetaInfo.get(Header.MOMENT_X.hash);
        if (o == null)
            return (Float.NaN);
        return (Float) Header.MOMENT_X.myClass.cast(o);
    }

    public void setMomentY(float momentY) {
        MetaInfo.put(Header.MOMENT_Y.hash, momentY);

    }

    public float getMomentY() {
        Object o = MetaInfo.get(Header.MOMENT_Y.hash);
        if (o == null)
            return (Float.NaN);
        return (Float) Header.MOMENT_Y.myClass.cast(o);
    }


    public void setTimestamp(double ts) {
        MetaInfo.put(Header.TIMESTAMP.hash, ts);
    }

    public double getTimestamp() {
        return (Double) MetaInfo.get(Header.TIMESTAMP.hash);
    }

    public Vector<String> getCompleteHeader() {
        return completeHeader;
    }

    public void setCompleteHeader(Vector<String> completeHeader) {
        this.completeHeader = completeHeader;
    }

}
