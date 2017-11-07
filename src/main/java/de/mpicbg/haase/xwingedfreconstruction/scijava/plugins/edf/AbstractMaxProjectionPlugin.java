package de.mpicbg.haase.xwingedfreconstruction.scijava.plugins.edf;

import de.mpicbg.haase.xwingedfreconstruction.scijava.image.PixelwiseImageProduct;
import de.mpicbg.haase.xwingedfreconstruction.scijava.image.Sampler;
import de.mpicbg.haase.xwingedfreconstruction.scijava.image.ThresholdImage;
import de.mpicbg.haase.xwingedfreconstruction.scijava.plugins.internal.ArgMaxProjection;
import de.mpicbg.haase.xwingedfreconstruction.scijava.plugins.internal.ZMapProjection;
import de.mpicbg.haase.xwingedfreconstruction.scijava.statistics.Average;
import de.mpicbg.haase.xwingedfreconstruction.scijava.statistics.StandardDeviation;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
public class AbstractMaxProjectionPlugin<T extends RealType<T>>
{


  protected RandomAccessibleInterval project(Img<T> input, double samplingFactor, RandomAccessibleInterval<FloatType> sampledProjectionArgumentImg)
  {
    RandomAccessibleInterval stdDevImage = sampledProjectionArgumentImg;

    ArgMaxProjection maxProjector = new ArgMaxProjection(stdDevImage);
    RandomAccessibleInterval<FloatType> maxProjection = maxProjector.getMaxProjection();

    Img<UnsignedShortType> zMap = maxProjector.getArgMaxProjection();
    //uiService.show("max", maxProjection);
    //uiService.show("argMax", zMap);

    RandomAccessibleInterval upsampledMaxProjection = resampleStackSliceBySlice(maxProjection, 1.0 / samplingFactor);
    RandomAccessibleInterval upsampledZMap = resampleStackSliceBySlice(zMap, 1.0 / samplingFactor);
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
    return product;
  }


  protected static <T extends RealType<T>> RandomAccessibleInterval<T> resampleStackSliceBySlice(RandomAccessibleInterval<T> img, double factor) {

    double[] scaling = new double[img.numDimensions()];
    for (int d = 0 ; d < 2; d++) {
      scaling[d] = factor;
    }
    if (img.numDimensions() > 2) {
      scaling[2] = 1.0;
    }

    return new Sampler(img, scaling).getSampledImage();

  }
}
