package org.cowjumping.FitsUtils;

import jsky.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;
import org.cowjumping.guiUtils.Preferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;


public class funpackwrapper {

    private static Logger log = Logger.getLogger(funpackwrapper.class);

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
                "/usr/local/bin/funpack");

        tempDirectory = Preferences.thePreferences.getProperty("tempdir", FileUtils.getTempDirectoryPath());

    }



    private void callout(String g) {
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = null;

            log.debug("Executing funpack: " + g);

            proc = rt.exec(g, null);
            if (proc == null)
                return;

            BufferedReader err = new BufferedReader(new InputStreamReader(
                    proc.getErrorStream()));
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    proc.getInputStream()));

            String sline = null;
            String eline = null;
            while ((sline = br.readLine()) != null
                    || (eline = err.readLine()) != null) {
                if (sline != null) log.debug(sline);
                if (eline != null) log.error(sline);
            }

            proc.waitFor();

        } catch (Exception e) {

            log.error(e);
        }
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

        String name = String.format("%s.%s", RandomStringUtils.randomAlphanumeric(8), "fits");
        File outfile = new File (new File (tempDirectory), name);
        String commandline = execLocation + " -O " + outfile.getAbsolutePath() + " " + input;
        callout(commandline);
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
