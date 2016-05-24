package org.wiyn.odi.VisualFitsBrowser.util.FitsComments;

import org.wiyn.odi.VisualFitsBrowser.util.ODIFitsFileEntry;

public interface FitsCommentInterface {

	public void readComment(ODIFitsFileEntry e);

	public void writeComment(ODIFitsFileEntry e);

}
