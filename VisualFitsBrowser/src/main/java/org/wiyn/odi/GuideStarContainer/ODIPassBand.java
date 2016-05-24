package org.wiyn.odi.GuideStarContainer;

public class ODIPassBand {
  /** Human readable identifier of filter */
  public String Name;

  /** UNique ID of a filter */
  public int id;

  /** Photometric zeropoint of filter is AB magnitude system */
  public double ZeropointAB = 0;

  /**
   * Best matching reference catalog passband when retrieving guide stars for
   * this filter.
   * 
   */

  public IPPCatalogMagPassband bestReferenceBand;

  public static ODIPassBand INDEF = new ODIPassBand(0, "INDEF", 0,
      IPPCatalogMagPassband.IPP_SDSSr);

  public static ODIPassBand SDSSg = new ODIPassBand(1, "SDSSg", 26.7,
      IPPCatalogMagPassband.IPP_SDSSg);
  public static ODIPassBand SDSSr = new ODIPassBand(2, "SDSSr", 26.6,
      IPPCatalogMagPassband.IPP_SDSSr);
  public static ODIPassBand SDSSi = new ODIPassBand(3, "SDSSi", 25.4,
      IPPCatalogMagPassband.IPP_SDSSi);
  public static ODIPassBand SDSSz = new ODIPassBand(4, "SDSSz", 25.5,
      IPPCatalogMagPassband.IPP_SDSSz);
  public static ODIPassBand NB815 = new ODIPassBand(5, "NB815", 0,
      IPPCatalogMagPassband.IPP_SDSSi);
  public static ODIPassBand NB910 = new ODIPassBand(6, "NB910", 0,
      IPPCatalogMagPassband.IPP_SDSSz);

  public static ODIPassBand SDSSu = new ODIPassBand(7, "SDSSu", 26.7,
      IPPCatalogMagPassband.IPP_SDSSg);

  public static ODIPassBand INSTRMAG = new ODIPassBand(8, "InstMag", 0,
      IPPCatalogMagPassband.INDEF);

  private ODIPassBand(int id, String name, double zp,
      IPPCatalogMagPassband bestReferenceBand) {

    this.Name = name;
    this.id = id;
    this.ZeropointAB = zp;
    this.bestReferenceBand = bestReferenceBand;
  }

  public String toString() {
    return Name;

  }

}
