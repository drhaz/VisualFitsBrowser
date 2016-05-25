package org.wiyn.odi.VisualFitsBrowser.ImageActions;

import java.util.Vector;

import org.wiyn.odi.VisualFitsBrowser.util.ODIFitsFileEntry;

public interface OTAFileListListener {

	public void pushFileSelection(Vector<ODIFitsFileEntry> fileList);

}