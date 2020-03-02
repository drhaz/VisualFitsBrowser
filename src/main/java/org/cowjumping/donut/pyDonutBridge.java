package org.cowjumping.donut;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cowjumping.guiUtils.Preferences;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by harbeck on 6/30/16.
 * <p>
 * This is a bridge to call an external tool, a python donut processor, to determine the first 11 zernikes of a donut.
 */


public class pyDonutBridge implements Callable<pyDonutBridge> {


    private static final Logger log = LogManager.getLogger();
    private final boolean intrafocus;
    private final int w;
    private final int x;
    private final int y;

    public String resultsString = null;
    public Image resultImage = null;
    private String donutExecutable = "/home/dharbeck/Software/donut/script/donut_odi";
    private String donutConfig = "/home/dharbeck/Software/donut/script/lco-1m-ef.json";
    private String defaultpath = "/tmp";
    private String defaultOutput = "donutfit_" + new Date().getTime();
    private File input;

    public void setResultListener(DonutBridgeResultListener resultListener) {
        this.resultListener = resultListener;
    }

    private DonutBridgeResultListener resultListener = null;

    private static final ExecutorService myThreadPool = Executors.newFixedThreadPool(2);

    public static void submitTask (pyDonutBridge newTask) {
        myThreadPool.submit(newTask);
    }


    public pyDonutBridge(File input, int x, int y, int width, boolean intrafocus, Preferences p) {
        this(input, intrafocus, x, y, width);
        this.donutExecutable = p.getProperty("donutbridge.executable", this.donutExecutable);
        this.defaultpath =     p.getProperty("donutbridge.tmpdir", this.defaultpath);
        this.donutConfig =     p.getProperty("donutbridge.donutconfig", this.donutConfig);

    }





    public pyDonutBridge(File input, boolean intra, int x, int y, int width) {

        this.input = input;
        this.intrafocus = intra;
        this.x = x;
        this.y = y;
        this.w = width;
    }

    private void callout(String g) {
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = null;

            log.debug("Executing donut: " + g + " in directory "+ new File(donutExecutable).getParentFile());

            proc = rt.exec(g, null, new File(donutExecutable).getParentFile());
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



    private String generateExecString(File donut, boolean intra) {



        return this.donutExecutable +
                " -i " + donut.getAbsolutePath() + " " +
                " -p " + donutConfig + " " +
                " -o " + defaultpath + "/" + this.defaultOutput + " " +
                (intra ? "--intra" : "--extra" + " ") +
                " -x " + this.x + " " +
                " -y " + this.y + " " +
                " -w " + this.w + " ";

    }

    @Override
    public pyDonutBridge call() throws Exception {

        String command = generateExecString(this.input, this.intrafocus);

        callout(command);

        File resultstxt = new File(defaultpath + "/" + defaultOutput + ".txt");
        File resultsimg = new File(defaultpath + "/" + defaultOutput + ".png");
        if (resultstxt.exists() && resultsimg.exists()) {

            this.resultsString = IOUtils.toString(new FileInputStream(resultstxt), (Charset) null);
            this.resultImage = ImageIO.read(resultsimg);
            log.debug("\n" + this.resultsString);
            if (this.resultListener != null)
                this.resultListener.notifyResult(this);
            return (this);

        } else {

            log.error("Cannot open result: " + resultstxt.getAbsolutePath());
        }


        return null;
    }


    public static void main(String args[]) throws Exception {


        pyDonutBridge d = new pyDonutBridge(new File("/mnt/data/daydirs/ef12/20171024/raw/bpl1m002-ef12-20171024-0003-x00.fits.fz"),
                false,650,101,250);

        d = d.call();

        System.out.println(d.resultsString);

    }

}
