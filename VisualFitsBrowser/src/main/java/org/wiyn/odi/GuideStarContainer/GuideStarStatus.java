package org.wiyn.odi.GuideStarContainer;

public enum GuideStarStatus {
  INDEF ("Indef", true), //
  OK("OK", true), //
  FAINT_WARN("Faint warn", true), //
  FAINT_ERR("Faint rej", false), //
  EDGE_WARN("Edge warn", true), //
  EDGE_ERROR("Edge rej", false), //
  REJECTED ("Rejected", false), //
  SATURATION_ERR("Saturated", false);

  public final String Message;
  public final boolean Usable;

  GuideStarStatus(String Msg, boolean usable) {
    this.Message = Msg;
    this.Usable = usable;
  }

  public String toString() {
    return Message;
  }
}
