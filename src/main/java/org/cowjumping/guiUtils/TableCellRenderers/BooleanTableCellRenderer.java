package org.cowjumping.guiUtils.TableCellRenderers;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.cowjumping.guiUtils.GUIConsts;

public class BooleanTableCellRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = 1222547296903386205L;
	final ImageIcon YESIcon;
	final ImageIcon NOIcon;

	public BooleanTableCellRenderer(int IconSize) {
		super();

		YESIcon = GUIConsts.getIcon("/resources/icons/Success.png", IconSize);

		NOIcon = GUIConsts.getIcon("/resources/icons/Error.png", IconSize);

		setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
	}

	public BooleanTableCellRenderer() {
		this(24);

	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (value != null) setIcon(((Boolean) value) ? YESIcon : NOIcon);
		return this;
	}

}