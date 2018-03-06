package org.cowjumping.VisualFitsBrowser.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.cowjumping.guiUtils.SoundSignal;

/**
 * Class that handles watching a directory for any new ODI images.
 * <p>
 * The class polls a directory and investiages all directories in there ( == ODI
 * images) that are newer than the latest image addition. Since it can take a
 * while between creating an image directory and an actual image to be written
 * (e.g., exposure time plus readout time), new detected fields are added to a
 * watch list, and once an image is finished, a notificatoon is sent.
 */

public class DirectoryListener implements Runnable {

    final static private Logger myLogger = Logger
            .getLogger(DirectoryListener.class);

    /**
     * The directory to Watch
     */
    File myDirectory;

    /**
     * The last time we have checked the directory.
     * <p>
     * When instantiating this listener, it is assumed that the directory has been
     * parsed already, i.e., we do not include already existing files.
     */
    long timeOfLastDirectoryRead = 0; //new Date().getTime();

    /**
     * Listener to notify when a new file has arrived
     */

    private DirectoryChangeReceiver rec;

    /**
     * Internal flag if listener should die.
     */
    private boolean abort = false;

    /**
     * Flag whether to beep when a new file has arrived.
     */
    public boolean beepOnNew = true;


    /**
     * The kind of files we are looking for
     */
    private FilenameFilter mFileNameFilter;

    /**
     * The image directory that was the most recent one
     */
    // private File lastNewFile = null;

    private Queue<File> newFileQueue = null;

    private Semaphore abortWait;

    public DirectoryListener(File dir, DirectoryChangeReceiver rec) {
        myDirectory = dir;
        this.rec = rec;
        newFileQueue = new ConcurrentLinkedQueue<File>();
        abortWait = new Semaphore(1);
        try {
            abortWait.acquire();
        } catch (InterruptedException e) {
            myLogger.error("Error while acquiring initial semaphore.");
        }
    }

    public long getNFileWatched() {
        long retVal = 0;
        if (newFileQueue != null)
            retVal = newFileQueue.size();
        return retVal;
    }

    public void run() {
        myLogger.info("Starting new Directory Listener Thread for: "
                + myDirectory.getAbsolutePath());
        while (!abort) {



            long lastModified = myDirectory.lastModified();

            // Step 1: Check if directory has new files to look for in it
            if (lastModified > timeOfLastDirectoryRead) {
                Vector<File> newFiles = getNewFilesSinceChange();

                // If new directories showed up, check if they are complete, or if we
                // have to monitor some files.

                if (!abort) {

                    for (File f : newFiles) {

                        if (myLogger.isDebugEnabled())
                            myLogger.debug("Adding new file candidate to queue: "
                                    + f.getAbsolutePath());
                        newFileQueue.add(f);

                    }

                }

            }


            // Step 2: profits. Parse the new file list and check if is ready to be displayed. This could be included
            // in the loop above, but this process allows for additional checks, such as having an external trigger
            // when a file is ready for view.

            for (File f : newFileQueue) {


                long length = f.length();

                if (!f.isDirectory() && (length < 2880)) {
                    // Sanity check: fits file to be valid has to ahve a t least one full fits block length.
                    continue;
                }

                rec.addSingleNewItem(f);
                if (beepOnNew)
                    SoundSignal.notifyNewImage();


                // Important: only update last access timestamp idf there were genuine new files there. Otherwise,
                // buffered file that are in transit, and then renamed, cn areally mess up things.
                timeOfLastDirectoryRead = lastModified;


            }
            newFileQueue.clear();

            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        myLogger.info("Exiting Listener Thread for Directory "
                + myDirectory.getAbsolutePath());
        newFileQueue.clear();
        abortWait.release();
    }

    public void waitToabort() {
        this.abort = true;
        myLogger.info("Waiting for Directorylistener to quit.");
        try {
            this.abortWait.acquire();
        } catch (InterruptedException e) {
            myLogger.error("Error while waiting for listener thread to finish.", e);
        }
    }

    Vector<File> getNewFilesSinceChange() {

        Vector<File> newFiles = new Vector<File>();

        File[] fArray = myDirectory.listFiles(FitsFileEntry.thefileFilter);


        for (File f : fArray) {

            if (f.isFile() && f.lastModified() > this.timeOfLastDirectoryRead) {

                boolean alreadyInQueue = false;
                boolean alreadyInList = false;
                for (File watchedFiles : newFileQueue) {
                    if (watchedFiles.equals(f)) {
                        alreadyInQueue = true;
                        myLogger.debug("File " + f.getAbsolutePath() + " is already under observation");
                    }
                }

                for (File listedFiles : this.rec.getListedFiles()) {
                    if (listedFiles.equals(f)) {
                        alreadyInList = true;
                        //	myLogger.debug("File " + f.getAbsolutePath() + " is already in browser");
                    }
                }

                if (!alreadyInQueue && !alreadyInList)
                    newFiles.add(f);
                else {
//                    myLogger.debug("Rejecting file from new candidate list: Queue: "
//                            + alreadyInQueue + " List:" + alreadyInList + " File: "
//                            + f.getAbsolutePath());
                }
            }
        }

        return newFiles;

    }
}

class FileQueueEntry {

    protected FileQueueEntry(File f, Date lastModified) {
        super();
        this.f = f;
        this.lastModified = lastModified;
    }

    public File f;
    public Date lastModified;

}
