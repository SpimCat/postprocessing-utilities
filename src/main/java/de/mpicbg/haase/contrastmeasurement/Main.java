package de.mpicbg.haase.contrastmeasurement;

import de.mpicbg.haase.contrastmeasurement.scijava.*;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorSingleBoundary;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Random;

public class Main
{
  final static ImageJ ij = new ImageJ();

  final static double xyDownScalingFactor = 0.25;

  public static void main(final String... args) throws Exception
  {
    ij.ui().showUI();

    Dataset dataset = (Dataset)ij.io().open("C:\\structure\\data\\xwing\\2017-11-01-EDF\\EDF5_focus_going_through_lightsheet_10.tif");
    ij.ui().show(dataset);

    Img<UnsignedShortType> input = (Img)dataset;
        //
    //    System.out.println(dataset.getClass().getCanonicalName());
    //
    //    Img<UnsignedShortType> stdDevImage = testStdDevCalculation(dataset);

    Dataset datasetStdDev = (Dataset)
        ij.io()
          .open(
              "C:\\structure\\data\\xwing\\2017-11-01-EDF\\EDF5_focus_going_through_lightsheet_10_stdDev.tif");

    ij.ui().show(datasetStdDev);

    Img<UnsignedShortType> stdDevImage = (Img)datasetStdDev;

    ArgMaxProjection maxProjector = new ArgMaxProjection(stdDevImage);
    RandomAccessibleInterval<FloatType> maxProjection = maxProjector.getMaxProjection();

    Img<UnsignedShortType> zMap = maxProjector.getArgMaxProjection();
    ij.ui().show("max", maxProjection);
    ij.ui().show("argMax", zMap);


    RandomAccessibleInterval upsampledMaxProjection = sample(maxProjection, 1.0 / xyDownScalingFactor);
    RandomAccessibleInterval upsampledZMap = sample(zMap, 1.0 / xyDownScalingFactor);
    ij.ui().show("upsampled argMax", upsampledMaxProjection);
    ij.ui().show("upsampled argMax", upsampledZMap);

    ZMapProjection<UnsignedShortType, UnsignedShortType> zMapProjector = new ZMapProjection<UnsignedShortType, UnsignedShortType>(input, upsampledZMap);
    RandomAccessibleInterval edfProjection =  zMapProjector.getProjection();
    ij.ui().show("projection", edfProjection);


    ThresholdImage thresholder = new ThresholdImage(upsampledMaxProjection, 1000.0);
    RandomAccessibleInterval goodRegion = thresholder.getBinary();

    ij.ui().show(goodRegion);

    PixelwiseImageProduct pixelwiseImageProduct = new PixelwiseImageProduct(edfProjection, goodRegion);
    RandomAccessibleInterval product = pixelwiseImageProduct.getProduct();

    ij.ui().show(product);
  }

  private static Img<FloatType> testStdDevCalculation(Img img) throws
                                                               Exception
  {

    ij.ui().show(img);


    RandomAccessibleInterval
        downScaledImg = sample(img, xyDownScalingFactor);

    int radius = 5;

    Img<FloatType>
        stdDevImage =
        StandardDeviationPerPixelMeasurement.process(downScaledImg,
                                                     radius);

    System.out.println("Bye!");

    return stdDevImage;
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
