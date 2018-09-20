package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imagemath;

import clearcl.ClearCLImage;
import clearcl.imagej.ClearCLIJ;
import clearcl.imagej.kernels.Kernels;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsShowingTheResult;
import ij.IJ;
import ij.ImagePlus;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * The EqualizeMeanGreyValueOfSlices plugin allows equalizing the mean average grey value of all slices
 *
 * Author: @haesleinhuepf
 * September 2018
 */

@Plugin(type = Command.class, menuPath = "SpimCat>Internal (experimental)>Equalize grey values of slices")
public class EqualizeMeanGreyValueOfSlices implements Command, AllowsShowingTheResult {

    @Deprecated
    public EqualizeMeanGreyValueOfSlices() {
        super();
    }

    public EqualizeMeanGreyValueOfSlices(ImagePlus input) {
        super();
        this.input = input;
    }

    @Parameter
    ImagePlus input;


    boolean showResult = true;

    ImagePlus output = null;

    @Override
    public void run() {
        // initialize / convert images to OpenCL
        ClearCLIJ clij = ClearCLIJ.getInstance();
        ClearCLImage inputCL = clij.converter(input).getClearCLImage();
        ClearCLImage outputCL = clij.createCLImage(inputCL);

        // determine normalisation factors per slice
        double[] sums = Kernels.sumPixelsSliceBySlice(clij, inputCL);
        double maximumSum = new Max().evaluate(sums);
        float[] factors = new float[sums.length];
        for (int i = 0; i < factors.length; i++) {
            factors[i] = (float) (maximumSum / sums[i]);
        }

        // multiply image stack slice by slice with given scalars
        Kernels.multiplySliceBySliceWithScalars(clij, inputCL, outputCL, factors);

        // convert back
        output = clij.converter(outputCL).getImagePlus();

        outputCL.close();
        inputCL.close();

        if (showResult) {
            output.show();
        }
    }

    @Override
    public void setShowResult(boolean value) {
        showResult = value;
    }

    public ImagePlus getOutput() {
        if (output == null) {
            run();
        }
        return output;
    }

    public static void main(String ... args) {

        new ij.ImageJ();

        ImagePlus
                input = IJ.openImage("C:/structure/data/fish_GAFASO.tif");
        input.show();

        EqualizeMeanGreyValueOfSlices emgvos = new EqualizeMeanGreyValueOfSlices(input);
        emgvos.setShowResult(false);
        ImagePlus result = emgvos.getOutput();
        result.show();
        result.setTitle("equalized mean grey");
    }
}















