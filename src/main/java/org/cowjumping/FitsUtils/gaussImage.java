package org.cowjumping.FitsUtils;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

import org.cowjumping.FitsUtils.ImageContainer;


@SuppressWarnings("serial")
public class gaussImage extends ImageContainer {

    static Random myRandom = new Random();
    // public int dimension = 16;


    // public short[] simage = null;

    public gaussImage() {
        this(32, 32);
    }


    public gaussImage(int dimX) {
        this(dimX, dimX);
    }

    public gaussImage(int dimX, int dimY) {
        this.setImageDimX(dimX);
        this.setImageDimY(dimY);
        this.rawImageBuffer = new float[dimX * dimY];
    }

    int clamp(int x, int low, int high) {
        return (x < low) ? low : ((x > high) ? high : x);
    }


    public void add (float xce, float yce, float peak, float fwhmX,
                     float fwhmY, float readnoise, float bias) {

        gaussImage temp = new gaussImage(this.imageDimX, this.imageDimY);
        temp.create(xce,yce,peak,fwhmX,fwhmY, readnoise, bias);

        for  (int ii = 0; ii < rawImageBuffer.length; ii++) {
            rawImageBuffer[ii] += temp.rawImageBuffer[ii];
        }



    }

    public void create(float xce, float yce, float peak, float fwhmX,
                       float fwhmY, float readnoise, float bias) {

        double cX = fwhmX / 2.35482;
        cX = 2 * cX * cX;
        double cY = fwhmY / 2.35482;
        cY = 2 * cY * cY;

        for (float xx = 0; xx < this.getImageDimX(); xx++)
            for (float yy = 0; yy < this.getImageDimY(); yy++) {
                float dx = (xx - xce) * getBinningX();
                float dy = (yy - yce) * getBinningY();
                dx = dx * dx;
                dy = dy * dy;

                float s = (float) Math.exp(-(dx / cX) - (dy / cY));
                float n = (float) (myRandom.nextGaussian() * readnoise + bias);
                float flux = peak * s;
                flux = (float) (flux + myRandom.nextGaussian() * Math.sqrt(flux)) * getBinningX() * getBinningY();
                float value = flux + n ;


                rawImageBuffer[(int) (yy * this.getImageDimX() + xx)] = value;

            }
    }

    public void safe(String fname) {

        File output = new File(fname);
        BufferedImage bi = new BufferedImage(this.getImageDimX(),
                this.getImageDimY(), BufferedImage.TYPE_USHORT_GRAY);

        WritableRaster ras = bi.getRaster();
        short[] b = new short[rawImageBuffer.length];
        for (int ii = 0; ii < rawImageBuffer.length; ii++) {
            b[ii] = (short) (clamp( Math.round(rawImageBuffer[ii]), 0, Short.MAX_VALUE * 2) & 0xFFFF);
        }

        ras.setDataElements(0, 0, this.getImageDimX(), this.getImageDimY(),
                b);

        try {
            ImageIO.write(bi, "png", output);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public static void main(String args[]) {

        gaussImage g = new gaussImage(128, 128);
        g.setBinningY((short) 2);

        g.create(64, 64, 255 * 250, 24, 24, 0, 0);

        g.safe("gauss.png");


        //   odiGaussFitCentroider.centroid(g);
        System.out.println("Image metrics: \n" + "xce: " + g.getCenterX() + "\n"
                + "yce: " + g.getCenterY());

        System.out.println("fwhm X: " + g.getFWHM_X());
        System.out.println("fwhm Y: " + g.getFWHM_X());
        System.out.println("Peak  : " + g.getPeak());
        System.exit(0);
        ;
    }

}