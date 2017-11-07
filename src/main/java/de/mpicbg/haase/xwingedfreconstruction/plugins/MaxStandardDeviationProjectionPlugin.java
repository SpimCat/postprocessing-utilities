package de.mpicbg.haase.xwingedfreconstruction.plugins;

import de.mpicbg.haase.xwingedfreconstruction.scijava.*;
import de.mpicbg.haase.xwingedfreconstruction.scijava.statistics.Average;
import de.mpicbg.haase.xwingedfreconstruction.scijava.statistics.StandardDeviation;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */

@Plugin(type = Command.class, menuPath = "XWing>EDF>Max Standard Deviation projection")
public class MaxStandardDeviationProjectionPlugin<T extends RealType<T>> implements Command
{
  @Parameter private Img<T> input;

  @Parameter private UIService uiService;

  @Parameter private int focusedRegionRadius = 5;
  @Parameter private double samplingFactor = 0.5;

  @Override public void run()
  {

    RandomAccessibleInterval
        downScaledImg = sample(input, samplingFactor);

    Img<FloatType>
        stdDevImage =
        StandardDeviationPerPixelMeasurement.process(downScaledImg,
                                                     focusedRegionRadius);

    ArgMaxProjection maxProjector = new ArgMaxProjection(stdDevImage);
    RandomAccessibleInterval<FloatType> maxProjection = maxProjector.getMaxProjection();

    Img<UnsignedShortType> zMap = maxProjector.getArgMaxProjection();
    //uiService.show("max", maxProjection);
    //uiService.show("argMax", zMap);

    RandomAccessibleInterval upsampledMaxProjection = sample(maxProjection, 1.0 / samplingFactor);
    RandomAccessibleInterval upsampledZMap = sample(zMap, 1.0 / samplingFactor);
    //uiService.show("upsampled argMax", upsampledMaxProjection);
    //uiService.show("upsampled argMax", upsampledZMap);

    ZMapProjection
        zMapProjector = new ZMapProjection(input, upsampledZMap);
    RandomAccessibleInterval edfProjection =  zMapProjector.getProjection();
    //uiService.show("projection", edfProjection);

    Average average = new Average(maxProjection);
    StandardDeviation
        standardDeviation = new StandardDeviation(maxProjection, average);

    double threshold = 0; //average.getAverage();// + standardDeviation.getStandardDevation() * 2;

    //System.out.println("Threshold: " + threshold);

    ThresholdImage
        thresholder = new ThresholdImage(upsampledMaxProjection, threshold);
    RandomAccessibleInterval goodRegion = thresholder.getBinary();

    //uiService.show("good region", goodRegion);

    PixelwiseImageProduct
        pixelwiseImageProduct = new PixelwiseImageProduct(edfProjection, goodRegion);
    RandomAccessibleInterval product = pixelwiseImageProduct.getProduct();

    uiService.show("EDF from " + input.dimension(2), product);
  }


  private static <T extends RealType<T>> RandomAccessibleInterval<T> sample(RandomAccessibleInterval<T> img, double factor) {

    InterpolatorFactory
        interpolatorFactory =
        new NLinearInterpolatorFactory<>();

    //OutOfBoundsBorderFactory<T, RandomAccessibleInterval<T>> outOfBoundsBorderFactory = new OutOfBoundsBorderFactory<T, RandomAccessibleInterval<T>>();

    long[] size = new long[img.numDimensions()];
    for (int d = 0 ; d < img.numDimensions(); d++) {
      size[d] = img.dimension(d);
    }

    RealRandomAccessible<T>
        interpolated = Views.interpolate(Views.expandBorder(img, size), interpolatorFactory);

    double[] scaling = new double[img.numDimensions()];
    for (int d = 0 ; d < 2; d++) {
      scaling[d] = factor;
    }
    if (img.numDimensions() > 2) {
      scaling[2] = 1.0;
    }

    RealRandomAccessible scaled = RealViews.affine(interpolated, new Scale(scaling));

    RandomAccessible sampled = Views.raster(scaled);

    long[] newSize = new long[img.numDimensions() * 2];
    for (int d = 0; d < 2; d++) {
      newSize[d+img.numDimensions()] = (long)(img.dimension(d) * factor);
    }
    if (img.numDimensions() > 2) {
      newSize[2 + img.numDimensions()] = img.dimension(2);
    }

    return Views.interval(sampled, Intervals.createMinSize(newSize));

  }
}
