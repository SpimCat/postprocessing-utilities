package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.quality;

import autopilot.image.DoubleArrayImage;
import autopilot.measures.FocusMeasures;
import autopilot.utils.ArrayMatrix;
import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.imagej.ClearCLIJ;
import clearcl.imagej.kernels.Kernels;
import de.mpicbg.rhaase.scijava.AbstractFocusMeasuresPlugin;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsSilentProcessing;
import de.mpicbg.rhaase.utils.DoubleArrayImageImgConverter;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
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

@Plugin(type = Command.class, menuPath = "XWing>Quality measurement>Image Focus Measurements tiles IMP (Adapted Autopilot code, Royer et Al. 2016)")
public class MeasureQualityInTilesPlugin extends AbstractFocusMeasuresPlugin implements
        Command, AllowsSilentProcessing
{
    private final static int tileSize = 64;
    ClearCLIJ clij;


    public MeasureQualityInTilesPlugin()
    {
        super();
        clij = ClearCLIJ.getInstance();
    }

    public MeasureQualityInTilesPlugin(ImagePlus imp, int numberOfTilesX, int numberOfTilesY) {
        this.numberOfTilesX = numberOfTilesX;
        this.numberOfTilesY = numberOfTilesY;
        currentData = imp;
        clij = ClearCLIJ.getInstance();
    }

    @Parameter
    private ImagePlus currentData;

    @Parameter
    private int numberOfTilesX = 16;

    @Parameter
    private int numberOfTilesY = 32;

    private boolean silent = false;

    private HashMap<FocusMeasures.FocusMeasure, Img<FloatType>>
            resultMaps;

    @Override public void run()
    {
        if (!silent) {
            if (!showDialog()) {
                return;
            }
        }

        int numDimensions = 3;


        // convert imageplus to CLImage<AnyType>
        ClearCLImage anyTypeImage = clij.converter(currentData).getClearCLImage();

        // convert CLImage<AnyType> to CLImage<FloatType>
        ClearCLImage floatTypeImage = clij.createCLImage(anyTypeImage.getDimensions(), ImageChannelDataType.Float);
        Kernels.copy(clij, anyTypeImage, floatTypeImage);

        // convert CLImage<FloatType> to RandomAccessibleInterval<FloatType>
        RandomAccessibleInterval<FloatType> floatData = (RandomAccessibleInterval<FloatType>) clij.converter(floatTypeImage).getRandomAccessibleInterval();

        resultMaps = new HashMap<>();

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
        for (FocusMeasures.FocusMeasure focusMeasure : formerChoice)
        {
            Img<FloatType> img = resultMaps.get(focusMeasure);
            clij.show(img, focusMeasure.getLongName());
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

        return clij.converter(img).getImagePlus();
    }

    public static void main(String... args) throws IOException
    {
        new ij.ImageJ();
        
        ImagePlus
                input = IJ.openImage("C:/structure/data/fish_GAFASO.tif");
        input.show();

        MeasureQualityInTilesPlugin mqitp = new MeasureQualityInTilesPlugin(input, input.getWidth() / tileSize, input.getHeight() / tileSize);
        mqitp.setSilent(true);
        ImagePlus result = mqitp.analyseFocusMeasure(FocusMeasures.FocusMeasure.SpectralNormDCTEntropyShannon);
        result.show();


    }

    @Override
    public void setSilent(boolean value) {
        silent = value;
    }
}
