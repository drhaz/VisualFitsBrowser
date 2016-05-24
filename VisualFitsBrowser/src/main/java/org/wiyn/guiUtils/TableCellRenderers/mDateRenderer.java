package org.wiyn.guiUtils.TableCellRenderers;

import java.awt.Color;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.wiyn.guiUtils.GUIConsts;

@SuppressWarnings("serial")
public class mDateRenderer extends DefaultTableCellRenderer {

	private final static SimpleDateFormat mDateFormat = new SimpleDateFormat(
			"HH:mm:ss");
	private final static SimpleDateFormat mShortDateFormat = new SimpleDateFormat(
			"HH:mm");
	private long diffTime;
	private final SimpleDateFormat DateFormat;

	public mDateRenderer(long diffTime, boolean shortForm) {
		super();
		this.diffTime = diffTime;
		DateFormat = shortForm ? mShortDateFormat : mDateFormat;
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		Component comp = super.getTableCellRendererComponent(table, value,
				isSelected, hasFocus, row, column);

		((JLabel) comp).setText((value == null) ? "n/a" : DateFormat
				.format(value));
		((JLabel) comp).setHorizontalAlignment(JLabel.RIGHT);

		String text = ((JLabel) comp).getText();

		if (diffTime != 0) {
			Color c = GUIConsts.GoodStatusBackgroundColor;
			Color fg = Color.black;
			if (value == null) {
				c = GUIConsts.WarnStatusBackgroundColor;

			} else if ( ((Date) value).getTime () == 1) {
				c=Color.white;
				fg = Color.lightGray;
				text = "n/a";
			}			
			else if (new java.util.Date().getTime()
					- ((Date) value).getTime() > diffTime) {
				c = GUIConsts.ErrorStatusBackgroundColor;
				text = "! " + text;
			}
			((JLabel) comp).setText(text);
			comp.setBackground(c);
			comp.setForeground(fg);
		}

		return comp;
	}

	// public void setValue(Object value) {
	//
	// setText((value == null) ? "n/a" : mDateFormat.format(value));
	// }

}
