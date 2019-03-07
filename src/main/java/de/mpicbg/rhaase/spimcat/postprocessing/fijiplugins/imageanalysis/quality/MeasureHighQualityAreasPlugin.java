package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.quality;

import autopilot.measures.FocusMeasures;
import de.mpicbg.rhaase.scijava.AbstractFocusMeasuresPlugin;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsShowingTheResult;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsSilentProcessing;
import ij.IJ;
import ij.ImagePlus;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLImage;
import net.haesleinhuepf.clij.clearcl.enums.ImageChannelDataType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import static de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.quality.MeasureQualityInTilesPlugin.defaultTileSize;

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
        MeasureQualityInTilesPlugin mqitp = new MeasureQualityInTilesPlugin(input, input.getWidth() / defaultTileSize, input.getHeight() / defaultTileSize);
        mqitp.setShowResult(false);
        mqitp.setSilent(true);

        ImagePlus qualityMap = mqitp.analyseFocusMeasure(FocusMeasures.FocusMeasure.DifferentialTenengrad);

        // determine area of high quality pixels
        CLIJ clij = CLIJ.getInstance();
        ClearCLImage inputCL = clij.convert(qualityMap, ClearCLImage.class);
        ClearCLImage thresholdMaskCL = clij.createCLImage(inputCL.getDimensions(), ImageChannelDataType.UnsignedInt8);

        double sum = clij.op().sumPixels(inputCL);

        double average = sum / inputCL.getWidth() / inputCL.getHeight() / inputCL.getDepth();


        clij.op().threshold(inputCL, thresholdMaskCL, (float)average);

        output = clij.convert(thresholdMaskCL, ImagePlus.class);
        inputCL.close();
        thresholdMaskCL.close();

        if (showResult) {
            output.show();
            output.setDisplayRange(0, 1);
            output.setTitle("high quality mask");
        }

    }

    public static void main(String...  args) {
        new ij.ImageJ();

        ImagePlus
                input = IJ.openImage("C:/structure/data/fish_GAFASO.tif");
        input.show();

        new MeasureHighQualityAreasPlugin(input).run();

        CLIJ clij = CLIJ.getInstance();
        ClearCLImage inputCL = clij.convert(input, ClearCLImage.class);
        ClearCLImage outputCL = clij.createCLImage(inputCL);

        clij.op().blurSliceBySlice(inputCL, outputCL, 20, 20, 10f, 10f);

        ImagePlus output = clij.convert(outputCL, ImagePlus.class);

        inputCL.close();
        outputCL.close();

        new MeasureHighQualityAreasPlugin(output).run();

    }
}
