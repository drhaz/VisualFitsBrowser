package org.cowjumping.guiUtils.TableCellRenderers;

import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("serial")
public class NumberFormatterCellRenderer extends DefaultTableCellRenderer {

  private final String formatText;
  private final String smallText;
  private final Logger myLogger = LogManager.getLogger(NumberFormatterCellRenderer.class);

  public NumberFormatterCellRenderer(String decimalFormat) {
    this (decimalFormat, null);
    
  }
  public NumberFormatterCellRenderer(String decimalFormat, String smallFormat) {
    super();
    formatText = decimalFormat;
    smallText = smallFormat;
    setHorizontalAlignment(JLabel.RIGHT);
  }
  

  public void setValue(Object value) {
    String text = null;
    if (value != null) {
      try {
        
        text = String.format(formatText, value);
        if (smallText != null && (Float) value < 1)
          text = String.format(smallText, value);
      } catch (Exception e) {
        if (myLogger.isDebugEnabled())
          myLogger.error("Error while translating exposure time: " + value, e);
      }
    }
    setText((text == null) ? "n/a" : text);
  }
}
