package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.projection.presentation;

import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsShowingTheResult;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsSilentProcessing;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.DeliversOutputImage;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.TakesAnInputImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 *
 *
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * January 2018
 */

@Plugin(type = Command.class, menuPath = "XWing>Presentation>Half stack projection")
public class HalfStackProjectionPlugin implements Command, AllowsSilentProcessing, TakesAnInputImage, DeliversOutputImage, AllowsShowingTheResult
{
    @Parameter
    private ImagePlus inputImage;

    public static boolean silent = false;

    public static int minSlice = 0;
    public static int maxSlice = 50;

    public static boolean cameraOffsetCorrection = false;
    public static double minimumGreyValue = 105;

    public static boolean backgroundSubtraction = false;
    public static int backgroundSubtractionRollingBallSize = 20;

    public static boolean unevenIlluminationCorrectionBeforeProjection = true;
    public static boolean unevenIlluminationCorrectionAfterProjection = false;
    public static double unevenIlluminationCorrectionGaussSigma = 50;

    public static boolean autoContrast = true;

    public static boolean showResult = true;

    private ImagePlus outputImage;

    @Override
    public void run() {
        if (!silent) {
            if (!showDialog()) {
                return;
            }
        }
        //inputImage.show();

        System.out.println("minSlice: " + minSlice);
        System.out.println("maxSlice: " + maxSlice);

        System.out.println("Minimum grey value:" + minimumGreyValue);

        System.out.println("backgroundSubtraction: " + backgroundSubtraction);
        System.out.println("backgroundSubtractionRollingBallSize: " + backgroundSubtractionRollingBallSize);
        System.out.println("unevenIlluminationCorrectionBeforeProjection: " + unevenIlluminationCorrectionBeforeProjection);
        System.out.println("unevenIlluminationCorrectionAfterProjection: " + unevenIlluminationCorrectionAfterProjection);
        System.out.println("unevenIlluminationCorrectionGaussSigma: " + unevenIlluminationCorrectionGaussSigma);
        System.out.println("autoContrast: " + autoContrast);

        ImagePlus imageCopy = new Duplicator().run (inputImage,minSlice + 1, maxSlice + 1);
        IJ.run(imageCopy, "32-bit", "");
        //imageCopy.show();

        //imageCopy = new Duplicator().run (imageCopy,minSlice + 1, maxSlice + 1);
        if (cameraOffsetCorrection) {
            setPixelsBelowThresholdToZero(imageCopy, minimumGreyValue);
        }
        //imageCopy.show();
        //if (true) return;

        if (unevenIlluminationCorrectionBeforeProjection) {
            ImagePlus illuminationImage = new Duplicator().run(imageCopy, 1, imageCopy.getNSlices());
            illuminationImage.show();
            illuminationImage.setTitle("illu");
            IJ.run(illuminationImage, "Gaussian Blur...", "sigma=" + unevenIlluminationCorrectionGaussSigma + " stack");
            ImageCalculator imageCalculator = new ImageCalculator();
            imageCopy = imageCalculator.run("Divide create 32-bit stack", imageCopy, illuminationImage);
            imageCopy.show();
            imageCopy.setTitle("divi");
        }

        if (backgroundSubtraction) {
            //imageCopy = new Duplicator().run (imageCopy,minSlice + 1, maxSlice + 1);
            IJ.run(imageCopy, "Subtract Background...", "rolling=" + backgroundSubtractionRollingBallSize + " sliding stack");
            //imageCopy.show();
            //imageCopy.setTitle("backgr");
            //imageCopy = new Duplicator().run (imageCopy,minSlice + 1, maxSlice + 1);

        }


        //imageCopy.show();
        //IJ.run(imageCopy, "Z Project...", "projection=[Max Intensity]");
        //imageCopy = IJ.getImage();
        imageCopy = ZProjector.run(imageCopy, "Max Intensity");

        System.out.println("Project has " + imageCopy.getNSlices() + " slices");

        if (unevenIlluminationCorrectionAfterProjection) {
            ImagePlus illuminationImage = new Duplicator().run(imageCopy);
            IJ.run(illuminationImage, "Gaussian Blur...", "sigma=" + unevenIlluminationCorrectionGaussSigma);
            ImageCalculator imageCalculator = new ImageCalculator();
            imageCopy = imageCalculator.run("Divide create 32-bit", imageCopy, illuminationImage);
        }

        if (autoContrast) {
            IJ.run(imageCopy, "Enhance Contrast", "saturated=0.35");
        }
        outputImage = imageCopy;

        if(showResult) {
            outputImage.show();
        }

    }

