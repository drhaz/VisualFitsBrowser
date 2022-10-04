package org.cowjumping.guiUtils;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.CallableClient;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;
import org.astrogrid.samp.xmlrpc.StandardClientProfile;
import org.cowjumping.FitsUtils.funpackwrapper;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Tools to use ds9 via SAMP. This is mostly concerning interaction with ds9, but can easilly be expanded to
 * collaborate with other SAMP clients.
 *
 * @author harbeck
 */

public class SAMPUtilities {
    private final static Logger log = LogManager.getLogger(SAMPUtilities.class);
    private static HubConnector sampHubConnector = null;
    private static Hub theHub = null;

    /* ds9 specific declarations */
    private static String ds9binary = null;
    private final static String[] ds9binarycandidates = {"/usr/local/bin/ds9",
            "/usr/bin/ds9"};


    static ExecutorService ds9Pool = Executors.newSingleThreadExecutor();


    /**
     * Initiate the SAMP connection.
     *
     * @param name   Identification of our connection
     * @param Descr  Description of our connection
     * @param runHub if true, run a SAMP hub.
     */

    public static void initHubConnector(String name, String Descr, boolean runHub) {

        if (runHub) {
            try {
                theHub = Hub.runHub(HubServiceMode.MESSAGE_GUI);
            } catch (IOException e) {
                log.error("Error while starting SAMP hub. SAMP Services will not be available.\n    "
                        + e.getLocalizedMessage());
            }

        }

        ClientProfile profile = StandardClientProfile.getInstance();
        sampHubConnector = new HubConnector(profile);
        Metadata meta = new Metadata();
        meta.setName(name);
        meta.setDescriptionText(Descr);
        sampHubConnector.declareMetadata(meta);
        sampHubConnector.declareSubscriptions(sampHubConnector.computeSubscriptions());
        sampHubConnector.setAutoconnect(10);
    }

    /**
     * Do a controlled shutdown of the SAMP hub.
     * <p>
     * Call this procedure when your application quits.
     */

    public static void onExit() {

        if (theHub != null) {
            theHub.shutdown();
        }
    }

    /**
     * @return a HubConnector that can be used i your application.
     */

    public synchronized static HubConnector getHubConnector() {

        if (sampHubConnector == null) {

            initHubConnector("no name", "uninitialized ODI Utilitis SAMP hub.", false);

        }

        return sampHubConnector;
    }


    /**
     * ds9: activate a frame in ds9.
     * <p>
     * Caution: If entering a large number, ds9 will open as many new frames internaly, i.e., this can potentially
     * cause ds9 to consumer a lot of resources.
     *
     * @param frameNumber ds9 frame number
     */

    public static void selectFrameDS9(int frameNumber) {

        if (frameNumber > 100)
            log.warn("SAMP: Select image frame in ds9: " + frameNumber +
                    " frame number is large! This can cause resource issues in ds9");
        sendCommandDS9("frame " + frameNumber);

    }

    public static void loadImageDS9(String fname, int frameNumber) {
        log.debug("SAMP: Loading fits image to ds9: " + fname);
        selectFrameDS9((frameNumber));
        String escapedFitsname = fname.replace(" ", "\\ ");
        sendCommandDS9("file fits " + escapedFitsname);
    }

    public static void loadMEFSaveDS9(final String fname, int frame, boolean funpack) {


        Runnable r = new Runnable() {

            @Override
            public void run() {

                String myfname = fname;
                if (funpack) {
                    log.info("Funpacking file for you");
                    File f;
                    f = funpackwrapper.getInstance().funpackfile(myfname);
                    if ((f != null) && (f.exists())) {
                        myfname = f.getAbsolutePath();
                    }
                }

                boolean ismef = SAMPUtilities.isMEF(myfname);

                if (ismef)
                    SAMPUtilities.loadMosaicDS9(myfname, frame);
                else
                    SAMPUtilities.loadImageDS9(myfname, frame);
            }
        };

        ds9Pool.submit(r);

    }


    public static void lockScale() {
        sendCommandDS9("scale lock yes");
    }

    public static void lockFrame() {
        sendCommandDS9("lock frame image");
    }

    public static void lockColorbar() {
        sendCommandDS9("cmap lock yes");
    }

    public static void clearAllFrames() {
        sendCommandDS9("frame delete all");
    }

    public static void lockAll() {
        lockColorbar();
        lockScale();
        lockFrame();
    }

    private static void sendCommandDS9(String command) {
        try {

            Message m = new Message("ds9.set");
            m.addParam("cmd", command);

            getHubConnector().getConnection().notifyAll(m);

        } catch (Exception e) {
            log.error("Could not send command " + command + " to ds9 via SAMP", e);
        }
    }


