package org.cowjumping.guiUtils.TableCellRenderers;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.cowjumping.guiUtils.GUIConsts;

public class TemperatureCellRenderer extends DefaultTableCellRenderer {

  private float limitUpWarn;
  private float limitUpError;

  public TemperatureCellRenderer(float limitUpWarn, float limitUpError) {
    this.limitUpWarn = limitUpWarn;
    this.limitUpError = limitUpError;
  }

  public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {
    Component comp = super.getTableCellRendererComponent(table, value,
        isSelected, hasFocus, row, column);

    String text = ((JLabel) comp).getText();
    Color c = GUIConsts.GoodStatusBackgroundColor;
    Color fg = Color.black;
      
    float t = (Float) value;
    if (t > limitUpWarn) {
      c = GUIConsts.WarnStatusBackgroundColor;
      text = "! " + text;
    }
    if (t > limitUpError) {
      c = GUIConsts.ErrorStatusBackgroundColor;
     // fg = Color.white;
      text = "!! " + text;
    }
    
    if (t == 9999) {
    	c = Color.white;
    	fg = Color.LIGHT_GRAY;
    	text = "n/a";
    }
    
    ((JLabel) comp).setText(text);
    ((JLabel) comp).setHorizontalAlignment( JLabel.RIGHT );
    comp.setBackground(c);
    comp.setForeground(fg);
    return comp;
  }

}
