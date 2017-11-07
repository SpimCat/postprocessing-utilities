package de.mpicbg.haase.xwingedfreconstruction.scijava.image;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
public class Sampler<T extends RealType<T>>
{
  RandomAccessibleInterval<T> img;
  double[] samplingFactors;
  RandomAccessibleInterval<T> output = null;

  public Sampler(RandomAccessibleInterval<T> input, double[] samplingFactors) {
    this.img = input;
    this.samplingFactors = samplingFactors;
  }

  private synchronized void process() {
    if (output != null) {
      return;
    }

    InterpolatorFactory
        interpolatorFactory =
        new NLinearInterpolatorFactory<>();

    long[] size = new long[img.numDimensions()];
    for (int d = 0 ; d < img.numDimensions(); d++) {
      size[d] = img.dimension(d);
    }

    RealRandomAccessible<T>
        interpolated = Views.interpolate(Views.expandBorder(img, size), interpolatorFactory);

    double[] scaling = samplingFactors;

    RealRandomAccessible scaled = RealViews.affine(interpolated, new Scale(scaling));

    RandomAccessible sampled = Views.raster(scaled);

    long[] newSize = new long[img.numDimensions() * 2];
    for (int d = 0; d < img.numDimensions(); d++) {
      newSize[d+img.numDimensions()] = (long)(img.dimension(d) * samplingFactors[d]);
    }

    output = Views.interval(sampled, Intervals.createMinSize(newSize));

  }

  public RandomAccessibleInterval<T> getSampledImage() {
    process();
    return output;
  }
}
