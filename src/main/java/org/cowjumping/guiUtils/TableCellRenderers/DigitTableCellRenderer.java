package org.cowjumping.guiUtils.TableCellRenderers;

import java.awt.Component;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class DigitTableCellRenderer extends DefaultTableCellRenderer {

  private NumberFormat formatter = null;

  public DigitTableCellRenderer(int n) {
    formatter = DecimalFormat.getInstance();
    formatter.setMaximumFractionDigits(n);
    formatter.setMinimumFractionDigits(n);
    setHorizontalTextPosition(DefaultTableCellRenderer.CENTER);
  }

  public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {
    // System.out.println("value is " + value);
    if (value == null) {
      value = new Double(0.0);
    }
    value = formatter.format(value);
    JLabel renderer = (JLabel) super.getTableCellRendererComponent(table,
        value, isSelected, hasFocus, row, column);

    renderer.setHorizontalAlignment(JLabel.RIGHT);
    return renderer;

  }
}