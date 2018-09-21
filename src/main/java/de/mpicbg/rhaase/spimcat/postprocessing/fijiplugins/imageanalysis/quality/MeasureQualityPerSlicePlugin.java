package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.quality;

import autopilot.image.DoubleArrayImage;
import autopilot.measures.FocusMeasures;
import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.imagej.ClearCLIJ;
import clearcl.imagej.kernels.Kernels;
import de.mpicbg.rhaase.scijava.AbstractFocusMeasuresPlugin;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsShowingTheResult;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsSilentProcessing;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.deprecated.DCTS22PerSlice;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imagemath.EqualizeMeanGreyValueOfSlices;
import de.mpicbg.rhaase.utils.DoubleArrayImageImgConverter;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.measure.ResultsTable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;

/**
 * MeasureQualityPerSlicePlugin
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 09 2018
 */

@Plugin(type = Command.class, menuPath = "SpimCat>Quality measurement>Image Focus Measurements slice by slice IMP (Adapted Autopilot code, Royer et Al. 2016)")
public class MeasureQualityPerSlicePlugin extends AbstractFocusMeasuresPlugin implements
        Command, AllowsSilentProcessing, AllowsShowingTheResult {

    @Parameter
    ImagePlus input;
    private boolean silent = false;
    private boolean showResult = true;

    @Deprecated
    public MeasureQualityPerSlicePlugin() {
        super();
    }

    public MeasureQualityPerSlicePlugin(ImagePlus input) {
        this.input = input;
    }


    @Override
    public void setShowResult(boolean value) {
        showResult = value;
    }

    @Override
    public void setSilent(boolean value) {
        silent = true;
    }


    HashMap<FocusMeasures.FocusMeasure, double[]> resultMatrix = null;


    @Override
    public void run()
    {
        if (!silent) {
            if (!showDialog()) {
                return;
            }
        }

        ClearCLIJ clij = ClearCLIJ.getInstance();

        // convert imageplus to CLImage<AnyType>
        ClearCLImage anyTypeImage = clij.converter(input).getClearCLImage();

        // convert CLImage<AnyType> to CLImage<FloatType>
        ClearCLImage floatTypeImage = clij.createCLImage(anyTypeImage.getDimensions(), ImageChannelDataType.Float);
        Kernels.copy(clij, anyTypeImage, floatTypeImage);

        // convert CLImage<FloatType> to RandomAccessibleInterval<FloatType>
        RandomAccessibleInterval<FloatType> floatData = (RandomAccessibleInterval<FloatType>) clij.converter(floatTypeImage).getRandomAccessibleInterval();
        anyTypeImage.close();
        floatTypeImage.close();

        int numDimensions = floatData.numDimensions();

        if (showPlots) {
            if (numDimensions == 3)
            {
                resultMatrix = new HashMap<FocusMeasures.FocusMeasure, double[]>();
            } else {
                resultMatrix = null;
                IJ.log("Plotting is not possible for 2D images. Choose an image stack.");
            }
        }


        System.out.println("running");

        ResultsTable resultsTable = ResultsTable.getResultsTable();
        if (numDimensions == 2) {
            resultsTable.incrementCounter();
            process2D(floatData, 0);
        } else if (numDimensions == 3) {
            int numberOfSlices = (int) floatData.dimension(2);

            if (resultMatrix != null)
            {
                for (FocusMeasures.FocusMeasure focusMeasure : formerChoice)
                {
                    resultMatrix.put(focusMeasure, new double[numberOfSlices]);
                }
            }

            for (int z = 0; z < numberOfSlices; z++)
            {
                System.out.println("Slice " + z);
                RandomAccessibleInterval<FloatType>
                        slice = Views.hyperSlice(floatData, 2, z);

                resultsTable.incrementCounter();

                process2D(slice, z);
            }

        }


        if (showResult) {
            if (resultMatrix != null)
            {
                plotResultMatrix();
            }
            resultsTable.show("Results");
        }
    }


    private void process2D(RandomAccessibleInterval<FloatType> img, int slice) {
        ResultsTable resultsTable = ResultsTable.getResultsTable();
        resultsTable.addValue("slice", slice);

        DoubleArrayImage image = new DoubleArrayImageImgConverter(Views.iterable(img)).getDoubleArrayImage();


        for (FocusMeasures.FocusMeasure focusMeasure : formerChoice) {
            System.out.println("Determining " + focusMeasure.getLongName());
            double focusMeasureValue = FocusMeasures.computeFocusMeasure(focusMeasure, image);
            resultsTable.addValue(focusMeasure.getLongName(), focusMeasureValue);

            if (resultMatrix != null)
            {
                resultMatrix.get(focusMeasure)[slice] = focusMeasureValue;
            }
        }
    }


    private void plotResultMatrix() {

        double[] xValues = null;
        for (FocusMeasures.FocusMeasure focusMeasure : formerChoice) {
            double[] yValues = resultMatrix.get(focusMeasure);
            if (xValues == null) {
                xValues = new double[yValues.length];
                for (int i = 0; i < xValues.length; i++) {
                    xValues[i] = i;
                }
            }

            Plot plot = new Plot(focusMeasure.getLongName(), "slice", focusMeasure.getLongName(), xValues, yValues);
            plot.show();
        }
    }

    public double[] analyseFocusMeasure(FocusMeasures.FocusMeasure focusMeasure) {
        formerChoice.clear();
        formerChoice.add(focusMeasure);

        boolean wasShowResult = showResult;
        boolean wasShowPlots = showPlots;
        boolean wasSilent = silent;
        silent = true;
        showPlots = true;
        showResult = false;
        run();
        silent = wasSilent;
        showPlots = wasShowPlots;
        showResult = wasShowResult;

        return resultMatrix.get(focusMeasure);
    }

    public static void main(String... args) {

        new ij.ImageJ();

        ImagePlus
                input = IJ.openImage("C:/structure/data/fish_GAFASO.tif");
        input.show();

        FocusMeasures.FocusMeasure focusMeasure = FocusMeasures.FocusMeasure.StatisticMean;

        System.out.println(focusMeasure + " per slice: " + Arrays.toString(new MeasureQualityPerSlicePlugin(input).analyseFocusMeasure(focusMeasure)));

        EqualizeMeanGreyValueOfSlices emg = new EqualizeMeanGreyValueOfSlices(input);
        ImagePlus inputEqualized = emg.getOutput();

        System.out.println(focusMeasure + " eq per slice: " + Arrays.toString(new MeasureQualityPerSlicePlugin(inputEqualized).analyseFocusMeasure(focusMeasure)));

        //Plot plot = new Plot(focusMeasure, focusMeasure, focusMeasure + " eq", dctsPerSlice.getDcts2d(), dctsPerSlice2.getDcts2d());
        //plot.show();
    }
}
