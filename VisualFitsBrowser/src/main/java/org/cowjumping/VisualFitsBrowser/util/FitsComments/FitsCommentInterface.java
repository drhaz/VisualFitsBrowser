package org.cowjumping.VisualFitsBrowser.util.FitsComments;

import org.cowjumping.VisualFitsBrowser.util.ODIFitsFileEntry;

public interface FitsCommentInterface {

	public void readComment(ODIFitsFileEntry entry);

	public void writeComment(ODIFitsFileEntry e);

}
