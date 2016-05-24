package org.wiyn.util.FitsComments;

import org.wiyn.util.ODIFitsFileEntry;

public interface FitsCommentInterface {

	public void readComment(ODIFitsFileEntry e);

	public void writeComment(ODIFitsFileEntry e);

}
