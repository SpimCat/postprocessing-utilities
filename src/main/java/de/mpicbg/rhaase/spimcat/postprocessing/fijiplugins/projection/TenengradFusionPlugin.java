package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.projection;

import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.imagej.ClearCLIJ;
import clearcl.imagej.kernels.Kernels;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * TenengradFusionPlugin
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 09 2018
 */

@Plugin(type = Command.class, menuPath = "SpimCat>Projection>Tenengrad fusion")
public class TenengradFusionPlugin implements Command {
    @Parameter
    ImagePlus input;


    public static double blurWeightsSigmaX = 15;
    public static double blurWeightsSigmaY = 15;
    public static double blurWeightsSigmaZ = 5;

    public static boolean silent = false;

    public static boolean showResult = true;

    @Override
    public void run() {
        if (!showDialog()) {
            return;
        }

        ImagePlus result = process(input);
        if (showResult && result != null) {
            result.show();
        }
    }

    public ImagePlus process(ImagePlus input) {

        int numberOfImages = input.getNSlices();
        if (numberOfImages > 12) {
            IJ.log("Tenengrad fusion just supports up to 12 images consider fusing subsequently!");
            return null;
        }

        ClearCLIJ clij = ClearCLIJ.getInstance();
        ClearCLImage inputCLImage = clij.converter(input).getClearCLImage();
        ClearCLImage outputCLImage = clij.createCLImage(new long[] {inputCLImage.getWidth(), inputCLImage.getHeight(), 1}, inputCLImage.getChannelDataType());

        ClearCLImage[] slices = new ClearCLImage[numberOfImages];
        for (int i = 0; i < numberOfImages; i++) {
            slices[i] = clij.createCLImage(new long[]{inputCLImage.getWidth(), inputCLImage.getHeight(), 1}, ImageChannelDataType.Float);
        }

        Kernels.splitStack(clij, inputCLImage, slices);

        Kernels.tenengradFusion(clij, outputCLImage, new float[]{(float)blurWeightsSigmaX, (float)blurWeightsSigmaY, (float)blurWeightsSigmaZ}, slices);

        ImagePlus output = clij.converter(outputCLImage).getImagePlus();

        for (int i = 0; i < numberOfImages; i++) {
            slices[i].close();
        }

        inputCLImage.close();
        outputCLImage.close();

        return output;
    }

    private static boolean showDialog() {
        if (silent) {
            return true;
        }

        GenericDialog gd = new GenericDialog("Tenengrad fusion");
        gd.addNumericField("Blur weights in X sigma (in pixels)", blurWeightsSigmaX, 2);
        gd.addNumericField("Blur weights in Y sigma (in pixels)", blurWeightsSigmaY, 2);
        gd.addNumericField("Blur weights in Z sigma (in pixels)", blurWeightsSigmaZ, 2);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        }

        blurWeightsSigmaX = gd.getNextNumber();
        blurWeightsSigmaY = gd.getNextNumber();
        blurWeightsSigmaZ = gd.getNextNumber();

        return true;
    }

    public static void main(String... args){
        new ImageJ();
        IJ.open("C:\\structure\\data\\Tribolium_early_membrane.tif");

        TenengradFusionPlugin tfp = new TenengradFusionPlugin();
        tfp.showDialog();

        ImagePlus result = tfp.process(IJ.getImage());
        result.show();

    }
}
