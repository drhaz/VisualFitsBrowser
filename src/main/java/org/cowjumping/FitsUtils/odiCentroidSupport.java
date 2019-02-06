package org.cowjumping.FitsUtils;

import java.awt.Rectangle;
import java.util.Arrays;


import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class odiCentroidSupport {

    static Logger myLogger = Logger.getLogger(odiCentroidSupport.class);

    /**
     * Supporting function to translate x/y positions into array indices of a
     * linear data array. The return value is checked for buffer over/under flow
     * issues.
     *
     * @param xx   x pixel position
     * @param yy   y pixel position
     * @param dimX X-dimension of the image
     * @param dimY Y-dimension of the image
     * @return
     */
    static final int getImageIndex(int xx, int yy, int dimX, int dimY) {

        int index = yy * dimX + xx;
        if (index >= dimX * dimY)
            index = 0;
        if (index < 0)
            index = 0;
        return index;
    }

    /**
     * perform aperture photometry. The aperture is based on the FWHM of the
     * star.
     */
    public static void aperturePhotometry(ImageContainer gs) {

        int dimX = gs.getImageDimX();
        int dimY = gs.getImageDimY();
        float[] image = gs.getRawImageBuffer();
        double totalFlux = 0;
        // determine the aperture: FWHM.

        double apertureRadius = 1.5 * (gs.getFWHM_X() + gs.getFWHM_X()) / 2;
        double apertureRadiusSqr = apertureRadius * apertureRadius;

        // would the aperture be in the image?

        float xc = gs.getCenterX();
        float yc = gs.getCenterY();

        // Constrain the sub window in the array
        int minX = (int) (xc - apertureRadius);
        int minY = (int) (yc - apertureRadius);
        int maxX = (int) (xc + apertureRadius);
        int maxY = (int) (yc + apertureRadius);

        if (minX < 0 || minY < 0 || maxX > dimX - 1 || maxY > dimY - 1) {
            if (myLogger.isDebugEnabled())
                myLogger.warn(
                        "Aperture photometry: aperture does not fit into image. Adjusting save boundaries, but photometry will be wrong");

            minX = Math.max(0, minX);
            maxX = Math.min(dimX - 1, maxX);
            minY = Math.max(0, minY);
            maxY = Math.min(dimY - 1, maxY);

        }

        float sky = gs.getBackground();
        float nPixels = 0;
        for (int xx = minX; xx <= maxX; xx++)
            for (int yy = minY; yy <= maxY; yy++) {

                double radius2 = ((xx - xc) * (xx - xc) + (yy - yc) * (yy - yc));
                if (radius2 <= apertureRadiusSqr) {
                    totalFlux += image[getImageIndex(xx, yy, dimX, dimY)] - sky;
                    nPixels++;
                }

            }

        gs.setFlux((float) totalFlux);

        float expTime = gs.getExptime();
        if (expTime <= 0)
            expTime = 1;

        gs.setInstMag((float) (-2.5 * Math.log10(totalFlux / expTime)));

        double sn = totalFlux / (Math.sqrt(totalFlux + nPixels * (gs.getBackNoise() * gs.getBackNoise())));
        gs.setSN((float) sn);
    }

    /**
     * Determine the FWHM and fitted peak value of a star from a gaussian fit.
     * <p>
     * Only data that is at least 3-sigma above the sky level will be used in
     * the fit.
     */

    public static void gaussianFitFWHM(ImageContainer gs) {

        int dimX = gs.getImageDimX();
        int dimY = gs.getImageDimY();

        float xce = gs.getCenterX();
        float yce = gs.getCenterY();

        float fwhm = (gs.getFWHM_X() + gs.getFWHM_Y()) / 2f;

        double fitRangeSigma = 0.9;

        // Only data in a box with side length of 1 fwhm will be used, since
        // everything else will be sky anyway.
        Rectangle window = new Rectangle((int) (xce - fitRangeSigma * fwhm), (int) (yce - fitRangeSigma * fwhm), (int) (2 * fitRangeSigma * fwhm) + 1,
                (int) (2 * fitRangeSigma * fwhm) + 1);

        // Check boundary conditions
        Rectangle imageFrame = new Rectangle(0, 0, dimX, dimY);
        Rectangle safePixelArea = window.intersection(imageFrame);

        if (safePixelArea.width > 0 && safePixelArea.height > 0) {
            // Extract the radial profile
            RadialProfile myProfile = new RadialProfile();
            myProfile.update(xce, yce, gs, safePixelArea, gs.getBackground() + gs.getBackNoise() * 3);

            if (myLogger.isDebugEnabled())
                myLogger.debug("gaussianFitFWHM: getting radialprofile from window: " + safePixelArea + "containing "
                        + myProfile.getNElements() + " data points");

            // subtract the sky level from the radial plot
            odiCentroidSupport.scaleAndBiasImage(myProfile.value, 1, gs.getBackground());

            // and do the fitting
            fitOneDGauss(myProfile, gs);
        } else {
            if (myLogger.isDebugEnabled())
                myLogger.debug("No overlapping area between object and pixel frame. Rejecting");
            gs.setFWHM_X(0);
            gs.setFWHM_Y(0);
        }

    }

    protected static void prepareProfileForGaussFit(RadialProfile profile) {

        if (profile.getNElements() == 0)
            return;

        profile.error = new float[profile.getNElements()];

        for (int ii = 0; ii < profile.getNElements(); ii++) {

            double value = profile.value[ii];
            double error = Math.sqrt(value + 10 * 10); // shot noise plus read
            // noise

            if (value > 0) {
                double dvalue = value + error;
                value = Math.log(value);
                error = Math.log(dvalue) - value;
            } else {
                error = 0;
            }

            profile.value[ii] = (float) value;
            profile.error[ii] = (float) error;
            profile.radius[ii] = (profile.radius[ii]) * (profile.radius[ii]);

        }
    }

    /**
     * Fit a gaussian function to a radial profile.
     * <p>
     * This is not a truly non-linear fit, but we linearize the problem by
     * taking the logarithm of the flux data:
     * <p>
     * f(x) = a * e^ { -(x)^2 / (2 s^2) }
     * <p>
     * ln f(x) = ln(a) - { x^2/ (2 s^2) }
     * <p>
     * <p>
     * so fit a function y' = a' + b' * x'
     * <p>
     * where a' = ln(a)
     * <p>
     * b' = -1 / (2s^2)
     * <p>
     * x' = x^2
     * <p>
     * The data has to fullfill a few critical conditions: - data have to be sky
     * subtracted - all data has to be larger than 0.
     */

    public static void fitOneDGauss(RadialProfile myProfile, ImageContainer gs) {

        // Adjust data to enable linear fitting.
        prepareProfileForGaussFit(myProfile);

        double[] retVal = linearFitRej(myProfile, 3, 1.);
        double fwhm = 0;
        double fittedPeak = 0;
        double pixelPeak = gs.getPeak() - gs.getBackground();

        fittedPeak = (float) Math.exp(retVal[0]);

        if (retVal[1] != Float.NaN) {
            if (retVal[1] != 0)
                fwhm = (float) (Math.sqrt(-1. / 2. / retVal[1]) * 2.35482);
            else
                fwhm = 0;
        }

        if (myLogger.isDebugEnabled())
            myLogger.debug("fitOneDGauss: " + fittedPeak + " " + fwhm);

        gs.setFWHM_X((float) fwhm);
        gs.setFWHM_Y((float) fwhm);

        double peakFitRatio = (pixelPeak / fittedPeak);
        gs.setSharp((float) peakFitRatio);
        if (peakFitRatio > 1.5 || peakFitRatio < 0.5)
            gs.setViable(false);

        double goodness = Math.sqrt(retVal[2]) / retVal[3];
        gs.setChi((float) goodness);
        if (goodness > .1)
            gs.setViable(false);

        if (myLogger.isInfoEnabled()) {
            myLogger.debug(String.format("Gauss peak to pixel peak ratio: %7.3f", peakFitRatio, 3));
            myLogger.debug("Goodness of fit: " + goodness);
        }

    }

    private static double[] linearFitRej(RadialProfile myProfile, int nIter, double rej) {

        double[] retVal = new double[]{0, 0, 0, 0};
        int maxGood = myProfile.getNElements();

        for (int ii = 0; ii < nIter; ii++) {
            if (maxGood < 3)
                break; // do not fit with too few data!

            retVal = linearFit(myProfile, maxGood);
            retVal[3] = maxGood;

            if (ii < nIter - 1) {
                // do the rejection
                for (int jj = 0; jj < maxGood; jj++) {

                    double calc = retVal[0] + retVal[1] * myProfile.getRadius(jj);
                    double value = myProfile.getValue(jj);
                    double error = myProfile.getError(jj);

                    double delta = Math.abs(value - calc);

                    if (delta > rej * error) {
                        myProfile.swap(jj, maxGood - 1);
                        maxGood--;
                    }

                }
            }

        }
        if (myLogger.isDebugEnabled())
            myLogger.debug("A total of " + (myProfile.getNElements() - maxGood) + " values were rejected.");
        return retVal;
    }

    public static double[] linearFit(RadialProfile myProfile) {
        return linearFit(myProfile, myProfile.getNElements());
    }

    /**
     * Do a linear least square fit to a set of data.
     * <p>
     * f(x) = a + bx The algorithms is taken from Numerical Recipies [update
     * reference]
     *
     * @return float[0] = a; float[1] = b
     */
    private static double[] linearFit(RadialProfile myProfile, int maxgood) {

        double[] retVal = new double[4];
        double Sx = 0;
        double Sy = 0;
        double Sxy = 0;
        double Sxx = 0;
        double S = 0;

        for (int ii = 0; ii < Math.min(maxgood, myProfile.getNElements()); ii++) {

            double x = myProfile.getRadius(ii);
            double y = myProfile.getValue(ii);
            double w = myProfile.getError(ii);

            if (x > 0 && w != 0) {

                w = w != 0 ? 1 : w * w / y; // We weigh by inverse Signal to
                // noise
                S += 1 / w;
                Sx += x / w;
                Sy += y / w;
                Sxy += x * y / w;
                Sxx += x * x / w;
            }
        }

        double d = S * Sxx - (Sx * Sx);
        if (d == 0) {
            if (myLogger.isDebugEnabled())
                myLogger.warn("linearFit: singular Matrix!");
            retVal[0] = -1;
            retVal[1] = -1;
            retVal[2] = 0;
            retVal[3] = 0;
        } else {
            if (myLogger.isDebugEnabled()) {
                myLogger.debug("Linear fit sums: ");
                myLogger.debug("S   -  " + S);
                myLogger.debug("Sx  -  " + Sx);
                myLogger.debug("Sxx -  " + Sxx);
                myLogger.debug("Sy  -  " + Sy);
                myLogger.debug("Sxy -  " + Sxy);
                myLogger.debug("det -  " + d);
            }
            retVal[0] = (float) ((Sxx * Sy - Sx * Sxy) / d);
            retVal[1] = (float) ((S * Sxy - Sx * Sy) / d);
            double xi2 = 0;
            for (int ii = 0; ii < Math.min(maxgood, myProfile.getNElements()); ii++) {
                double val = myProfile.getValue(ii) - retVal[0] - myProfile.getRadius(ii) * retVal[1];
                val /= myProfile.getError(ii);
                xi2 += val * val;
            }
            // OK, we have a final X2
            retVal[2] = xi2;
            retVal[3] = maxgood;
        }

        if (myLogger.isDebugEnabled())
            myLogger.debug("[linearFit] Result: " + retVal[0] + " " + retVal[1]);
        return retVal;

    }

    /**
     * Determine the center of an object in a subimage by first moment analysis.
     *
     * @param minX window in which to do the centroiding
     * @param maxX window in which to do the centroiding
     * @param minY window in which to do the centroiding
     * @param maxY window in which to do the centroiding
     * @return
     */

    public static void MomentAnalysis(ImageContainer gs, int minX, int maxX, int minY, int maxY) {

        float sigmaThres = 3; // how many sigma above background does flux need
        // to be in order to count?
        float sky = gs.getBackground();
        float peakflux = gs.getPeak() - sky;
        float noise = gs.getBackNoise();

        double totalFlux = 0;

        double max = 0;
        double stellarFlux = 0;

        float[] image = gs.getRawImageBuffer();
        int dimX = gs.getImageDimX();
        int dimY = gs.getImageDimY();

        if (peakflux < 0 && myLogger.isDebugEnabled()) {
            myLogger.debug("Peak value of star is <0. That is fishy. Continuing anyway.");
        }

        // check boundaries
        if (minX < 0)
            minX = 0;
        if (maxX > dimX)
            maxX = dimX;
        if (minY < 0)
            minY = 0;
        if (maxY > dimY)
            maxY = dimY;

        // One sanity check: is the threshold compatible with the noise?
        float dr = peakflux;
        if (noise > 0) {
            dr /= noise;
            gs.setSN(dr);
            if (myLogger.isDebugEnabled() && dr < 5)
                myLogger.debug("MomentAnalysis: Star has low S/N");
        } else {
            gs.setSN(0f);
        }

        // minimum flux that we need in order to be accounted for in the moment
        // analysis

        float minimumFlux = noise * sigmaThres;

        double momentXX = 0;
        double momentYY = 0;
        double momentXY = 0;
        double centerX = 0;
        double centerY = 0;

        // TODO: Why the -1?
        for (int xx = minX; xx < maxX - 1; xx++) {
            for (int yy = minY; yy < maxY - 1; yy++) {

                float pixelFlux = image[getImageIndex(xx, yy, dimX, dimY)];
                // while we are iterating anyways we can as well improve the
                // peak estimate.:
                if (pixelFlux > max) {
                    max = pixelFlux;
                }

                pixelFlux -= sky;
                // Do some basic flux estimate

                if (pixelFlux >= minimumFlux) {
                    // TODO: should this be summed outside the noise threshold?
                    stellarFlux += pixelFlux;

                    centerX += pixelFlux * (float) xx;
                    centerY += pixelFlux * (float) yy;

                    totalFlux += pixelFlux;

                }

            }
        }

        if (totalFlux > 0) {
            centerX = (centerX / totalFlux);
            centerY = (centerY / totalFlux);
        } else {

            centerX = 0;
            centerY = 0;
        }
        gs.setCenterX((float) centerX);
        gs.setCenterY((float) centerY);

        // Now: Calculate higher moments
        for (int xx = minX; xx < maxX - 1; xx++) {
            for (int yy = minY; yy < maxY - 1; yy++) {

                float flux = image[getImageIndex(xx, yy, dimX, dimY)];

                flux -= sky;

                if (flux >= minimumFlux) {
                    double dx = xx - centerX;
                    double dy = yy - centerY;
                    momentXX += flux * (float) (dx * dx);
                    momentYY += flux * (float) (dy * dy);
                    momentXY += flux * (float) (dx * dy);

                }

            }
        }
        if (totalFlux > 0) {
            momentXX /= totalFlux;
            momentYY /= totalFlux;
            momentXY /= totalFlux;
        } else {
            totalFlux = 0;
            momentXX = 0;
            momentYY = 0;
            momentXY = 0;

        }
        double fwhmX = Math.sqrt(momentXX) * 2.3548;
        double fwhmY = Math.sqrt(momentYY) * 2.3548;
        gs.setFWHM_X((float) fwhmX);
        gs.setFWHM_Y((float) fwhmY);
        gs.setFlux((float) stellarFlux);
        gs.setPeak((float) max);
        if (fwhmX + fwhmY == 0) {
            gs.setRoundness((float) 0);
        } else {
            gs.setRoundness((float) ((fwhmX - fwhmY) / (fwhmX + fwhmY)));
        }
        gs.setMomentXY((float) momentXY);

    }

    /**
     * Search the maximum image value and determine sky level & sky noise in one
     * pass.
     */
    public static void findSkyandPeak(ImageContainer gs, int skydelta, int skyband)

    {
        int dimX = gs.getImageDimX();
        int dimY = gs.getImageDimY();
        float[] image = gs.getRawImageBuffer();

        if (skydelta + skyband > dimX)
            myLogger.warn("findSkyandPeak: out of bounds: " + (skydelta + skyband) + " is larger than image X " + dimX);

        double[] sums = {0, 0, 0, 0}; // will hold the sky values in the
        // corners
        long[] nSums = {0, 0, 0, 0};

        float min = image[skydelta + skyband];
        float max = min;
        int maxIndex = 0;

        // We do one pass over the whole array to find the peak value. In the
        // same pass we sum the flux in little squares in the corners to
        // estimate the sky.

        for (int yy = skydelta; yy + skydelta < dimY; yy++) {

            for (int xx = skydelta; xx + skydelta < dimX; xx++) {

                int imageIndex = getImageIndex(xx, yy, dimX, dimY);
                float imageValue = image[imageIndex];

                if (imageValue > 0 && imageValue < 65000) {
                    // check for the brightest/dimmest pixel that is in the
                    // central region, i.e., not in skyband or skydelta
                    if (xx > skydelta + skyband && xx < dimX - skyband - skydelta && yy > skyband + skydelta
                            && yy < dimY - skyband - skydelta) {

                        if (min > imageValue)
                            min = imageValue;

                        if (max < imageValue) {
                            max = imageValue;
                            maxIndex = imageIndex;
                        }
                    }

                    // potentially we are in the left sky area
                    if (xx >= skydelta && xx < skydelta + skyband) {

                        if (yy >= skydelta && yy < skydelta + skyband) {
                            sums[0] += imageValue; // lower left sky
                            nSums[0]++;
                        } else if (yy < dimY - skydelta && yy >= dimY - skyband - skydelta) {
                            sums[2] += imageValue; // upper left
                            nSums[2]++;
                        }

                        // or in the right sky area:
                    } else if (xx < dimX - skydelta && xx >= dimX - skydelta - skyband)

                        if (yy >= skydelta && yy < skydelta + skyband) {
                            sums[1] += imageValue; // lower right sky
                            nSums[1]++;
                        } else if (yy < dimY - skydelta && yy >= dimY - skyband - skydelta) {
                            sums[3] += imageValue; // upper right sky
                            nSums[3]++;
                        }

                }
            }

        }
        // store location and property of brightest pixel

        gs.setPeak(max);
        gs.setCenterX((int) maxIndex % dimX);
        gs.setCenterY((int) maxIndex / dimX);
        gs.setPeakX(gs.getCenterX());
        gs.setPeakY(gs.getCenterY());

        // normalize the sky counts to the number of pixels invlved.
        for (int ii = 0; ii < 4; ii++) {
            sums[ii] = nSums[ii] > 0 ? sums[ii] / nSums[ii] : 0;
            if (myLogger.isDebugEnabled())
                myLogger.debug(" Sky Value:  " + ii + "  " + sums[ii] + "  N: " + nSums[ii]);
        }

        // we reject the highest and lowest value values here!
        Arrays.sort(sums);
        float skyLevel = (float) ((sums[1] + sums[2]) / 2.);
        gs.setBackground(skyLevel);

        // now determine noise of sky in a second pass

        double variance[] = new double[4];

        for (int ii = 0; ii < variance.length; ii++) {
            variance[ii] = 0;
            nSums[ii] = 0;
        }
        double v = 0;
        for (int xx = 0; xx < skyband; xx++)
            for (int yy = 0; yy < skyband; yy++) {

                v = image[getImageIndex(skydelta + xx, skydelta + yy, dimX, dimY)];
                if (v > 0 && v < 65000) {
                    v -= sums[0];
                    variance[0] += v * v;
                    nSums[0]++;
                }

                v = image[getImageIndex(dimX - skydelta - xx, skydelta + yy, dimX, dimY)];
                if (v > 0 && v < 65000) {
                    v -= sums[1];
                    variance[1] += v * v;
                    nSums[1]++;
                }

                v = image[getImageIndex(skydelta + xx, dimY - skydelta - yy, dimX, dimY)];
                if (v > 0 && v < 65000) {
                    v -= sums[2];
                    variance[2] += v * v;
                    nSums[2]++;
                }

                v = image[getImageIndex(dimX - skydelta - xx, dimY - skydelta - yy, dimX, dimY)];
                if (v > 0 && v < 65000) {
                    v -= sums[3];
                    variance[3] += v * v;
                    nSums[3]++;
                }

            }

        for (int ii = 0; ii < 4; ii++) {
            if (nSums[ii] != 0)
                variance[ii] = Math.sqrt(variance[ii] / nSums[ii]);
            else
                variance[ii] = 0;
        }

        if (myLogger.isDebugEnabled())
            myLogger.debug("Individual sky variances: " + variance[0] + "  " + variance[1] + "  " + variance[2] + "  "
                    + variance[3]);

        Arrays.sort(variance);
        gs.setBackNoise((float) ((variance[1] + variance[2]) / 2));

        if (myLogger.isDebugEnabled()) {

            myLogger.debug("Peak: " + gs.getPeak() + "  at location " + gs.getCenterX() + " / " + gs.getCenterY());

            myLogger.debug("Background Level: " + gs.getBackground() + " Noise:  " + gs.getBackNoise());
        }

    }

    // /////////////////////////////////////////////////////////
    // /
    // / General image handling routines.
    // /
    // //////////////////////////////////////////////////////////

    /**
     * Copy a short[] array to a float[] array with valid type conversion.
     *
     * @param ImageS source array
     * @param ImageF target array, which has to be allocated.
     */
    public static void copyImageShortToFloat(short[] ImageS, float[] ImageF) {

        if (ImageS.length != ImageF.length) {
            myLogger.error("copyImageShortToFloat: Arrays are of unequal size! Aborting");
            return;
        }
        for (int ii = 0; ii < ImageF.length; ii++) {

            ImageF[ii] = (float) (ImageS[ii] & 0xffff);

        }

    }

    public static void copyImageFloatToShort(float[] ImageF, short[] ImageS) {
        if (ImageS.length != ImageF.length) {
            myLogger.error("copyImageFloatToShort: Arrays are of unequal size! Aborting");
            return;
        }
        for (int ii = 0; ii < ImageF.length; ii++) {

            ImageS[ii] = (short) (Math.round(ImageF[ii]) & 0xFFFF);

        }

    }

    /**
     * Rescale a float[] array
     * <p>
     * Image[i] = (Image[i]-bzero )/ scale
     *
     * @param Image input float[] array.
     * @param scale
     * @param bzero
     */

    public static void scaleAndBiasImage(float[] Image, float scale, float bzero) {
        if (Image != null) {
            if (myLogger.isDebugEnabled())
                myLogger.debug("Rescaleimage with values: " + scale + " " + bzero);
            for (int ii = 0; ii < Image.length; ii++) {
                Image[ii] = (Image[ii] - bzero) / scale;

            }

        } else
            myLogger.warn("Attempt to scale image, but null image was given!");
    }

    public static float findMedian(ImageContainer gs, int ymin, int ymax, int xmin, int xmax) {
        float ovValue;
        int n = (ymax - ymin + 1) * (xmax - xmin + 1);

        float[] ovBuffer = new float[n];

        n = 0;
        for (int row = ymin; row < ymax; row++) {
            for (int xx = xmin; xx < xmax; xx++) {

                int idx = row * gs.getImageDimX() + xx;

                float value = gs.rawImageBuffer[idx];

                ovBuffer[n++] = value;
            }
        }

        //ovValue = (starFinder.selectKth(ovBuffer, (n - 1) / 2, n - 1, true));
        return 0;
    }

    public static double findMean(ImageContainer gs, int minx, int maxx, int miny, int maxy, double sigma) {

        if (maxx > gs.getImageDimX())
            maxx = gs.getImageDimX();
        if (maxy > gs.getImageDimY())
            maxy = gs.getImageDimY();
        if (minx < 0)
            minx = 0;
        if (miny < 0)
            miny = 0;

        double n = (maxx - minx) * (maxy - miny);
        int NAXIS1 = gs.getImageDimX();

        double mean = 0;
        double ovValue = gs.getOVLevel();

        if (n <= 0) {
            myLogger.error("findMean: Empty region. Abort.");
            return 0;
        }

        double sum = 0;
        n = 0;
        for (int yy = miny; yy < maxy; yy++)
            for (int xx = minx; xx < maxx; xx++) {

                float imgval = gs.rawImageBuffer[yy * NAXIS1 + xx];

                sum += imgval;
                n++;

            }

        mean = sum / n - ovValue;

        if (sigma > 0) {
            double stddev = findStdDev(gs, mean, minx, maxx, miny, maxy, 0);
            sum = 0;
            n = 0;
            mean += ovValue;
            for (int yy = miny; yy < maxy; yy++)
                for (int xx = minx; xx < maxx; xx++) {

                    double val = gs.rawImageBuffer[yy * NAXIS1 + xx];


                    sum += val;
                    n++;

                }

            mean = ((double) sum) / n - ovValue;

        }

        return mean;
    }

    public static double findStdDev(ImageContainer gs, double mean, int minx, int maxx, int miny, int maxy,
                                    double sigmarejection) {

        double stddev = 0;

        int dimX = gs.getImageDimX();
        if (maxx >= gs.getImageDimX() - 1)
            maxx = gs.getImageDimX();
        if (maxy >= gs.getImageDimY() - 1)
            maxy = gs.getImageDimY();

        double ovValue = gs.getOVLevel();
        double n = (maxx - minx) * (maxy - miny);
        n = 0;

        int rejected = 0;
        for (int yy = miny; yy < maxy; yy++)
            for (int xx = minx; xx < maxx; xx++) {

                int imgval = ((int) (gs.rawImageBuffer[yy * dimX + xx]) & 0xffff);


                double delta = mean - (imgval - ovValue);

                stddev += delta * delta;
                n++;

            }
        if (myLogger.isDebugEnabled() && rejected > 0)
            myLogger.debug("Stddev: Rejected " + rejected + " pixels for bad value");

        if (n <= 0) {
            myLogger.debug("findStdDev: No valid pixels found!");
            return 0;

        }

        stddev = Math.sqrt(stddev / n);

        // now with sigma rejection
        if (sigmarejection > 0 && stddev > 0) {
            n = 0;
            double newstddev = 0;

            for (int yy = miny; yy < maxy; yy++)
                for (int xx = minx; xx < maxx; xx++) {

                    // Now conversion to unsigned is deadly here because one can
                    // assume that we have a mean of 0 in the short buffer by
                    // now.
                    // this is a real deficiency here.

                    float imgval = gs.rawImageBuffer[yy * dimX + xx];


                    double delta = mean - (imgval - ovValue);

                    if (Math.abs(delta / stddev) < sigmarejection) {
                        newstddev += delta * delta;
                        n++;
                    }


                }
            stddev = Math.sqrt(newstddev / n);
        }

        return stddev;
    }

    // /////////////////////////////////////
    // /
    // / Some test routines here
    // /
    // ///////////////////////////////////////

    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.DEBUG);
        RadialProfile p = new RadialProfile();
        p.radius = new float[]{1f, 2f, 3f, 4f, 5f, 6f};
        p.value = new float[]{3f, 4f, 5f, 6.2f, 7.01f, 8f};
        p.error = new float[]{0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f};

        double res[] = odiCentroidSupport.linearFit(p);

        System.err.println("Linear Fit Test:");
        System.err.println("f(x) = a + b*x -> a=" + res[0] + "  b=" + res[1] + " X2=" + res[2] + "  N=" + res[3]);

        res = odiCentroidSupport.linearFitRej(p, 3, 0.2);

//		System.err.println("Linear Fit Test:");
//		System.err.println("f(x) = a + b*x -> a=" + res[0] + "  b=" + res[1] + " X2=" + res[2] + "  N=" + res[3]);
//
//		gaussImage gi = new gaussImage(24);
//		gi.create(12, 12, 120, 4, 4, 5, 0);
//		odiGaussFitCentroider.centroid(gi);
//		System.out.println(gi);
//
//		OTAReader.correctOV = false;
//
//		Vector<GuideStarContainer> otaCells = OTAReader.readDetectorMEFFile(
//				"/Volumes/odifile/archive/podi/TEST-12B-2101/2012.08.20/b20120820T210352.3", 0, 0, 1, 1);
//		GuideStarContainer gs = otaCells.firstElement();
//
//		double lastd = 0;
//		for (int ii = 0; ii < 1; ii++) {
//
//			double m = odiCentroidSupport.findMean(gs, 150, 250, 150, 250, 0);
//			double s = odiCentroidSupport.findStdDev(gs, m, 150, 250, 150, 250, 0);
//			System.out.println(" Noise in image: " + m + "  " + s);
//
//		}
//		System.exit(1);

    }

}
