package org.cowjumping.VisualFitsBrowser.ImageActions;

import java.util.Vector;

import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

public interface OTAFileListListener {

	public void pushFileSelection(Vector<FitsFileEntry> fileList);

}