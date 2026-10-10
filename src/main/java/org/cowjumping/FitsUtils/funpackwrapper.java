package org.cowjumping.FitsUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cowjumping.guiUtils.Preferences;
import org.cowjumping.guiUtils.ProcessRunner;

import java.io.File;
import java.util.concurrent.Callable;


public class funpackwrapper {

    private static Logger log = LogManager.getLogger(funpackwrapper.class);

    private static String execLocation = null;
    private static String tempDirectory = null;

    synchronized  public static funpackwrapper getInstance () {

        if (theWrapper == null)
            theWrapper = new funpackwrapper();
        return theWrapper;
    }


    private static funpackwrapper theWrapper = null;
    private  funpackwrapper() {

        execLocation = Preferences.thePreferences.getProperty("cowumping.funpack.exec",
                "/usr/bin/funpack");

        tempDirectory = Preferences.thePreferences.getProperty("tempdir", FileUtils.getTempDirectoryPath());

    }



    /**
     *
     * Blocking function to funpack a fits file.
     *
     * Input: absolute path to .fits.fz file.
     * Will extract file into a random file name into the system temp directory.
     * Returns File pointing to randomly generated file.
     *
     * @param input
     * @return
     */

     public File funpackfile (String input) {

        if (execLocation == null)
            return null;

        File in = new File (input);
        if (! in.exists()) {
            log.error ("Error while funpacking: input file [" + input + "] does not exists");
            return null;
        }

        String name = FilenameUtils.getBaseName(input);
        if (! name.endsWith(".fits"))
            name = name + ".fits";
        File outfile = new File (new File (tempDirectory), name);
        int exit = ProcessRunner.run(ProcessRunner.command(execLocation, "-O", outfile.getAbsolutePath(), input),
                null, log);
        if (exit != 0)
            log.warn("funpack exited with code " + exit + " for " + input);
        if (outfile.exists()) {
            outfile.deleteOnExit();
            return outfile;
        }

            return null;
    }



    Callable<File> getfunpackerCallable (String file) {

         return new Callable<File> () {

             @Override
             public File call() throws Exception {
                 return funpackfile(file);
             }
         };

    }

}