    private void setPixelsBelowThresholdToZero(ImagePlus imageCopy, double minimumGreyValue) {
        for (int z = 0; z < imageCopy.getNSlices(); z++) {
            imageCopy.setZ(z + 1);
            ImageProcessor ip = imageCopy.getProcessor();
            for (int x = 0; x < imageCopy.getWidth(); x++) {
                for (int y = 0; y < imageCopy.getHeight(); y++) {
                    if (ip.getf(x, y) < minimumGreyValue) {
                        ip.setf(x,y,0);
                    } else {
                        ip.setf(x,y, ip.getf(x, y));
                    }
                }
            }
        }
    }

    private boolean showDialog() {
        GenericDialog gd = new GenericDialog("Half stack projection");

        gd.addMessage("It may make sense to just do the projection of half of the stack. Enter first and last slice you would like to take into account for the projection.");
        gd.addNumericField("First Slice (zero-based)", minSlice, 0);
        gd.addNumericField("Last Slice (zero-based)", maxSlice, 0);

        gd.addMessage("Image quality can be improved by initially removing camera offset and a certain noise level.\nYou can determine this value by taking an image slice without sample and measuring maximum intensity in this image.");
        gd.addCheckbox("Camera offset / noise subtraction", cameraOffsetCorrection);
        gd.addNumericField("Minimum grey value", minimumGreyValue, 0);

        gd.addMessage("Background subtraction helps getting a clear image of the bright structures you would like to visualise by eliminating all surrounding signal. Enter the size of the largest thinkable object you not like to loose by background subtraction.");
        gd.addCheckbox("Background subtraction", backgroundSubtraction);
        gd.addNumericField("Background subtraction rolling ball radius (in pixels)", backgroundSubtractionRollingBallSize, 0);

        gd.addMessage("Uneven illumination correction makes all bright spots similarily bright. Enter a sigma which is by one order of magnitude higher than the expected size of the objects of interest.");
        gd.addCheckbox("Uneven illumination correction before projection", unevenIlluminationCorrectionBeforeProjection);
        gd.addCheckbox("Uneven illumination correction after projection", unevenIlluminationCorrectionAfterProjection);
        gd.addNumericField("Uneven illumination correction Gaussian blur sigma (in pixels)", unevenIlluminationCorrectionGaussSigma, 2);

        gd.addMessage("'Auto contrast' clicks the 'Auto' button in the Brightness/Contrst dialog for you.");
        gd.addCheckbox("Auto contrast", autoContrast);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        }

        minSlice = Integer.max((int)gd.getNextNumber(), 0);
        maxSlice = Integer.min((int)gd.getNextNumber(), inputImage.getNSlices() - 1);

        cameraOffsetCorrection = gd.getNextBoolean();
        minimumGreyValue = gd.getNextNumber();

        backgroundSubtraction = gd.getNextBoolean();
        backgroundSubtractionRollingBallSize = (int)gd.getNextNumber();

        unevenIlluminationCorrectionBeforeProjection = gd.getNextBoolean();
        unevenIlluminationCorrectionAfterProjection = gd.getNextBoolean();
        unevenIlluminationCorrectionGaussSigma = gd.getNextNumber();

        autoContrast = gd.getNextBoolean();

        return true;
    }

    public static void main(String ... args) {
        new ImageJ();
        ImagePlus inputImage = IJ.openImage("C:/structure/data/xwing/2018-01-18-16-30-25-11-Robert_CalibZAP_Wfixed/processed/tif/000200.raw.tif");

        HalfStackProjectionPlugin halfStackProjectionPlugin = new HalfStackProjectionPlugin();
        halfStackProjectionPlugin.setInputImage(inputImage);
        halfStackProjectionPlugin.minSlice = 0;
        halfStackProjectionPlugin.maxSlice = 55;
        halfStackProjectionPlugin.run();


    }


    @Override
    public void setInputImage(ImagePlus inputImage) {
        this.inputImage = inputImage;
    }

    @Override
    public ImagePlus getOutputImage() {
        return outputImage;
    }

    @Override
    public void setSilent(boolean value) {
        HalfStackProjectionPlugin.silent = value;
    }

    @Override
    public void setShowResult(boolean value) {
        showResult = value;
    }
}
