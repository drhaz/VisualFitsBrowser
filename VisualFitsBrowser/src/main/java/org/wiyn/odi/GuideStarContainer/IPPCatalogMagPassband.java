package org.wiyn.odi.GuideStarContainer;

/**
 * This class is a named decription of the IPP catalog passbands used for ODI.
 * 
 * @author harbeck
 * 
 */

public class IPPCatalogMagPassband {

    public int id = 0;
    public String name = null;

    public static IPPCatalogMagPassband INDEF = new IPPCatalogMagPassband (0, "INDEF");

    public static IPPCatalogMagPassband IPP_SDSSg = new IPPCatalogMagPassband (1,
	    "SDSS g");
    public static IPPCatalogMagPassband IPP_SDSSr = new IPPCatalogMagPassband (2,
	    "SDSS r");
    public static IPPCatalogMagPassband IPP_SDSSi = new IPPCatalogMagPassband (3,
	    "SDSS i");
    public static IPPCatalogMagPassband IPP_SDSSz = new IPPCatalogMagPassband (4,
	    "SDSS z");

    /**
     * Instrumental magnitude, as measured from an actual observation.
     * 
     * This magnitude will be relevant when a guide star configuration is
     * created from a short ODI pre-exposure. Zeropoint is assumed to be 0,
     * i.e., magnitudes are negative. Magnitude is normalized for the exposure
     * time.
     * 
     */
    public static IPPCatalogMagPassband ODI_INSTRUMENTAL = new IPPCatalogMagPassband (
	    99, "Instrumental");

    private IPPCatalogMagPassband(int id, String name) {
	this.id = id;
	this.name = name;
    }

    public boolean isEqual (IPPCatalogMagPassband o) {

	return (o.id == this.id);
    }

    public String toString () {
	return this.name;
    }

}

// public static final int PASSBAND_INDEF = 0;
// public static final int PASSBAND_SDSSu = 1;
// public static final int PASSBAND_SDSSg = 2;
// public static final int PASSBAND_SDSSr = 3;
// public static final int PASSBAND_SDSSi = 4;
// public static final int PASSBAND_SDSSz = 5;
// public static final int PASSBAND_SDSSY = 6;