package org.cowjumping.FitsUtils;

import java.io.File;
import org.apache.log4j.Logger;

public enum OBSTYPE {
  INDEF, BIAS, DARK, DOMEFLAT, OBJECT, FOCUS, PREIMAGE;
  final static Logger myLogger = Logger.getLogger(OBSTYPE.class);

  public static OBSTYPE getObstypeForFile(File f) {
    OBSTYPE ot = OBSTYPE.INDEF;
    String o = "_";
    o = f.getName().substring(0, 1);

    if (o.equals("b"))
      ot = OBSTYPE.BIAS;

    if (o.equals("d"))
      ot = OBSTYPE.DARK;

    if (o.equals("o"))
      ot = OBSTYPE.OBJECT;

    if (o.equals("c"))
      ot = OBSTYPE.OBJECT;

    if (o.equals("l"))
      ot = OBSTYPE.OBJECT;

    if (f.getName().startsWith("f")) {

      if (f.getName().startsWith("focus")) {

        ot = OBSTYPE.FOCUS;
      } else {

        ot = OBSTYPE.DOMEFLAT;
      }
    }

    if (o.equals("p")) {

      if (f.getName().startsWith("pre")) {

        ot = OBSTYPE.PREIMAGE;

      }
    }

    if (o.equals("t"))
      ot = OBSTYPE.DOMEFLAT;

    myLogger.debug("Obstype for file " + f.getName() + " is: " + ot
        + "  Trigger first letter is " + o);
    return ot;
  }

}
