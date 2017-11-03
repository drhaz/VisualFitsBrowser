package org.cowjumping.VisualFitsBrowser.util.FitsComments;

import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

public interface FitsCommentInterface {

	public void readComment(FitsFileEntry entry);

	public void writeComment(FitsFileEntry e);

}
