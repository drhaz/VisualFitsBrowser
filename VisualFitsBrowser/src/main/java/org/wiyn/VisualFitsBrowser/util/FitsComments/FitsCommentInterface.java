package org.wiyn.VisualFitsBrowser.util.FitsComments;

import org.wiyn.VisualFitsBrowser.util.ODIFitsFileEntry;

public interface FitsCommentInterface {

	public void readComment(ODIFitsFileEntry entry);

	public void writeComment(ODIFitsFileEntry e);

}
