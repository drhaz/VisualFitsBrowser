package org.cowjumping.VisualFitsBrowser.util.FitsComments;

import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

/**
 * Storage back end for user comments attached to FITS images.
 * <p>
 * Comments are identified by {@link FitsFileEntry#FName} (the file name without
 * directory). The implementation in use is {@link FITSTextCommentSQLITEImp}, which
 * keeps all comments in ~/.VisualFileBrowser.usercomments.
 */
public interface FitsCommentInterface {

	/**
	 * Look up the comment for an image and store it in {@link FitsFileEntry#UserComment}.
	 *
	 * @return true if the lookup succeeded (also when no comment exists).
	 */
	public boolean readComment(FitsFileEntry entry);

	/**
	 * Persist {@link FitsFileEntry#UserComment} for the given image, replacing any previous comment.
	 *
	 * @return true if the comment was stored (or queued for storage).
	 */
	public boolean writeComment(FitsFileEntry e);

	/** Flush pending writes and release the storage. Called when the application exits. */
	void close();
}
