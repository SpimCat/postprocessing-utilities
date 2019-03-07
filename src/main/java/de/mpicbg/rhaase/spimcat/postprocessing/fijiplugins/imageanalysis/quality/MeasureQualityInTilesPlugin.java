package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.quality;

import autopilot.image.DoubleArrayImage;
import autopilot.measures.FocusMeasures;
import autopilot.utils.ArrayMatrix;
import de.mpicbg.rhaase.scijava.AbstractFocusMeasuresPlugin;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsShowingTheResult;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsSilentProcessing;
import de.mpicbg.rhaase.utils.DoubleArrayImageImgConverter;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLImage;
import net.haesleinhuepf.clij.clearcl.enums.ImageChannelDataType;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.HashMap;

/**
 * MeasureQualityInTilesPlugin
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 09 2018
 */

@Plugin(type = Command.class, menuPath = "SpimCat>Quality measurement>Image Focus Measurements in tiles IMP (Adapted Autopilot code, Royer et Al. 2016)")
public class MeasureQualityInTilesPlugin extends AbstractFocusMeasuresPlugin implements
        Command, AllowsSilentProcessing, AllowsShowingTheResult
{
    final static int defaultTileSize = 16;
    CLIJ clij;


    public MeasureQualityInTilesPlugin()
    {
        super();
        clij = CLIJ.getInstance();
    }

    public MeasureQualityInTilesPlugin(ImagePlus imp, int numberOfTilesX, int numberOfTilesY) {
        this.numberOfTilesX = numberOfTilesX;
        this.numberOfTilesY = numberOfTilesY;
        currentData = imp;
        clij = CLIJ.getInstance();
    }

    @Parameter
    private ImagePlus currentData;

    @Parameter
    private int numberOfTilesX = 16;

    @Parameter
    private int numberOfTilesY = 32;

    private boolean silent = false;
    private boolean showResult = true;

    private HashMap<FocusMeasures.FocusMeasure, Img<FloatType>>
            resultMaps;

    @Override public void run()
    {
        if (!silent) {
            if (!showDialog()) {
                return;
            }
        }



        // convert imageplus to CLImage<AnyType>
        ClearCLImage anyTypeImage = clij.convert(currentData, ClearCLImage.class);

        // convert CLImage<AnyType> to CLImage<FloatType>
        ClearCLImage floatTypeImage = clij.createCLImage(anyTypeImage.getDimensions(), ImageChannelDataType.Float);
        clij.op().copy(anyTypeImage, floatTypeImage);

        // convert CLImage<FloatType> to RandomAccessibleInterval<FloatType>
        RandomAccessibleInterval<FloatType> floatData = (RandomAccessibleInterval<FloatType>) clij.convert(floatTypeImage, RandomAccessibleInterval.class);
        anyTypeImage.close();
        floatTypeImage.close();

        resultMaps = new HashMap<>();

        int numDimensions = floatData.numDimensions();
        if (numDimensions == 2) {
            for (FocusMeasures.FocusMeasure focusMeasure : formerChoice)
            {
                resultMaps.put(focusMeasure, ArrayImgs.floats(new long[]{numberOfTilesX, numberOfTilesY}));
            }

            process2D(floatData, 0);
        } else if (numDimensions == 3) {

            int numberOfSlices = (int) currentData.getNSlices();
            for (FocusMeasures.FocusMeasure focusMeasure : formerChoice)
            {
                resultMaps.put(focusMeasure, ArrayImgs.floats(new long[]{numberOfTilesX, numberOfTilesY, numberOfSlices}));
            }

            for (int z = 0; z < numberOfSlices; z++)
            {
                System.out.println("Slice " + z);
                RandomAccessibleInterval<FloatType>
                        slice = Views.hyperSlice(floatData, 2, z);


                process2D(slice, z);
            }

        }
        if (showResult) {
            for (FocusMeasures.FocusMeasure focusMeasure : formerChoice) {
                Img<FloatType> img = resultMaps.get(focusMeasure);
                clij.show(img, focusMeasure.getLongName());
            }
        }
    }


    private void process2D(RandomAccessibleInterval<FloatType> img, int slice) {
        for (FocusMeasures.FocusMeasure focusMeasure : formerChoice) {

            Img<FloatType> resultImg = resultMaps.get(focusMeasure);

            mapFeatureToImg(img, slice, focusMeasure, resultImg);
        }
    }

    private void mapFeatureToImg(RandomAccessibleInterval<FloatType> img,
                                 int slice,
                                 FocusMeasures.FocusMeasure focusMeasure,
                                 Img<FloatType> resultImg)
    {
        RandomAccess<FloatType> resultRA = resultImg.randomAccess();
        long[] position;

        System.out.println("Determining " + focusMeasure.getLongName());

        DoubleArrayImage
                image = new DoubleArrayImageImgConverter(Views.iterable(img)).getDoubleArrayImage();


        final ArrayMatrix<DoubleArrayImage>
                lTiles = image.extractTiles(numberOfTilesX, numberOfTilesY);

        for (int x = 0; x < numberOfTilesX; x++)
        {
            for (int y = 0; y < numberOfTilesY; y++)
            {
                final DoubleArrayImage lTile = lTiles.get(x, y);
                double
                        focusMeasureValue =
                        FocusMeasures.computeFocusMeasure(focusMeasure, lTile);

                if (slice < 0)
                { // 2D
                    position = new long[] { x, y };
                }
                else
                { // 3D?
                    position = new long[] { x, y, slice };
                }
                resultRA.setPosition(position);
                resultRA.get().setReal(focusMeasureValue);
            }
        }
    }

    public ImagePlus analyseFocusMeasure(FocusMeasures.FocusMeasure focusMeasure) {
        resultMaps = null;
        formerChoice.clear();
        formerChoice.add(focusMeasure);

        run();

        Img<FloatType> img = resultMaps.get(focusMeasure);

        return clij.convert(img, ImagePlus.class);
    }

    public static void main(String... args) throws IOException
    {
        new ij.ImageJ();

        ImagePlus
                input = IJ.openImage("C:/structure/data/fish_GAFASO.tif");
        input.show();

        MeasureQualityInTilesPlugin mqitp = new MeasureQualityInTilesPlugin(input, input.getWidth() / defaultTileSize, input.getHeight() / defaultTileSize);
        mqitp.setSilent(true);
        mqitp.setShowResult(false);

        ImagePlus tenengrad = mqitp.analyseFocusMeasure(FocusMeasures.FocusMeasure.DifferentialTenengrad);
        //ImagePlus dcts2d = mqitp.analyseFocusMeasure(FocusMeasures.FocusMeasure.SpectralNormDCTEntropyShannon);
        //ImagePlus mean = mqitp.analyseFocusMeasure(FocusMeasures.FocusMeasure.StatisticMean);

        tenengrad.show();
        tenengrad.setTitle("tenengrad");
        //dcts2d.show();
        //dcts2d.setTitle("dcts2d");
        //mean.show();
        //mean.setTitle("mean");

        CLIJ clij = CLIJ.getInstance();
        ClearCLImage inputCL = clij.convert(input, ClearCLImage.class);
        ClearCLImage outputCL = clij.createCLImage(inputCL);

        clij.op().blurSliceBySlice(inputCL, outputCL, 20, 20, 10f, 10f);

        ImagePlus output = clij.convert(outputCL, ImagePlus.class);

        inputCL.close();
        outputCL.close();

        new MeasureQualityInTilesPlugin(output, input.getWidth() / defaultTileSize, input.getHeight() / defaultTileSize).run();



    }

    @Override
    public void setSilent(boolean value) {
        silent = value;
    }

    @Override
    public void setShowResult(boolean value) {
        showResult = value;
    }
}
