package org.wiyn.guiUtils;

import javax.swing.table.AbstractTableModel;

abstract public class MultiTableDataModel extends AbstractTableModel {

    /**
     * Return over how many rows a record in this table is to be spread
     * 
     * @return number of records
     */
    abstract public int getRowsPerRecord ();

    /** Return in which row the record is to be drawn */

    abstract public int getRecordRow (int column);

    abstract public int getRecordColumn (int column);

}