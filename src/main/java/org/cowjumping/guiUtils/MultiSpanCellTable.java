package org.cowjumping.guiUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.Enumeration;


public class MultiSpanCellTable extends JTable {

    private static final Logger myLogger = LogManager.getLogger(MultiSpanCellTable.class);

    public MultiSpanCellTable(TableModel model) {
	super (model);
	setUI (new MultiSpanCellTableUI ());
	// if (!(model instanceof MultiTableDataModel))
	// throw new IllegalArgumentException ("Must use ultiTableDataModel!");

    }

    // public Rectangle getCellRect (int row, int column, boolean
    // includeSpacing) {
    // Rectangle sRect = super.getCellRect (row, column, includeSpacing);
    //
    // MultiTableDataModel model = (MultiTableDataModel) this.getModel ();
    //
    // int RecordRowCount = model.getRowsPerRecord ();
    // int myRow = model.getRecordRow (column);
    //
    // int height = super.getRowHeight ();
    //
    // if (myRow == 1) {
    // sRect.y += height;
    // sRect.x = 0;
    // }
    // return sRect;
    // }

    public int getRowHeight () {

	return super.getRowHeight ()
		* ((MultiTableDataModel) this.dataModel).getRowsPerRecord ();
    }

    public int rowAtPoint (Point point) {
	return super.rowAtPoint (point);
    }

    public int columnAtPoint (Point point) {
	
	return super.columnAtPoint (point);
    }

}

class MultiSpanCellTableUI extends BasicTableUI {

    public void paint (Graphics g, JComponent c) {
	Rectangle oldClipBounds = g.getClipBounds ();
	Rectangle clipBounds = new Rectangle (oldClipBounds);
	int tableWidth = table.getColumnModel ().getTotalColumnWidth ();
	clipBounds.width = Math.min (clipBounds.width, tableWidth);
	g.setClip (clipBounds);

	// Paint the grid
	paintGrid (g);

	// Paint the rows
	int firstIndex = table.rowAtPoint (new Point (0, clipBounds.y));
	int lastIndex = lastVisibleRow (clipBounds);

	Rectangle rowRect = new Rectangle (0, 0, tableWidth,
		table.getRowHeight (firstIndex) + table.getRowMargin ());
	rowRect.y = table.getCellRect (firstIndex, 0, false).y;

	for (int index = firstIndex; index <= lastIndex; index++) {
	    // Paint any rows that need to be painted
	    if (rowRect.intersects (clipBounds)) {
		paintRow (g, index);
	    }
	    rowRect.y += rowRect.height;
	    rowRect.height = table.getRowHeight (index + 1);
	}
	g.setClip (oldClipBounds);
    }

    private void paintGrid (Graphics g) {
	g.setColor (table.getGridColor ());

	if (table.getShowHorizontalLines ()) {
	    paintHorizontalLines (g);
	}
	if (table.getShowVerticalLines ()) {
	    paintVerticalLines (g);
	}
    }

    private void paintHorizontalLines (Graphics g) {
	Rectangle r = g.getClipBounds ();
	Rectangle rect = r;
	// int delta = table.getRowHeight() + table.getRowMargin();
	int rowMargin = table.getRowMargin ();
	int firstIndex = table.rowAtPoint (new Point (0, r.y));
	int lastIndex = lastVisibleRow (r);
	// int y = delta*firstIndex+(delta-1);
	int y = table.getCellRect (firstIndex + 1, 0, false).y - 1;

	for (int index = firstIndex; index <= lastIndex; index++) {
	    if ((y >= rect.y) && (y <= (rect.y + rect.height))) {
		g.drawLine (rect.x, y, rect.x + rect.width - 1, y);
	    }
	    y += table.getRowHeight (index + 1) + rowMargin;
	}
    }

    /*
     * This method paints vertical lines regardless of whether the table is set
     * to paint one automatically.
     */
    private void paintVerticalLines (Graphics g) {
	Rectangle rect = g.getClipBounds ();
	int x = 0;
	int count = table.getColumnCount ();
	int horizontalSpacing = table.getIntercellSpacing ().width;
	for (int index = 0; index <= count; index++) {
	    if ((x > 0)
		    && (((x - 1) >= rect.x) && ((x - 1) <= (rect.x + rect.width)))) {
		g.drawLine (x - 1, rect.y, x - 1, rect.y + rect.height - 1);
	    }

	    if (index < count)
		x += ((TableColumn) table.getColumnModel ().getColumn (index))
			.getWidth () + horizontalSpacing;
	}
    }

