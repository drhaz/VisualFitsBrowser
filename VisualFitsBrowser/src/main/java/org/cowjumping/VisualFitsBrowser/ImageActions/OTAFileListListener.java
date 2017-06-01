package org.cowjumping.VisualFitsBrowser.ImageActions;

import java.util.Vector;

import org.cowjumping.VisualFitsBrowser.util.ODIFitsFileEntry;

public interface OTAFileListListener {

	public void pushFileSelection(Vector<ODIFitsFileEntry> fileList);

}