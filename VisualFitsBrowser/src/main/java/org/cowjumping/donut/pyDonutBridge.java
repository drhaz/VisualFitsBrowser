package org.cowjumping.donut;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.cowjumping.guiUtils.Preferences;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * Created by harbeck on 6/30/16.
 * <p>
 * This is a bridge to call an external tool, a python donut processor, to determine the first 11 zernikes of a donut.
 */


public class pyDonutBridge implements Callable<pyDonutBridge> {


    private static final Logger log = Logger.getLogger(pyDonutBridge.class);
    private final boolean intrafocus;
    private final int w;
    private final int x;
    private final int y;

    String resultsString = null;
    Image resultImage = null;
    private String donutExecutable = "/home/dharbeck/Software/donut/script/donut_odi";
    private String donutConfig = "/home/dharbeck/Software/donut/script/lco-1m.json";
    private String defaultpath = "/tmp";
    private String defaultOutput = "donutfit_" + new Date().getTime();
    private File input;


    public pyDonutBridge(File input, int x, int y, int width, boolean intrafocus, Preferences p) {
        this(input, intrafocus, x, y, width);
        this.donutExecutable = p.getProperty("donutbridge.executable", this.donutExecutable);
        this.defaultpath =     p.getProperty("donutbridge.tmpdir", this.defaultpath);
        this.donutConfig =     p.getProperty("donutbridge.donutconfig", this.donutConfig);

    }

    public pyDonutBridge(File input, boolean intrafocus, int x, int y, int width) {

        this.input = input;
        this.intrafocus = intrafocus;
        this.x = x;
        this.y = y;
        this.w = width;
    }

    private boolean callout(String g) {
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = null;

            log.debug("Executing donut: " + g);

            proc = rt.exec(g.toString(), null, new File(donutExecutable).getParentFile());
            if (proc == null)
                return false;

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
        return true;
    }



    private String generateExecString(File donut, boolean intra) {


        StringBuilder sb = new StringBuilder();

        sb.append(this.donutExecutable);
        sb.append(" -i " + donut.getAbsolutePath() + " ");
        sb.append(" -p " + donutConfig + " ");
        sb.append(" -o " + defaultpath + "/" + this.defaultOutput + " ");
        sb.append(intra ? "--intra" : "--extra" + " ");
        sb.append(" -x " + this.x + " ");
        sb.append(" -y " + this.y + " ");
        sb.append(" -w " + this.w + " ");
        return sb.toString();

    }

    @Override
    public pyDonutBridge call() throws Exception {

        String command = generateExecString(this.input, this.intrafocus);

        callout(command);

        File resultstxt = new File(defaultpath + "/" + defaultOutput + ".txt");
        File resultsimg = new File(defaultpath + "/" + defaultOutput + ".png");
        if (resultstxt.exists() && resultsimg.exists()) {

            this.resultsString = IOUtils.toString(new FileInputStream(resultstxt));
            this.resultImage = ImageIO.read(resultsimg);

            return (this);

        } else {

            log.error("Cannot open result: " + resultstxt.getAbsolutePath());
        }


        return null;
    }


    public static void main(String args[]) throws Exception {

        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.DEBUG);
        pyDonutBridge d = new pyDonutBridge(new File("/mnt/data/daydirs/ef12/20171024/raw/bpl1m002-ef12-20171024-0003-x00.fits.fz"),
                false,650,101,200);

        d = d.call();

        System.out.println(d.resultsString);

    }

}
