package org.cowjumping.VisualFitsBrowser.util.FitsComments;

import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

public interface FitsCommentInterface {

	public boolean readComment(FitsFileEntry entry);

	public boolean writeComment(FitsFileEntry e);

}