    private void paintRow (Graphics g, int row) {
	Rectangle rect = g.getClipBounds ();
	int column = 0;
	boolean drawn = false;
	int draggedColumnIndex = -1;
	Rectangle draggedCellRect = null;
	Dimension spacing = table.getIntercellSpacing ();
	JTableHeader header = table.getTableHeader ();

	// Set up the cellRect
	Rectangle cellRect = new Rectangle ();
	cellRect.height = table.getRowHeight (row) + spacing.height;
	cellRect.y = table.getCellRect (row, 0, false).y; // row *
							  // cellRect.height;

	Enumeration enumeration = table.getColumnModel ().getColumns ();
	MultiTableDataModel model = (MultiTableDataModel) table.getModel ();

	// Paint the non-dragged table cells first
	while (enumeration.hasMoreElements ()) {
	    TableColumn aColumn = (TableColumn) enumeration.nextElement ();

	    Rectangle modRect = new Rectangle (cellRect);

	    modRect.y -= table.getRowHeight () / 4;
	    if (model.getRecordRow (aColumn.getModelIndex ()) > 0) {
		modRect.y += table.getRowHeight () / 2;
		modRect.x = 10;

	    }

	    modRect.width = aColumn.getWidth () + spacing.width;
	    if (modRect.intersects (rect)) {
		drawn = true;
		if ((header == null) || (aColumn != header.getDraggedColumn ())) {
		    paintCell (g, modRect, row, column);
		} else {
		    // Paint a gray well in place of the moving column
		    // This would be unnecessary if we drew the grid more
		    // cleverly
		    g.setColor (table.getParent ().getBackground ());
		    g.fillRect (modRect.x, modRect.y, modRect.width,
			    modRect.height);
		    draggedCellRect = new Rectangle (modRect);
		    draggedColumnIndex = column;
		}
	    } else {
		if (drawn)
		    // Don't need to iterate through the rest
		    break;
	    }

	    cellRect.x += modRect.width;
	    column++;
	}

	// paint the dragged cell if we are dragging
	if (draggedColumnIndex != -1 && draggedCellRect != null) {
	    draggedCellRect.x += header.getDraggedDistance ();

	    // Fill the background
	    g.setColor (table.getBackground ());
	    g.fillRect (draggedCellRect.x, draggedCellRect.y,
		    draggedCellRect.width, draggedCellRect.height);

	    // paint grid if necessary.
	    g.setColor (table.getGridColor ());
	    int x1 = draggedCellRect.x;
	    int y1 = draggedCellRect.y;
	    int x2 = x1 + draggedCellRect.width - 1;
	    int y2 = y1 + draggedCellRect.height - 1;
	    if (table.getShowVerticalLines ()) {
		// Left
		// g.drawLine(x1-1, y1, x1-1, y2);
		// Right
		g.drawLine (x2, y1, x2, y2);
	    }
	    // Bottom
	    if (table.getShowHorizontalLines ()) {
		g.drawLine (x1, y2, x2, y2);
	    }

	    // Render the cell value
	    paintCell (g, draggedCellRect, row, draggedColumnIndex);
	}
    }

    private void paintCell (Graphics g, Rectangle cellRect, int row, int column) {
	// The cellRect is inset by half the intercellSpacing before painted
	int spacingHeight = table.getRowMargin ();
	int spacingWidth = table.getColumnModel ().getColumnMargin ();

	// Round so that when the spacing is 1 the cell does not paint obscure
	// lines.

	cellRect.setBounds (cellRect.x + spacingWidth / 2, cellRect.y
		+ spacingHeight / 2, cellRect.width - spacingWidth,
		cellRect.height - spacingHeight);

	if (table.isEditing () && table.getEditingRow () == row
		&& table.getEditingColumn () == column) {
	    Component component = table.getEditorComponent ();
	    component.setBounds (cellRect);
	    component.validate ();
	} else {
	    TableCellRenderer renderer = table.getCellRenderer (row, column);
	    Component component = table.prepareRenderer (renderer, row, column);

	    if (component.getParent () == null) {
		rendererPane.add (component);
	    }
	    rendererPane.paintComponent (g, component, table, cellRect.x,
		    cellRect.y, cellRect.width, cellRect.height, true);
	}
	// Have to restore the cellRect back to it's orginial size
	cellRect.setBounds (cellRect.x - spacingWidth / 2, cellRect.y
		- spacingHeight / 2, cellRect.width + spacingWidth,
		cellRect.height + spacingHeight);

    }

    private int lastVisibleRow (Rectangle clip) {
	int lastIndex = table.rowAtPoint (new Point (0, clip.y + clip.height
		- 1));
	// If the table does not have enough rows to fill the view we'll get -1.
	// Replace this with the index of the last row.
	if (lastIndex == -1) {
	    lastIndex = table.getRowCount () - 1;
	}
	return lastIndex;
    }
}
