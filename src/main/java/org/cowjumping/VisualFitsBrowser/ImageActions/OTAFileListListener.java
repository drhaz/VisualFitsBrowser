package org.cowjumping.VisualFitsBrowser.ImageActions;

import java.util.Vector;

import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

/** Receives the files the user selected in the file browser table. */
public interface OTAFileListListener {

	public void pushFileSelection(Vector<FitsFileEntry> fileList);

}