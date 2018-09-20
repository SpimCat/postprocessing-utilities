package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.quality;

import autopilot.measures.FocusMeasures;
import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.imagej.ClearCLIJ;
import clearcl.imagej.kernels.Kernels;
import de.mpicbg.rhaase.scijava.AbstractFocusMeasuresPlugin;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsShowingTheResult;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsSilentProcessing;
import ij.IJ;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * MeasureHighQualityAreasPlugin
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 09 2018
 */
@Plugin(type = Command.class, menuPath = "SpimCat>Quality measurement>Meausre areas of high quality")
public class MeasureHighQualityAreasPlugin implements
        Command, AllowsShowingTheResult {

    @Parameter
    ImagePlus input;

    boolean showResult = true;

    ImagePlus output = null;

    @Deprecated
    public MeasureHighQualityAreasPlugin() {}

    public MeasureHighQualityAreasPlugin(ImagePlus input) {
        this.input = input;
    }

    @Override
    public void setShowResult(boolean value) {
        showResult = value;
    }

    public ImagePlus getOutput() {
        return output;
    }

    @Override
    public void run() {

        // determine quality in tiles
        MeasureQualityInTilesPlugin mqitp = new MeasureQualityInTilesPlugin(input, input.getWidth() / 64, input.getHeight() / 64);
        mqitp.setShowResult(false);
        mqitp.setSilent(true);

        ImagePlus qualityMap = mqitp.analyseFocusMeasure(FocusMeasures.FocusMeasure.DifferentialTenengrad);

        // determine area of high quality pixels
        ClearCLIJ clij = ClearCLIJ.getInstance();
        ClearCLImage inputCL = clij.converter(qualityMap).getClearCLImage();
        ClearCLImage thresholdMaskCL = clij.createCLImage(inputCL.getDimensions(), ImageChannelDataType.UnsignedInt8);

        double sum = Kernels.sumPixels(clij, inputCL);

        double average = sum / inputCL.getWidth() / inputCL.getHeight() / inputCL.getDepth();


        Kernels.threshold(clij, inputCL, thresholdMaskCL, (float)average);

        output = clij.converter(thresholdMaskCL).getImagePlus();
        inputCL.close();
        thresholdMaskCL.close();

        if (showResult) {
            output.show();
            output.setTitle("high quality mask");
        }

    }

    public static void main(String...  args) {
        new ij.ImageJ();

        ImagePlus
                input = IJ.openImage("C:/structure/data/fish_GAFASO.tif");
        input.show();

        new MeasureHighQualityAreasPlugin(input).run();



    }
}
