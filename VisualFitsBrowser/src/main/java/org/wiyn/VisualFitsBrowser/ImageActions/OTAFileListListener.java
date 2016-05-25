package org.wiyn.VisualFitsBrowser.ImageActions;

import java.util.Vector;

import org.wiyn.VisualFitsBrowser.util.ODIFitsFileEntry;

public interface OTAFileListListener {

	public void pushFileSelection(Vector<ODIFitsFileEntry> fileList);

}