    /**
     * Search a ds9 instance in SAMPhub and send it a callback command.
     * The calling program has to register a call back with the SAMPHub, and process events that are
     * identified by callbackIdentifier
     *
     * @param command
     * @param callbackIdentifier
     */
    private static void sendCallbackDS9(String command, String callbackIdentifier) {


        try {

            HubConnector h = getHubConnector();
            String id = (String) h.getConnection().getSubscribedClients("ds9.get").keySet().iterator().next();

            if ((h != null) && ((h.getConnection() != null) && (id != null))) {
                Message m = new Message("ds9.get");
                m.addParam("cmd", command);

                h.getConnection().call(id, callbackIdentifier, m);

            } else {
                JOptionPane.showMessageDialog(null,
                        "No SAMP hub is available. Please start a SAMP hub; the OTAListener will provide one.",
                        "SAMP Error", JOptionPane.ERROR_MESSAGE);

            }

        } catch (Exception e) {
            log.error("While requesting cursor selection:", e);
        }

    }

    public static void getDS9Cursor(String callbackIdentifier) {

        log.debug("SAMP: Requesting ds9 cursor selection");
        sendCallbackDS9("iexam $filename $x $y $regions", callbackIdentifier);

    }

    public static void getDS9ImageCutout (String callbackIdentifier, int centerx, int centery, int width) {
        log.debug (String.format("SAMP: requesting iamge cutout from ds9 at coordinate %d %d, width %d",centerx,centery,width));
        int llx = centerx - width/2;
        int lly = centery - width/2;
        sendCallbackDS9 (String.format("data image %d %d %d %d no", llx,lly, width, width), callbackIdentifier);

    }


    public static void getDS9imexam (String callbackIdentifier) {
        log.debug ("SAMP: requesting imexam");
        sendCallbackDS9 (String.format("iexam key coordinate image"), callbackIdentifier);

    }

    public static void launchds9(String pathToBinary) {

        if (pathToBinary == null) {
            pathToBinary = searchds9Binary();
        }

        if (pathToBinary != null)

            try {
                Runtime.getRuntime().exec(pathToBinary + " -samp connect");
            } catch (IOException e) {
                log.error("While launching ds9: ", e);
            }

        else

            log.warn("No ds9 binary identified to launch!");

    }

    public synchronized static String searchds9Binary() {

        if (ds9binary != null) {
            log.info("ds9 binary pre-stored at loation " + ds9binary);
            return ds9binary;
        }

        for (String candidate : ds9binarycandidates) {

            if (new File(candidate).exists()) {
                ds9binary = candidate;
                break;
            }
        }

        log.info("Returned ds9 binary location from search: " + ds9binary);
        return ds9binary;

    }

    public static boolean isClientAvailable(String idContain) {
        boolean retVal = false;
        try {
            if (getHubConnector().getConnection() != null) {
                String clients[] = getHubConnector().getConnection().getRegisteredClients();

                for (String client : clients) {

                    String clientName = getHubConnector().getConnection().getMetadata(client).getName();

                    if (clientName != null) {

                        if (clientName.contains(idContain))
                            retVal = true;
                    }
                }
            }
        } catch (SampException e) {

            log.error(e);
        }

        return retVal;

    }


    /**
     * utility function: fin dout if a fits file is a MEF file that could be
     * displayed as mosaicimage iraf in ds9
     *
     * @param fname
     * @return
     */
    public static boolean isMEF(String fname) {

        Fits f = null;
        boolean mef = true;
        int nImageExtensions = 0;
        try {
            f = new Fits(fname);
            BasicHDU[] HDUs = f.read();
            for (BasicHDU HDU : HDUs) {
                if ((HDU instanceof ImageHDU)
                        || (HDU instanceof CompressedImageHDU)) {

                    nImageExtensions++;
                    if (!(HDU.getHeader().containsKey("DETSEC")
                            // && HDU.getHeader().containsKey("DETSIZE")
                    )) {

                        log.info("Image extension does not contrain DETSEC or DETSZIZE");
                        mef = false;
                        break;
                    }
                }

            }

        } catch (Exception e) {
            log.error(e);

        } finally {
            try {
                if (f != null) {
                    f.close();
                }
            } catch (Exception e) {
                log.error(e);
            }

        }

        log.info("MEF assessment: mef " + mef + " nExts = " + nImageExtensions);
        return (mef && (nImageExtensions > 1));
    }


    /**
     * @param fname
     * @param fno
     */
    public static void loadMosaicDS9(String fname, int fno) {
        log.debug("SAMP: Loading Mosiac image to ds9: " + fname);

        if (!isClientAvailable("DS9")) {
            JOptionPane.showMessageDialog(null,
                    "DS9 is not connected. DS9 conencts to SAMP only on startup, so if SAMP restarted, you need to restart ds9 as well.",
                    "SAMP Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SAMPUtilities.selectFrameDS9(fno);
        sendCommandDS9("file  mosaicimage iraf " + fname);

    }


    @Deprecated
    public static void registercallBack(CallableClient client) {
        HubConnector h = getHubConnector();
        try {
            if (client != null && h != null)
                h.getConnection().setCallable(client);
        } catch (SampException e) {

            log.error("Error while registering SAMP callback", e);
        }
    }

    public static void main(String args[]) {

        System.out.println(isClientAvailable("DS9"));
        System.exit(0);
    }

}
